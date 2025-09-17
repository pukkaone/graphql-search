package io.github.pukkaone.graphql.search.schema

import graphql.language.ObjectTypeDefinition

/**
 * Indicates an object type is the root object of a document to be stored in a search index.
 */
object DocumentDirective {

    private const val NAME = "document"

    /**
     * Checks if the directive is present on an object type definition.
     *
     * @param objectTypeDefinition
     *   object type definition
     * @return whether the object type definition has the directive
     */
    fun isPresent(objectTypeDefinition: ObjectTypeDefinition): Boolean {
        return objectTypeDefinition.hasDirective(NAME)
    }
}
