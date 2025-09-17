package io.github.pukkaone.graphql.search.schema

import graphql.language.Directive
import graphql.language.FieldDefinition

/**
 * Marks a field to be searchable.
 */
object SearchableDirective {

    private const val NAME = "searchable"
    const val TYPE = "type"
    const val ANALYZER = "analyzer"
    const val SEARCH_ANALYZER = "search_analyzer"
    const val FORMAT = "format"
    const val IGNORE_ABOVE = "ignore_above"

    /**
     * Checks if field definition is marked as searchable.
     *
     * @param fieldDefinition
     *   field definition
     * @return true if field is searchable
     */
    fun isPresent(fieldDefinition: FieldDefinition): Boolean {
        return fieldDefinition.hasDirective(NAME)
    }

    /**
     * Gets directive from field definition.
     *
     * @param fieldDefinition
     *   field definition
     * @return directive, or null if not found
     */
    fun getDirectiveOrNull(fieldDefinition: FieldDefinition): Directive? {
        return fieldDefinition.getDirectives(NAME).firstOrNull()
    }
}
