package io.github.pukkaone.graphql.search.model

import org.springframework.data.domain.Sort

/**
 * Request to sort by a field, distance from a geographic point, or script.
 */
data class SortField(private val fieldNameToValueMap: Map<String, Any>) {

    val field: String?
        get() = fieldNameToValueMap["field"] as String?

    val geoDistance: SortGeoDistance?
        get() = fieldNameToValueMap["geoDistance"]?.let { SortGeoDistance(it as Map<String, Any>) }

    val script: SortScript?
        get() = fieldNameToValueMap["script"]?.let { SortScript(it as Map<String, Any>) }

    val direction: Sort.Direction
        get() = fieldNameToValueMap["direction"]
            ?.let { Sort.Direction.fromString(it as String) }
            ?: Sort.DEFAULT_DIRECTION
}
