package io.github.pukkaone.graphql.search.model

import org.elasticsearch.common.unit.DistanceUnit

/**
 * Parameters for geo distance operator.
 */
data class GeoDistanceRange(private val fieldNameToValueMap: Map<String, Any>) {

    val center: GeoPoint
        get() = GeoPoint(fieldNameToValueMap["center"] as Map<String, Any>)

    val lte: Any
        get() = requireNotNull(fieldNameToValueMap["lte"])

    val unit: DistanceUnit
        get() = fieldNameToValueMap["unit"]
            ?.let { DistanceUnit.fromString((it as String).lowercase()) }
            ?: DistanceUnit.MILES
}
