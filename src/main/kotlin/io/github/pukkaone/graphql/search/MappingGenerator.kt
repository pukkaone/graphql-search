package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import graphql.Scalars.GraphQLBoolean
import graphql.Scalars.GraphQLFloat
import graphql.Scalars.GraphQLID
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.language.Directive
import graphql.language.EnumTypeDefinition
import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.language.StringValue
import graphql.scalars.ExtendedScalars.GraphQLByte
import graphql.scalars.ExtendedScalars.GraphQLLong
import graphql.scalars.ExtendedScalars.GraphQLShort
import graphql.schema.idl.TypeDefinitionRegistry
import io.github.pukkaone.graphql.search.model.GeoPoint
import io.github.pukkaone.graphql.search.schema.DocumentDirective
import io.github.pukkaone.graphql.search.schema.SearchableDirective
import io.github.pukkaone.graphql.search.schema.toBaseTypeName
import java.time.Instant
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

private fun Directive.getArgumentAsStringOrNull(name: String): String? {
    val argument = this.getArgument(name) ?: return null
    val value = argument.value
    if (value is StringValue) {
        return value.value
    }

    return null
}

private fun ObjectNode.typeIsObject(): Boolean {
    val typeNode = this.path("type")
    return typeNode.isMissingNode || typeNode.asText() == "nested" || typeNode.asText() == "object"
}

private fun ObjectNode.isObjectTypeWithEmptyProperties(): Boolean {
    if (this.typeIsObject()) {
        return this.path("properties").isEmpty
    }

    return false
}

/**
 * Generates search index mappings.
 */
class MappingGenerator(
    private val typeDefinitionRegistry: TypeDefinitionRegistry,
) {
    private val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

    private fun isNested(fieldDefinition: FieldDefinition): Boolean {
        val searchable = SearchableDirective.getDirectiveOrNull(fieldDefinition) ?: return false
        val formatArgument = searchable.getArgument(SearchableDirective.FORMAT)
        return formatArgument != null && formatArgument.value.toString() == "nested"
    }

    private fun toType(typeName: String): String {
        return when (typeName) {
            GraphQLBoolean.name ->
                "boolean"
            GraphQLByte.name ->
                "byte"
            GraphQLFloat.name ->
                "double"
            GeoPoint::class.simpleName ->
                "geo_point"
            GraphQLID.name ->
                "keyword"
            Instant::class.simpleName ->
                "date"
            GraphQLInt.name ->
                "integer"
            GraphQLLong.name ->
                "long"
            GraphQLShort.name ->
                "short"
            GraphQLString.name ->
                "text"
            else -> error("Unsupported type [$typeName]")
        }
    }

    private fun toMapping(fieldDefinition: FieldDefinition): ObjectNode {
        val fieldNode = objectMapper.createObjectNode()

        // If there is a directive that specifies a type, use that.
        SearchableDirective.getDirectiveOrNull(fieldDefinition)?.let { searchable ->
            val type = searchable.getArgumentAsStringOrNull(SearchableDirective.TYPE)
            if (type != null) {
                fieldNode.put("type", type)
                return fieldNode
            }
        }

        // Check for a known object or enum type in the type registry.
        val fieldType: String = fieldDefinition.type.toBaseTypeName()
        val optionalTypeDefinition = typeDefinitionRegistry.getType(fieldType)
        if (optionalTypeDefinition.isPresent) {
            val typeDefinition = optionalTypeDefinition.get()
            if (typeDefinition is ObjectTypeDefinition && fieldType != GeoPoint::class.simpleName) {
                return toMapping(typeDefinition, isNested(fieldDefinition))
            } else if (typeDefinition is EnumTypeDefinition) {
                // Convert enum to keyword.
                fieldNode.put("type", "keyword")
                return fieldNode
            }
        }

        // Convert known scalar types.
        fieldNode.put("type", toType(fieldType))
        return fieldNode
    }

    private fun toMapping(typeDefinition: ObjectTypeDefinition, isNested: Boolean): ObjectNode {
        val objectNode = objectMapper.createObjectNode()
        if (isNested) {
            objectNode.put("type", "nested")
        }

        val propertiesNode: ObjectNode = objectNode.putObject("properties")
        typeDefinition.fieldDefinitions
            .filter {
                SearchableDirective.isPresent(it) || typeDefinitionRegistry.isObjectType(it.type)
            }
            .forEach {
                val fieldMapping: ObjectNode = toMapping(it)
                if (!fieldMapping.isObjectTypeWithEmptyProperties()) {
                    propertiesNode.set<ObjectNode>(it.name, fieldMapping)
                }
            }
        return objectNode
    }

    private fun toMappings(): Map<String, ObjectNode> {
        return typeDefinitionRegistry.getTypes(ObjectTypeDefinition::class.java)
            .filter {
                DocumentDirective.isPresent(it)
            }
            .associate {
                it.name to toMapping(it, false)
            }
    }

    companion object {
        /**
         * Generates search index mappings.
         *
         * @param typeDefinitionRegistry
         *   types to translate
         * @return map of document type name to mapping
         */
        fun generate(typeDefinitionRegistry: TypeDefinitionRegistry): Map<String, ObjectNode> {
            return MappingGenerator(typeDefinitionRegistry).toMappings()
        }
    }
}
