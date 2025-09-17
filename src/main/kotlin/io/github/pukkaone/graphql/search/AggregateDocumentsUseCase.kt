package io.github.pukkaone.graphql.search

import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Requests
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.stereotype.Service

/**
 * Groups documents into buckets.
 */
@Service
class AggregateDocumentsUseCase(
    private val elasticsearchClient: RestHighLevelClient,
) {
    private fun extractBucket(bucket: Terms.Bucket): Bucket {
        val subAggregations: List<Aggregation> =
            if (bucket.aggregations != null && bucket.aggregations.asList().isNotEmpty()) {
                extractAggregations(bucket.aggregations)
            } else {
                emptyList()
            }

        return Bucket(
            key = bucket.keyAsString,
            count = bucket.docCount.toInt(),
            aggregations = subAggregations,
        )
    }

    private fun extractTerms(aggregation: Terms): Aggregation {
        val buckets: List<Bucket> = aggregation.buckets.map(::extractBucket)

        return Aggregation(
            groupBy = aggregation.name,
            buckets = buckets,
        )
    }

    private fun extractAggregation(aggregation: org.elasticsearch.search.aggregations.Aggregation): Aggregation {
        return when (aggregation) {
            is Terms ->
                extractTerms(aggregation)
            else ->
                error("Unsupported aggregation $aggregation")
        }
    }

    private fun extractAggregations(aggregations: Aggregations): List<Aggregation> {
        return aggregations.map(::extractAggregation)
    }

    /**
     * Groups documents into buckets.
     *
     * @param index
     *   index name
     * @param filter
     *   to select documents
     * @param groupBy
     *   how to assign documents to buckets
     * @return search results
     */
    operator fun invoke(
        index: String,
        filter: Map<String, Any>,
        groupBy: Map<String, Any>,
    ): List<Aggregation> {
        val query: QueryBuilder = QueryTransformer.toQuery(filter)
        val searchSource = SearchSourceBuilder()
            .query(query)
            .size(0)
            .trackTotalHits(false)
        for (aggregation in AggregationTransformer.toAggregations(groupBy)) {
            searchSource.aggregation(aggregation)
        }

        val searchRequest = Requests.searchRequest(index)
            .source(searchSource)
        val searchResponse: SearchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT)
        return extractAggregations(searchResponse.aggregations)
    }
}
