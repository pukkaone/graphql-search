package io.github.pukkaone.graphql.search

import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.github.pukkaone.graphql.search.schema.ProtoSchema
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class SearchSchemaGeneratorTest {

    private lateinit var typeDefinitionRegistry: TypeDefinitionRegistry

    private fun generateSchema(apiVersion: String): TypeDefinitionRegistry {
        val protoSchema = ProtoSchema(apiVersion)
        val schemaBytes = SearchSchemaGenerator.generate(protoSchema.typeDefinitionRegistry)
        val schema = String(schemaBytes.contentAsByteArray, StandardCharsets.UTF_8)
        val schemaParser = SchemaParser()
        return schemaParser.parse(schema)
    }

    @BeforeAll
    fun beforeAll() {
        typeDefinitionRegistry = generateSchema("test")
    }

    @Test
    fun `when generate then define filter input type`() {
        val typeDefinition = typeDefinitionRegistry.getType("WithIDFilterInput", InputObjectTypeDefinition::class.java)
            .orElseThrow()
        assertThat(typeDefinition.inputValueDefinitions)
            .extracting<String>(InputValueDefinition::getName)
            .contains("id")
    }

    @Test
    fun `when generate then define input type`() {
        val typeDefinition = typeDefinitionRegistry.getType("WithIDInput", InputObjectTypeDefinition::class.java)
            .orElseThrow()
        assertThat(typeDefinition.inputValueDefinitions)
            .extracting<String>(InputValueDefinition::getName)
            .contains("id")
    }

    @Test
    fun `when generate then define output type`() {
        val typeDefinitionRegistry = generateSchema("v2019_12_31")

        val typeDefinition = typeDefinitionRegistry.getType("TransactionSearch", ObjectTypeDefinition::class.java)
            .orElseThrow()
        assertThat(typeDefinition.fieldDefinitions)
            .extracting<String>(FieldDefinition::getName)
            .containsExactlyInAnyOrder("edges", "pageInfo", "groupBy")
    }
}
