package io.github.pukkaone.graphql.search

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.Requests
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Puts documents in a search index.
 */
@Service
class PutDocumentsUseCase(
    @Value("\${search.write.refresh:NONE}") private val refreshPolicy: RefreshPolicy,
    private val elasticsearchClient: RestHighLevelClient,
) {
    private fun extractId(document: Map<String, Any>, path: List<String>): String {
        var node = document
        for (i in 0 until path.size - 1) {
            node = node[path[i]] as Map<String, Any>
        }

        val id = node[path.last()]
            ?: error("Missing document ID field [${path.joinToString(".")}]")
        return id.toString()
    }

    private fun addIndexRequest(
        index: String,
        document: MutableMap<String, Any>,
        idPath: List<String>,
        bulkRequest: BulkRequest,
    ) {
        val documentId = extractId(document, idPath)
        TimestampField.addToDocument(document)
        val indexRequest = Requests.indexRequest(index)
            .id(documentId)
            .source(document)
        bulkRequest.add(indexRequest)
    }

    /**
     * Puts documents in a search index.
     *
     * @param index
     *   index name
     * @param documents
     *   to put
     * @param documentType
     *   document type name
     * @param protoSchema
     *   proto schema
     * @return true if all documents were put successfully
     */
    operator fun invoke(
        index: String,
        documents: List<MutableMap<String, Any>>,
        documentType: String,
        protoSchema: ProtoSchema,
    ): Boolean {
        val idPath = protoSchema.documentTypeToIdPathMap[documentType]
            ?: error("Unknown document type [$documentType]")

        val bulkRequest = Requests.bulkRequest()
            .setRefreshPolicy(refreshPolicy)
        for (document in documents) {
            addIndexRequest(index, document, idPath, bulkRequest)
        }

        val bulkResponse = elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT)
        if (bulkResponse.hasFailures()) {
            logger.error { bulkResponse.buildFailureMessage() }
            throw BulkResponseFailureException(bulkResponse)
        }

        return true
    }
}
