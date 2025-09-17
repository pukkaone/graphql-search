package io.github.pukkaone.graphql.search

import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SearchSchemaGeneratorTest {

    @Test
    fun `when generate then define filter input type`() {
        val typeDefinition = typeDefinitionRegistry.getType("WithIDFilterInput", InputObjectTypeDefinition::class.java)
            .orElseThrow()
        assertThat(typeDefinition.inputValueDefinitions)
            .extracting(InputValueDefinition::getName)
            .contains(tuple("id"))
    }

    @Test
    fun `when generate then define input type`() {
        val typeDefinition = typeDefinitionRegistry.getType("WithIDInput", InputObjectTypeDefinition::class.java)
            .orElseThrow()
        assertThat(typeDefinition.inputValueDefinitions)
            .extracting(InputValueDefinition::getName)
            .contains(tuple("id"))
    }

    companion object {
        private lateinit var typeDefinitionRegistry: TypeDefinitionRegistry

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            val protoSchema = ProtoSchema("test")
            val schemaBytes = SearchSchemaGenerator.generate(protoSchema.typeDefinitionRegistry)
            val schema = String(schemaBytes.contentAsByteArray, StandardCharsets.UTF_8)
            val schemaParser = SchemaParser()
            typeDefinitionRegistry = schemaParser.parse(schema)
        }
    }
}
