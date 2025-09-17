package io.github.pukkaone.graphql.search

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.bulk.BulkItemResponse
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.graphql.execution.ErrorType
import org.springframework.web.bind.annotation.ControllerAdvice

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
            .message(exception.toString())
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
