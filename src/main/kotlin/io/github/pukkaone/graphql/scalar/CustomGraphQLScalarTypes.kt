package io.github.pukkaone.graphql.scalar

import graphql.schema.GraphQLScalarType
import java.util.ServiceLoader
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Custom GraphQL scalar type collection.
 */
object CustomGraphQLScalarTypes {

    private val nameToScalarTypeMap: Map<String, GraphQLScalarType> =
        ServiceLoader.load(GraphQLScalarTypeBuilder::class.java)
            .stream()
            .map { it.get().build() }
            .collect(
                Collectors.toMap(
                    { it.name },
                    Function.identity()
                )
            )

    val scalarTypes: Collection<GraphQLScalarType>
        get() = nameToScalarTypeMap.values
}
