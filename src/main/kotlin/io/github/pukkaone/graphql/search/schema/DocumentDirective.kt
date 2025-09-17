package io.github.pukkaone.graphql.search.schema

import graphql.language.ObjectTypeDefinition

/**
 * Marks an object to be stored in a search index.
 */
object DocumentDirective {

    private const val NAME = "document"

    /**
     * Checks if object type definition is marked as document.
     *
     * @param objectTypeDefinition
     *   object type definition
     * @return true if object is document
     */
    fun isPresent(objectTypeDefinition: ObjectTypeDefinition): Boolean {
        return objectTypeDefinition.hasDirective(NAME)
    }
}
