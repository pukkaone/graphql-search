package io.github.pukkaone.graphql.search

import io.github.oshai.kotlinlogging.KotlinLogging
import org.elasticsearch.action.get.MultiGetRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Gets documents from a search index.
 */
@Service
class GetDocumentsUseCase(
    private val elasticsearchClient: RestHighLevelClient,
) {
    /**
     * Gets documents from a search index.
     *
     * @param index
     *   index name
     * @param ids
     *   document IDs to get
     * @return documents
     */
    operator fun invoke(
        index: String,
        ids: List<String>,
    ): List<Map<String, Any>?> {
        val multiGetRequest = MultiGetRequest()
        for (id in ids) {
            multiGetRequest.add(index, id)
        }

        val multiGetResponse = elasticsearchClient.mget(multiGetRequest, RequestOptions.DEFAULT)
        return multiGetResponse.responses
            .map { itemResponse ->
                if (itemResponse.isFailed) {
                    logger.error(itemResponse.failure.failure) { "Failed to get document id=${itemResponse.id}" }
                    null
                    // TODO: Add to errors in response.
                } else {
                    itemResponse.response.sourceAsMap
                }
            }
    }
}
