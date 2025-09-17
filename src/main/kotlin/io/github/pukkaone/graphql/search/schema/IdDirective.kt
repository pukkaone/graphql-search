package io.github.pukkaone.graphql.search.schema

import graphql.language.FieldDefinition

/**
 * Indicates a field definition contains the document ID.
 */
object IdDirective {

    private const val NAME = "id"

    /**
     * Checks if the directive is present on a field definition.
     *
     * @param fieldDefinition
     *   field definition
     * @return whether the field contains the document ID
     */
    fun isPresent(fieldDefinition: FieldDefinition): Boolean {
        return fieldDefinition.hasDirective(NAME)
    }
}
