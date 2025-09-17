package io.github.pukkaone.graphql.search

import org.elasticsearch.action.bulk.BulkResponse

/**
 * Thrown when a bulk response has failures.
 */
class BulkResponseFailureException(
    val bulkResponse: BulkResponse,
) : RuntimeException("Bulk response has failures")
