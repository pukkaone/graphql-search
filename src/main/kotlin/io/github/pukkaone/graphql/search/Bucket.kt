package io.github.pukkaone.graphql.search

/**
 * Bucket in aggregation result.
 */
data class Bucket(
    val key: String,
    val count: Int,
    val aggregations: Map<String, Any> = emptyMap(),
)
