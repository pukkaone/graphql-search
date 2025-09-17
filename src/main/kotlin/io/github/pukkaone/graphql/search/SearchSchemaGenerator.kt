package io.github.pukkaone.graphql.search

import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.Type
import graphql.language.TypeName
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeUtil
import io.github.pukkaone.graphql.search.schema.DocumentDirective
import io.github.pukkaone.graphql.search.schema.FilterInputTypeDefinition
import java.nio.charset.StandardCharsets
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource

private val IGNORE_OBJECT_TYPES = setOf("Mutation", "Query", "Subscription")

private fun Type<*>.print(nameSuffix: String = ""): String {
    return when (this) {
        is NonNullType ->
            "${this.type.print(nameSuffix)}!"
        is ListType ->
            "[${this.type.print(nameSuffix)}]"
        is TypeName ->
            "${this.name}$nameSuffix"
        else ->
            error("Unsupported type: $this")
    }
}

private fun Type<*>.printInput(): String {
    return this.print("Input")
}

/**
 * Generates a GraphQL schema to query from a search index.
 */
class SearchSchemaGenerator(
    private val typeDefinitionRegistry: TypeDefinitionRegistry,
) {
    private val schemaBuilder = StringBuilder()

    private fun generateOperatorWithValue(type: String) {
        schemaBuilder.append("""

            input OperatorWith${type}Value {

            """.trimIndent())

        if (type == "String") {
            schemaBuilder.append(" contains: [${type}!]\n")
        }

        schemaBuilder.append("""
              eq: ${type}!
              exists: Boolean
              gt: ${type}
              gte: ${type}
              in: [${type}!]
              lt: ${type}
              lte: ${type}
            }

            """.trimIndent())
    }

    private fun generateInputType(objectTypeDefinition: ObjectTypeDefinition) {
        if (objectTypeDefinition.name in IGNORE_OBJECT_TYPES) {
            return
        }

        schemaBuilder.append("""

            input ${objectTypeDefinition.name}Input {

            """.trimIndent())

        for (fieldDefinition in objectTypeDefinition.fieldDefinitions) {
            if (typeDefinitionRegistry.isObjectType(TypeUtil.unwrapAll(fieldDefinition.type))) {
                schemaBuilder.append("  ${fieldDefinition.name}: ${fieldDefinition.type.printInput()}\n")
            } else {
                schemaBuilder.append("  ${fieldDefinition.name}: ${fieldDefinition.type.print()}\n")
            }
        }

        schemaBuilder.append("""
            }

            """.trimIndent())
    }

    private fun generateInputTypes() {
        for ((typeName, _) in typeDefinitionRegistry.scalars()) {
            generateOperatorWithValue(typeName)
        }

        for (typeDefinition in typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)) {
            val filterInputTypeDefinition = FilterInputTypeDefinition(typeDefinition, typeDefinitionRegistry)
            if (!filterInputTypeDefinition.isEmpty()) {
                schemaBuilder.append(filterInputTypeDefinition.toString())
            }

            generateInputType(typeDefinition)
        }
    }

    private fun generateMutationField(objectTypeDefinition: ObjectTypeDefinition) {
        val type: String = objectTypeDefinition.name
        schemaBuilder.append("""
            |  delete${type}(index: String!, id: ID!): Boolean!
            |
            |  put${type}(index: String!, documents: [${type}Input!]!): Boolean!

            """.trimMargin())
    }

    private fun generateMutationFields() {
        schemaBuilder.append("\nextend type Mutation {\n")
        typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)
            .filter {
                DocumentDirective.isPresent(it)
            }
            .forEach {
                generateMutationField(it)
            }
        schemaBuilder.append("}\n")
    }

    private fun generateQueryFields(objectTypeDefinition: ObjectTypeDefinition) {
        val type: String = objectTypeDefinition.name
        val fieldPrefix = type.replaceFirstChar(Char::lowercase)
        schemaBuilder.append("""
            |  ${fieldPrefix}(index: String!, ids: [ID!]!): [${type}!]!
            |
            |  ${fieldPrefix}Connection(
            |      index: String!,
            |      filter: ${type}FilterInput,
            |      sort: [SortFieldInput!],
            |      after: String,
            |      first: Int
            |  ): ${type}Connection!

            """.trimMargin())
    }

    private fun generateQueryFields() {
        schemaBuilder.append("\nextend type Query {\n")
        typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)
            .filter {
                DocumentDirective.isPresent(it)
            }
            .forEach {
                generateQueryFields(it)
            }
        schemaBuilder.append("}\n")
    }

    private fun toSchema(): ByteArrayResource {
        generateInputTypes()
        generateMutationFields()
        generateQueryFields()

        val schema = schemaBuilder.toString()
        return ByteArrayResource(schema.toByteArray(StandardCharsets.UTF_8))
    }

    companion object {
        /**
         * Generates a GraphQL schema to query from a search index.
         *
         * @param typeDefinitionRegistry
         *   proto type definitions
         * @return resource containing the generated schema
         */
        fun generate(typeDefinitionRegistry: TypeDefinitionRegistry): Resource {
            return SearchSchemaGenerator(typeDefinitionRegistry).toSchema()
        }
    }
}
