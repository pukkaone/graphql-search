package io.github.pukkaone.graphql.search

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Search configuration properties.
 */
@ConfigurationProperties(prefix = "search")
data class SearchProperties(
    val apiVersions: List<String>,
)
