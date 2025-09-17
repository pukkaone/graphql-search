package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.language.ScalarTypeDefinition
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType

/**
 * Custom GraphQL scalar type that accepts strings formatted like yyyy-MM-dd
 * and produces java.time.LocalDate objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class DateScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar(ExtendedScalars.Date)
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(ExtendedScalars.Date.name)
                    .build()
            )
            .build()
    }
}
