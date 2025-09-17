package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.language.ScalarTypeDefinition
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType

/**
 * Custom GraphQL scalar type that produces java.math.BigDecimal objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class BigDecimalScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar(ExtendedScalars.GraphQLBigDecimal)
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(ExtendedScalars.GraphQLBigDecimal.name)
                    .build()
            )
            .build()
    }
}
