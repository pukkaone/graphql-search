package io.github.pukkaone.graphql.search.model

/**
 * Location on the Earth's surface specified by latitude and longitude coordinates.
 */
data class GeoPoint(private val fieldNameToValueMap: Map<String, Any>) {

    val latitude: Double
        get() = fieldNameToValueMap["lat"] as Double

    val longitude: Double
        get() = fieldNameToValueMap["lon"] as Double
}
