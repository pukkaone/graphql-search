package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MappingGeneratorTest {

    @Test
    fun `when type is Boolean then translate to boolean`() {
        val mapping = documentTypeToMappingMap["WithBoolean"]
        assertThat(mapping).hasToString("""
            {"properties":{"field":{"type":"boolean"}}}
            """.trimIndent())
    }

    @Test
    fun `when type is Float then translate to double`() {
        val mapping = documentTypeToMappingMap["WithFloat"]
        assertThat(mapping).hasToString("""
            {"properties":{"field":{"type":"double"}}}
            """.trimIndent())
    }

    @Test
    fun `when type is ID then translate to keyword`() {
        val mapping = documentTypeToMappingMap["WithID"]
        assertThat(mapping).hasToString("""
            {"properties":{"id":{"type":"keyword"}}}
            """.trimIndent())
    }

    @Test
    fun `when type is Int then translate to integer`() {
        val mapping = documentTypeToMappingMap["WithInt"]
        assertThat(mapping).hasToString("""
            {"properties":{"field":{"type":"integer"}}}
            """.trimIndent())
    }

    @Test
    fun `when type is Long then translate to long`() {
        val mapping = documentTypeToMappingMap["WithLong"]
        assertThat(mapping).hasToString("""
            {"properties":{"field":{"type":"long"}}}
            """.trimIndent())
    }

    @Test
    fun `when type is String then translate to text`() {
        val mapping = documentTypeToMappingMap["WithString"]
        assertThat(mapping).hasToString("""
            {"properties":{"field":{"type":"text"}}}
            """.trimIndent())
    }

    companion object {
        private lateinit var documentTypeToMappingMap: Map<String, ObjectNode>

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val protoSchema = ProtoSchema("test")
            documentTypeToMappingMap = MappingGenerator.generate(protoSchema.typeDefinitionRegistry)
        }
    }
}
