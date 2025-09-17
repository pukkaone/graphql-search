package io.github.pukkaone.graphql.search

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

/**
 * Handles aggregation query.
 */
class AggregateDataFetcher(
    private val aggregateDocuments: AggregateDocumentsUseCase,
) : DataFetcher<List<Aggregation>> {

    override fun get(environment: DataFetchingEnvironment): List<Aggregation> {
        val index: String = environment.getArgument("index")
        val filter: Map<String, Any> = environment.getArgument("filter") ?: emptyMap()
        val groupBy: Map<String, Any> = environment.getArgument("groupBy")
        return aggregateDocuments(
            index,
            filter,
            groupBy,
        )
    }
}
