package io.github.pukkaone.graphql.search.schema

import graphql.language.FieldDefinition

/**
 * Marks a field contains the document ID.
 */
object IdDirective {

    private const val NAME = "id"

    /**
     * Checks if field definition is marked as document ID.
     *
     * @param fieldDefinition
     *   field definition
     * @return true if field contains the document ID
     */
    fun isPresent(fieldDefinition: FieldDefinition): Boolean {
        return fieldDefinition.hasDirective(NAME)
    }
}
