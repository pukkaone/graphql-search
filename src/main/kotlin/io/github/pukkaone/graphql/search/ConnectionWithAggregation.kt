package io.github.pukkaone.graphql.search

import graphql.relay.Edge
import graphql.relay.PageInfo

typealias Hit = Map<String, Any>

/**
 * Connection and aggregation fields.
 */
data class ConnectionWithAggregation(
    val edges: List<Edge<Hit>>,
    val pageInfo: PageInfo,
    val groupBy: Map<String, Any>
)
