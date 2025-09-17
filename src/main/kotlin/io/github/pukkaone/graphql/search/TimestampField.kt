package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.Instant

private const val FIELD = "@timestamp"

/**
 * Field for when the document was put in the index.
 */
object TimestampField {

    /**
     * Adds timestamp field to mapping.
     *
     * @param mappingsNode
     *   document mapping
     * @param objectMapper
     *   to create JSON node
     */
    fun addToMapping(mappingsNode: ObjectNode, objectMapper: ObjectMapper) {
        val propertiesNode = mappingsNode.get("properties") as ObjectNode

        val timestampNode = objectMapper.createObjectNode()
            .put("type", "date")
        propertiesNode.set<JsonNode>(FIELD, timestampNode)
    }

    /**
     * Sets timestamp field in document.
     *
     * @param document
     *   to update
     */
    fun addToDocument(document: MutableMap<String, Any>) {
        document[FIELD] = Instant.now().toString()
    }
}
