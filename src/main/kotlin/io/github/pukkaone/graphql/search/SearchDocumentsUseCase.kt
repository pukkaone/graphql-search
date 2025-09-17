package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.search.model.SortField
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import kotlin.math.min
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Requests
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties
import org.springframework.data.domain.KeysetScrollPosition
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.pagination.CursorStrategy
import org.springframework.stereotype.Service

/**
 * Searches for documents.
 */
@Service
class SearchDocumentsUseCase(
    private val cursorStrategy: CursorStrategy<ScrollPosition>,
    private val elasticsearchClient: RestHighLevelClient,
    private val springDataWebProperties: SpringDataWebProperties,
) {
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
        documentType: String,
        protoSchema: ProtoSchema,
    ): Window<Map<String, Any>> {
        val size = with(springDataWebProperties.pageable) {
            val size = first ?: defaultPageSize
            size.takeIf { it <= maxPageSize }
                ?: error(
                    "first value $size exceeds maximum $maxPageSize" +
                        " configured by property spring.data.web.pageable.max-page-size"
                )
        }

        val query = QueryTransformer.toQuery(filter)
        val searchSource = SearchSourceBuilder()
            .query(query)
            // Fetch one extra hit to determine if there's a next page.
            .size(size + 1)
            .trackTotalHits(false)
        configureSearchAfter(after, searchSource)
        configureSort(sort, protoSchema.getIdField(documentType), searchSource)

        val searchRequest = Requests.searchRequest(index)
            .source(searchSource)
        val searchResponse: SearchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT)
        val hits = searchResponse.hits.hits
        val items = hits.asList()
            .subList(0, min(size, hits.size))
            .map(SearchHit::getSourceAsMap)
        return Window.from(
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
    }
}
