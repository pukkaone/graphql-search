package io.github.pukkaone.graphql.scalar

import com.google.auto.service.AutoService
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.ScalarTypeDefinition
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

private val COERCING = object : Coercing<LocalDateTime, String> {

    override fun serialize(input: Any, context: GraphQLContext, locale: Locale): String {
        if (input is LocalDateTime) {
            try {
                return input.toString()
            } catch (e: DateTimeException) {
                throw CoercingSerializeException("Cannot format LocalDateTime $input", e)
            }
        } else {
            throw CoercingSerializeException("Expected value of type LocalDate but was ${input.javaClass}")
        }
    }

    override fun parseValue(input: Any, context: GraphQLContext, locale: Locale): LocalDateTime {
        return when (input) {
            is LocalDateTime ->
                input

            is String ->
                try {
                    LocalDateTime.parse(input.toString())
                } catch (e: DateTimeParseException) {
                    throw CoercingParseValueException("Cannot parse [$input] to LocalDateTime", e)
                }

            else ->
                throw CoercingParseValueException("Expected input value of type String but was ${input.javaClass}")
        }
    }

    override fun parseLiteral(
        input: Value<*>, variables: CoercedVariables, context: GraphQLContext, locale: Locale
    ): LocalDateTime {
        if (input is StringValue) {
            try {
                return LocalDateTime.parse(input.value)
            } catch (e: DateTimeParseException) {
                throw CoercingParseLiteralException("Cannot parse [$input] to LocalDateTime", e)
            }
        } else {
            throw CoercingParseLiteralException("Expected literal of type StringValue but was ${input.javaClass}")
        }
    }
}

/**
 * Custom GraphQL scalar type that accepts strings formatted like yyyy-MM-dd'T'HH:mm:ss
 * and produces java.time.LocalDateTime objects at runtime.
 */
@AutoService(GraphQLScalarTypeBuilder::class)
class LocalDateTimeScalarTypeBuilder : GraphQLScalarTypeBuilder {

    override fun build(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name(LocalDateTime::class.simpleName)
            .description(
                "Date and time without time zone as used in human communication. " +
                "Value is a string formatted as yyyy-MM-dd'T'HH:mm:ss"
            )
            .definition(
                ScalarTypeDefinition.newScalarTypeDefinition()
                    .name(LocalDateTime::class.simpleName)
                    .build()
            )
            .coercing(COERCING)
            .build()
    }
}
