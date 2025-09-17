package io.github.pukkaone.graphql.search

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

/**
 * Handles get document query.
 */
class GetDataFetcher(
    private val getDocuments: GetDocumentsUseCase
) : DataFetcher<List<Map<String, Any>?>> {

    override fun get(environment: DataFetchingEnvironment): List<Map<String, Any>?> {
        val index: String = environment.getArgument("index")
        val ids: List<String> = environment.getArgument("ids")
        return getDocuments(index, ids)
    }
}
