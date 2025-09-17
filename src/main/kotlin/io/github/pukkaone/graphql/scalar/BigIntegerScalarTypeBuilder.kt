package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.language.ScalarTypeDefinition
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType

/**
 * Custom GraphQL scalar type that produces java.math.BigInteger objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class BigIntegerScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar(ExtendedScalars.GraphQLBigInteger)
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(ExtendedScalars.GraphQLBigInteger.name)
                    .build()
            )
            .build()
    }
}
