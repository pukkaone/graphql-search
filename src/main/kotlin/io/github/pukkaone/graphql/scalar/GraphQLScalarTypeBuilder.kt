package io.github.pukkaone.graphql.scalar

import graphql.schema.GraphQLScalarType

/**
 * [java.util.ServiceLoader] interface to build GraphQL scalar type.
 */
interface GraphQLScalarTypeBuilder {

    /**
     * Builds GraphQL scalar type.
     *
     * @return GraphQL scalar type
     */
    fun build(): GraphQLScalarType
}
