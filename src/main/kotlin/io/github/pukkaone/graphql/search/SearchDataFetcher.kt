package io.github.pukkaone.graphql.search

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.github.pukkaone.graphql.apiversion.RoutingExecutionGraphQlService
import io.github.pukkaone.graphql.search.model.SortField
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.springframework.data.domain.Window

/**
 * Handles search query.
 */
class SearchDataFetcher(
    private val documentType: String,
    private val searchDocuments: SearchDocumentsUseCase,
) : DataFetcher<Window<Map<String, Any>>> {

    private fun toSortFields(environment: DataFetchingEnvironment): List<SortField> {
        val sortArgument: List<Map<String, Any>> = environment.getArgument("sort") ?: emptyList()
        return sortArgument.map { SortField(it) }
    }

    override fun get(environment: DataFetchingEnvironment): Window<Map<String, Any>> {
        val index: String = environment.getArgument("index")
        val filter: Map<String, Any> = environment.getArgument("filter") ?: emptyMap()
        val sort: List<SortField> = toSortFields(environment)
        val after: String? = environment.getArgument("after")
        val first: Int? = environment.getArgument("first")
        val protoSchema: ProtoSchema = environment.graphQlContext.get(RoutingExecutionGraphQlService.PROTO_SCHEMA)
        return searchDocuments(
            index,
            filter,
            sort,
            after,
            first,
            documentType,
            protoSchema,
        )
    }
}
