package io.github.pukkaone.graphql.search

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Requests
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.cluster.metadata.AliasMetadata
import org.springframework.stereotype.Service

/**
 * Mediates interactions between components to fulfill alias requests.
 */
@Service
class AliasService(
    private val elasticsearchClient: RestHighLevelClient,
) {
    private fun findIndicesForAlias(aliasName: String): Map<String, Set<AliasMetadata>> {
        return elasticsearchClient.indices()
            .getAlias(GetAliasesRequest(aliasName), RequestOptions.DEFAULT)
            .aliases
            .filterValues { aliases ->
                aliases
                    .any { aliasMetadata ->
                        aliasMetadata.alias == aliasName
                    }
            }
    }

    /**
     * Finds index referenced by alias.
     *
     * @param aliasName
     *   alias name
     * @return index name, or null if not found
     */
    fun findIndexForAlias(aliasName: String): String? {
        return findIndicesForAlias(aliasName).keys.firstOrNull()
    }

    /**
     * Removes any existing alias and assigns alias to index.
     *
     * @param indexName
     *   index name
     * @param aliasName
     *   alias name
     */
    fun assignAlias(indexName: String, aliasName: String) {
        val indexAliasesRequest = Requests.indexAliasesRequest()

        findIndexForAlias(aliasName)?.let { existingIndex ->
            indexAliasesRequest.addAliasAction(
                IndicesAliasesRequest.AliasActions.remove()
                    .index(existingIndex)
                    .alias(aliasName)
            )
        }

        indexAliasesRequest.addAliasAction(
            IndicesAliasesRequest.AliasActions.add()
                .index(indexName)
                .alias(aliasName)
        )

        elasticsearchClient.indices()
            .updateAliases(indexAliasesRequest, RequestOptions.DEFAULT)
    }
}
