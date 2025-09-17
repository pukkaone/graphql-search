package io.github.pukkaone.graphql.search

/**
 * Aggregation result.
 */
data class Aggregation(
    val groupBy: String,
    val buckets: List<Bucket>,
)
