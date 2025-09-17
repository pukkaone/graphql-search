package io.github.pukkaone.graphql.search

import graphql.relay.DefaultConnectionCursor
import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import graphql.relay.Edge
import io.github.pukkaone.graphql.search.model.SortField
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import kotlin.math.min
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Requests
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties
import org.springframework.data.domain.KeysetScrollPosition
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.graphql.data.query.WindowConnectionAdapter
import org.springframework.stereotype.Service

private val EMPTY_PAGE_INFO = DefaultPageInfo(null, null, false, false)

@Suppress("DoubleMutabilityForCollection")
private fun MutableMap<String, Any>.putPath(keyPath: String, value: Any) {
    val keys: List<String> = keyPath.split('.')
    var parent = this
    for (key in keys.dropLast(1)) {
        val child = mutableMapOf<String, Any>()
        parent[key] = child
        parent = child
    }

    parent[keys.last()] = value
}

/**
 * Searches for documents.
 */
@Service
class SearchDocumentsUseCase(
    private val cursorStrategy: CursorStrategy<ScrollPosition>,
    private val elasticsearchClient: RestHighLevelClient,
    private val springDataWebProperties: SpringDataWebProperties,
) {
    private val connectionAdapter = WindowConnectionAdapter(cursorStrategy)

    private fun configureQuery(
        filter: Map<String, Any>,
        searchSource: SearchSourceBuilder,
    ) {
        val query = QueryTransformer.toQuery(filter)
        searchSource.query(query)
    }

    private fun configureSize(
        first: Int?,
        searchSource: SearchSourceBuilder,
    ): Int {
        var actualSize = with(springDataWebProperties.pageable) {
            val size = first ?: defaultPageSize
            size.takeIf { it <= maxPageSize }
                ?: error(
                    "first value $size exceeds maximum $maxPageSize" +
                        " configured by property spring.data.web.pageable.max-page-size"
                )
        }

        // Fetch one extra hit to determine if there's a next page.
        ++actualSize
        searchSource.size(actualSize)
        return actualSize
    }

    private fun configureSearchAfter(
        after: String?,
        searchSource: SearchSourceBuilder,
    ) {
        if (after != null) {
            val scrollPosition = cursorStrategy.fromCursor(after) as KeysetScrollPosition
            val searchAfter = arrayOfNulls<Any>(scrollPosition.keys.size)
            for ((key, value) in scrollPosition.keys) {
                searchAfter[key.toInt()] = value
            }

            searchSource.searchAfter(searchAfter)
        }
    }

    private fun configureSort(
        sortFields: List<SortField>,
        idField: String,
        searchSource: SearchSourceBuilder,
    ) {
        if (sortFields.isEmpty()) {
            searchSource.sort(SortBuilders.scoreSort())
        } else {
            for (sortField in sortFields) {
                searchSource.sort(SortTransformer.toSort(sortField))
            }
        }

        // Ensure search after pagination is consistent by adding the ID field to the sort.
        if (sortFields.none { it.field == idField }) {
            searchSource.sort(idField, SortOrder.ASC)
        }
    }

    private fun configureAggregration(
        groupBy: Map<String, Any>,
        searchSource: SearchSourceBuilder,
    ) {
        for (aggregation in AggregationTransformer.toAggregations(groupBy)) {
            searchSource.aggregation(aggregation)
        }
    }

    private fun extractHits(
        searchResponse: SearchResponse,
        size: Int,
    ): Window<Hit> {
        val hits = searchResponse.hits.hits
        val items = hits.asList()
            .subList(0, min(size, hits.size))
            .map(SearchHit::getSourceAsMap)
        val window = Window.from(
            items,
            { i ->
                val keys = mutableMapOf<String, Any>()
                hits[i].sortValues.forEachIndexed { index, value ->
                    keys[index.toString()] = value
                }

                ScrollPosition.forward(keys)
            },
            hits.size > size
        )
        return window
    }

    private fun extractBucket(bucket: Terms.Bucket): Bucket {
        val subAggregations: Map<String, Any> =
            if (bucket.aggregations != null && bucket.aggregations.asList().isNotEmpty()) {
                extractAggregations(bucket.aggregations)
            } else {
                emptyMap()
            }

        return Bucket(
            key = bucket.keyAsString,
            count = bucket.docCount.toInt(),
            aggregations = subAggregations,
        )
    }

    private fun extractBuckets(aggregation: org.elasticsearch.search.aggregations.Aggregation): List<Bucket> {
        return when (aggregation) {
            is Terms ->
                aggregation.buckets.map(::extractBucket)
            else ->
                error("Unsupported aggregation $aggregation")
        }
    }

    private fun extractAggregations(aggregations: Aggregations?): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (aggregations != null) {
            for (aggregation in aggregations) {
                result.putPath(aggregation.name, extractBuckets(aggregation))
            }
        }

        return result
    }

    private fun toConnectionWithAggregation(
        window: Window<Hit>,
        aggregations: Map<String, Any>,
    ): ConnectionWithAggregation {
        val nodes: Collection<Hit> = connectionAdapter.getContent(window)
        val edges = ArrayList<Edge<Hit>>(nodes.size)
        var index = 0
        for (node in nodes) {
            val cursor = connectionAdapter.cursorAt(window, index++)
            edges.add(DefaultEdge(node, DefaultConnectionCursor(cursor)))
        }

        val pageInfo = if (nodes.isEmpty()) {
            EMPTY_PAGE_INFO
        } else {
            DefaultPageInfo(
                edges[0].cursor,
                edges[edges.size - 1].cursor,
                connectionAdapter.hasPrevious(window),
                connectionAdapter.hasNext(window)
            )
        }

        return ConnectionWithAggregation(edges, pageInfo, aggregations)
    }

    /**
     * Searches for documents.
     *
     * @param index
     *   index name
     * @param filter
     *   to select documents
     * @param sort
     *   fields and directions to sort by
     * @param after
     *   cursor after which to select documents
     * @param first
     *   maximum number of documents to return
     * @param groupBy
     *   how to assign documents to buckets
     * @param documentType
     *   document type
     * @param protoSchema
     *   proto schema
     * @return search results
     */
    operator fun invoke(
        index: String,
        filter: Map<String, Any>,
        sort: List<SortField>,
        after: String?,
        first: Int?,
        groupBy: Map<String, Any>,
        documentType: String,
        protoSchema: ProtoSchema,
    ): ConnectionWithAggregation {
        val searchSource = SearchSourceBuilder()
            .trackTotalHits(false)
        configureQuery(filter, searchSource)
        configureSort(sort, protoSchema.getIdField(documentType), searchSource)
        configureSearchAfter(after, searchSource)
        val size = configureSize(first, searchSource)
        configureAggregration(groupBy, searchSource)

        val searchRequest = Requests.searchRequest(index)
            .source(searchSource)
        val searchResponse: SearchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT)
        val window: Window<Hit> = extractHits(searchResponse, size)
        val aggregations = extractAggregations(searchResponse.aggregations)
        return toConnectionWithAggregation(window, aggregations)
    }
}
