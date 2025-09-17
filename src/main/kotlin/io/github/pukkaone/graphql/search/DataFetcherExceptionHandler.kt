package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import java.io.IOException
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.client.ResponseException
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.graphql.execution.ErrorType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.bind.annotation.ControllerAdvice

private val OBJECT_MAPPER: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

private fun ElasticsearchException.extractReason(): String {
    if (this.suppressed.isEmpty()) {
        return this.detailedMessage
    }

    val throwable = this.suppressed[0]
    if (throwable is ResponseException) {
        var reason: String? = null
        try {
            val response: JsonNode = OBJECT_MAPPER.readTree(throwable.response.entity.content)
            reason = response
                .path("error")
                .path("root_cause")
                .path(0)
                .path("reason")
                .asText()
            if (reason == "runtime error") {
                reason = response
                    .path("error")
                    .path("failed_shards")
                    .path(0)
                    .path("reason")
                    .path("caused_by")
                    .path("reason")
                    .asText()
            }
        } catch (_: IOException) {
            // ignore
        }

        return reason ?: this.detailedMessage
    }

    return requireNotNull(throwable.message)
}

/**
 * Handles exception thrown by data fetcher.
 */
@ControllerAdvice
class DataFetcherExceptionHandler {

    /**
     * Fills errors in response when bulk request failed.
     */
    @GraphQlExceptionHandler
    fun onBulkResponseFailureException(
        errorBuilder: GraphqlErrorBuilder<*>,
        exception: BulkResponseFailureException,
    ): Collection<GraphQLError> {
        return exception.bulkResponse
            .items
            .filter(BulkItemResponse::isFailed)
            .map {
                errorBuilder
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .extensions(
                        mapOf(
                            "id" to it.id,
                            "index" to it.index,
                        )
                    )
                    .message(it.failureMessage)
                    .build()
            }
    }

    /**
     * Fills errors in response when Elasticsearch exception thrown.
     */
    @GraphQlExceptionHandler
    fun onElasticsearchException(
        errorBuilder: GraphqlErrorBuilder<*>,
        exception: ElasticsearchException,
    ): GraphQLError {
        return errorBuilder
            .errorType(ErrorType.INTERNAL_ERROR)
            .extensions(
                exception.metadataKeys
                    .associateWith { exception.getMetadata(it) }
            )
            .message(exception.extractReason())
            .build()
    }

    /**
     * Fills errors in response when IllegalStateException thrown.
     */
    @GraphQlExceptionHandler
    fun onIllegalStateException(
        errorBuilder: GraphqlErrorBuilder<*>,
        exception: IllegalStateException,
    ): GraphQLError {
        return errorBuilder
            .errorType(ErrorType.INTERNAL_ERROR)
            .message(exception.message)
            .build()
    }
}
