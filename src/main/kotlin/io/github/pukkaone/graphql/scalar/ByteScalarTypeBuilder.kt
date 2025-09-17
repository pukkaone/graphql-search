package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.language.ScalarTypeDefinition
import graphql.scalars.ExtendedScalars
import graphql.schema.GraphQLScalarType

/**
 * Custom GraphQL scalar type that produces java.lang.Byte objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class ByteScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar(ExtendedScalars.GraphQLByte)
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(ExtendedScalars.GraphQLByte.name)
                    .build()
            )
            .build()
    }
}
