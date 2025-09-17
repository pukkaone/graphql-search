package io.github.pukkaone.graphql.search

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.github.pukkaone.graphql.apiversion.RoutingExecutionGraphQlService
import io.github.pukkaone.graphql.search.model.SortField
import io.github.pukkaone.graphql.search.schema.ProtoSchema

/**
 * Handles search query.
 */
class SearchDataFetcher(
    private val documentType: String,
    private val searchDocuments: SearchDocumentsUseCase,
) : DataFetcher<ConnectionWithAggregation> {

    private fun toSortFields(environment: DataFetchingEnvironment): List<SortField> {
        val sortArgument: List<Map<String, Any>> = environment.getArgumentOrElse("sort") { emptyList() }
        return sortArgument.map { SortField(it) }
    }

    override fun get(environment: DataFetchingEnvironment): ConnectionWithAggregation {
        val index: String = environment.requireArgument("index")
        val filter: Map<String, Any> = environment.getArgumentOrElse("filter") { emptyMap() }
        val sort: List<SortField> = toSortFields(environment)
        val after: String? = environment.getArgument("after")
        val first: Int? = environment.getArgument("first")
        val groupBy: Map<String, Any> = environment.getArgumentOrElse("groupBy") { emptyMap() }
        val protoSchema: ProtoSchema = environment.graphQlContext.get(RoutingExecutionGraphQlService.PROTO_SCHEMA)
        return searchDocuments(
            index,
            filter,
            sort,
            after,
            first,
            groupBy,
            documentType,
            protoSchema
        )
    }
}
