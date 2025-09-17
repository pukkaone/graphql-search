package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Creates a search index and assigns an alias to it.
 */
@Service
class CreateIndexWithAliasUseCase(
    private val aliasService: AliasService,
    private val elasticsearchClient: RestHighLevelClient,
    private val objectMapper: ObjectMapper,
) {
    private fun generateMappingsSettings(mappingsNode: ObjectNode): ObjectNode {
        mappingsNode.put("dynamic", false)
        TimestampField.addToMapping(mappingsNode, objectMapper)

        val mappingsSettingsNode = objectMapper.readValue(
            CreateIndexWithAliasUseCase::class.java.getResourceAsStream("/settings.json"), ObjectNode::class.java)
        return mappingsSettingsNode.set("mappings", mappingsNode)
    }

    /**
     * Creates a search index and assigns an alias to it.
     *
     * @param type
     *   document type
     * @param index
     *   index name
     * @param alias
     *   alias name
     * @param protoSchema
     *   proto schema
     * @return true if index did not exist and index was created
     */
    operator fun invoke(
        type: String,
        index: String,
        alias: String,
        protoSchema: ProtoSchema,
    ): Boolean {
        val typeNameToMappingMap: Map<String, ObjectNode> =
            MappingGenerator.generate(protoSchema.typeDefinitionRegistry)
        val mappingsNode: ObjectNode = typeNameToMappingMap[type] ?: error("Unknown document type [$type]")
        val mappingsSettingsNode: ObjectNode = generateMappingsSettings(mappingsNode)

        val source = objectMapper.writeValueAsString(mappingsSettingsNode)
        val createIndexRequest = CreateIndexRequest(index)
            .source(source, XContentType.JSON)

        val acknowledged: Boolean = elasticsearchClient.indices()
            .create(createIndexRequest, RequestOptions.DEFAULT)
            .isAcknowledged
        if (!acknowledged) {
            logger.warn { "Master node created index, but some nodes did not respond" }
        }

        aliasService.assignAlias(index, alias)
        return acknowledged
    }
}
