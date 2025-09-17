package io.github.pukkaone.graphql.search.schema

import graphql.language.FieldDefinition
import graphql.language.ObjectTypeDefinition
import graphql.schema.idl.TypeDefinitionRegistry
import graphql.schema.idl.TypeUtil

/**
 * Input type definition for filtering by fields of an object.
 */
class FilterInputTypeDefinition(
    objectTypeDefinition: ObjectTypeDefinition,
    private val typeDefinitionRegistry: TypeDefinitionRegistry,
) {
    private val namePrefix: String = objectTypeDefinition.name
    private val fields = mutableListOf<String>()

    init {
        if (DocumentDirective.isPresent(objectTypeDefinition)) {
            addField("  and: [${namePrefix}FilterInput!]")
            addField("  not: [${namePrefix}FilterInput!]")
            addField("  or: [${namePrefix}FilterInput!]")
        }

        for (fieldDefinition in objectTypeDefinition.fieldDefinitions) {
            val fieldType: String = fieldDefinition.type.toBaseTypeName()
            if (fieldType == "GeoPoint" || typeDefinitionRegistry.scalars().containsKey(fieldType)) {
                if (SearchableDirective.isPresent(fieldDefinition)) {
                    addField("  ${fieldDefinition.name}: OperatorWith${fieldType}Input")
                }
            } else if (typeDefinitionRegistry.isObjectType(TypeUtil.unwrapAll(fieldDefinition.type))) {
                val fieldFilterInputTypeDefinition = toFilterInputTypeDefinition(fieldDefinition)
                if (!fieldFilterInputTypeDefinition.isEmpty()) {
                    addField("  ${fieldDefinition.name}: ${fieldType}FilterInput")
                }
            }
        }
    }

    private fun toFilterInputTypeDefinition(fieldDefinition: FieldDefinition): FilterInputTypeDefinition {
        val fieldType: String = fieldDefinition.type.toBaseTypeName()
        val typeDefinition = typeDefinitionRegistry.getType(fieldType).orElseThrow()
        if (typeDefinition is ObjectTypeDefinition) {
            return FilterInputTypeDefinition(typeDefinition, typeDefinitionRegistry)
        }

        error("Unsupported field type: $fieldType")
    }

    private fun addField(field: String) {
        fields.add(field)
    }

    /**
     * Checks if input type has no fields.
     */
    fun isEmpty(): Boolean {
        return fields.isEmpty()
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("\n")
        output.append("input ${namePrefix}FilterInput {\n")
        output.append(fields.joinToString(separator = "\n", postfix = "\n"))
        output.append("}\n")
        return output.toString()
    }
}
