package io.github.pukkaone.graphql.search.model

/**
 * Request to sort by distance from a geographic location.
 */
data class SortGeoDistance(private val fieldNameToValueMap: Map<String, Any>) {

    val center: GeoPoint
        get() = GeoPoint(fieldNameToValueMap["center"] as Map<String, Any>)
}
