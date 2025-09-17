package io.github.pukkaone.graphql.search

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.github.pukkaone.graphql.apiversion.RoutingExecutionGraphQlService
import io.github.pukkaone.graphql.search.schema.ProtoSchema

/**
 * Handles put documents mutation.
 */
class PutDataFetcher(
    private val documentType: String,
    private val putDocuments: PutDocumentsUseCase
) : DataFetcher<Boolean> {

    override fun get(environment: DataFetchingEnvironment): Boolean {
        val index: String = environment.requireArgument("index")
        val documents: List<MutableMap<String, Any>> = environment.requireArgument("documents")
        val protoSchema: ProtoSchema = environment.graphQlContext.get(RoutingExecutionGraphQlService.PROTO_SCHEMA)
        return putDocuments(index, documents, documentType, protoSchema)
    }
}
