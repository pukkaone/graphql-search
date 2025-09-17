package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.language.ScalarTypeDefinition
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType

/**
 * Custom GraphQL scalar type that accepts strings formatted like HH:mm:ss
 * and produces java.time.LocalTime objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class LocalTimeScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar(ExtendedScalars.LocalTime)
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(ExtendedScalars.LocalTime.name)
                    .build()
            )
            .build()
    }
}
