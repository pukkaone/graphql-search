package io.github.pukkaone.graphql.search

import org.elasticsearch.search.aggregations.AggregationBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder

private const val DEFAULT_SIZE = 10

/**
 * Transforms to aggregation request.
 */
class AggregationTransformer {

    private val aggregations = mutableListOf<AggregationBuilder>()
    private val path = mutableListOf<String>()

    private fun fieldPath(): String {
        return path.joinToString(".")
    }

    private fun terms(input: Map<String, Any>) {
        val field = fieldPath()
        val size = input["first"] as? Int ?: DEFAULT_SIZE
        val termsAggregation: TermsAggregationBuilder = AggregationBuilders.terms(field)
            .field(field)
            .size(size)

        aggregations.add(termsAggregation)
    }

    @Suppress("UNCHECKED_CAST")
    private fun pushField(field: String, value: Any) {
        path.add(field)
        transformOperatorToAggregation(value as Map<String, Any>)
        path.removeLast()
    }

    @Suppress("UNCHECKED_CAST")
    private fun transformOperatorToAggregation(groupBy: Map<String, Any>) {
        for ((key, value) in groupBy) {
            when (key) {
                "terms" ->
                    terms(value as Map<String, Any>)
                else ->
                    pushField(key, value)
            }
        }
    }

    companion object {
        /**
         * Transforms to aggregation requests.
         *
         * @param groupBy
         *   how to assign documents to buckets
         */
        fun toAggregations(groupBy: Map<String, Any>): List<AggregationBuilder> {
            val aggregationTransformer = AggregationTransformer()
            aggregationTransformer.transformOperatorToAggregation(groupBy)
            return aggregationTransformer.aggregations
        }
    }
}
