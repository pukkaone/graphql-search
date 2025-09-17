package io.github.pukkaone.graphql.search

import graphql.language.FieldDefinition
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

private val DENY_INPUT_TYPE_GENERATION_FROM_OBJECT_TYPES =
    setOf("Aggregation", "Bucket", "Mutation", "Query", "Subscription")

/**
 * Renders type in GraphQL schema definition language syntax.
 *
 * @param nameSuffix
 *   to append to the type name
 */
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

/**
 * Renders type as an input type in GraphQL schema definition language syntax.
 */
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

    private fun generateAggregationWithInput(type: String) {
        schemaBuilder.append(
            """

            input AggregationWith${type}Input {
              terms: TermsAggregationInput
            }

            """.trimIndent()
        )
    }

    private fun generateOperatorWithInput(type: String) {
        schemaBuilder.append(
            """

            input OperatorWith${type}Input {

            """.trimIndent()
        )

        when (type) {
            "GeoPoint" ->
                schemaBuilder.append("  geoDistance: GeoDistanceRangeInput\n")
            "String" ->
                schemaBuilder.append("  contains: [$type!]\n")
        }

        schemaBuilder.append(
            """
              eq: $type
              exists: Boolean
              gt: $type
              gte: $type
              in: [$type!]
              lt: $type
              lte: $type
            }

            """.trimIndent()
        )
    }

    private fun isObjectType(fieldDefinition: FieldDefinition): Boolean {
        return typeDefinitionRegistry.isObjectType(TypeUtil.unwrapAll(fieldDefinition.type))
    }

    private fun generateGroupByInputType(objectTypeDefinition: ObjectTypeDefinition) {
        schemaBuilder.append(
            """

            input ${objectTypeDefinition.name}GroupByInput {

            """.trimIndent()
        )

        for (fieldDefinition in objectTypeDefinition.fieldDefinitions) {
            val fieldType: TypeName = TypeUtil.unwrapAll(fieldDefinition.type)
            if (fieldType.name == "GeoPoint") {
                continue
            }

            if (typeDefinitionRegistry.isObjectType(fieldType)) {
                schemaBuilder.append("  ${fieldDefinition.name}: ${fieldType.name}GroupByInput\n")
            } else {
                schemaBuilder.append("  ${fieldDefinition.name}: AggregationWith${fieldType.name}Input\n")
            }
        }

        schemaBuilder.append(
            """
            }

            """.trimIndent()
        )
    }

    private fun generatePutInputType(objectTypeDefinition: ObjectTypeDefinition) {
        schemaBuilder.append(
            """

            input ${objectTypeDefinition.name}Input {

            """.trimIndent()
        )

        for (fieldDefinition in objectTypeDefinition.fieldDefinitions) {
            if (isObjectType(fieldDefinition)) {
                schemaBuilder.append("  ${fieldDefinition.name}: ${fieldDefinition.type.printInput()}\n")
            } else {
                schemaBuilder.append("  ${fieldDefinition.name}: ${fieldDefinition.type.print()}\n")
            }
        }

        schemaBuilder.append(
            """
            }

            """.trimIndent()
        )
    }

    private fun generateInputTypes() {
        for ((typeName, _) in typeDefinitionRegistry.scalars()) {
            generateAggregationWithInput(typeName)
            generateOperatorWithInput(typeName)
        }

        for (objectTypeDefinition in typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)) {
            if (objectTypeDefinition.name in DENY_INPUT_TYPE_GENERATION_FROM_OBJECT_TYPES) {
                continue
            }

            val filterInputTypeDefinition = FilterInputTypeDefinition(objectTypeDefinition, typeDefinitionRegistry)
            if (!filterInputTypeDefinition.isEmpty()) {
                schemaBuilder.append(filterInputTypeDefinition.toString())
            }

            if (objectTypeDefinition.name != "GeoPoint") {
                generateGroupByInputType(objectTypeDefinition)
            }

            generatePutInputType(objectTypeDefinition)
        }
    }

    private fun generateMutationField(objectTypeDefinition: ObjectTypeDefinition) {
        val type: String = objectTypeDefinition.name
        schemaBuilder.append(
            """
            |  delete$type(index: String!, id: ID!): Boolean!
            |
            |  put$type(index: String!, documents: [${type}Input!]!): Boolean!

            """.trimMargin()
        )
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
        schemaBuilder.append(
            """
            |  $fieldPrefix(index: String!, ids: [ID!]!): [$type!]!
            |
            |  ${fieldPrefix}Aggregation(
            |      index: String!,
            |      filter: ${type}FilterInput,
            |      groupBy: ${type}GroupByInput!
            |  ): [Aggregation!]!
            |
            |  ${fieldPrefix}Connection(
            |      index: String!,
            |      filter: ${type}FilterInput,
            |      sort: [SortFieldInput!],
            |      after: String,
            |      first: Int
            |  ): ${type}Connection!

            """.trimMargin()
        )
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
