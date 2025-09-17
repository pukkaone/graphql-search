package io.github.pukkaone.graphql.search.model

/**
 * Request to sort by a stored script.
 */
data class SortScript(private val fieldNameToValueMap: Map<String, Any>) {

    val name: String
        get() = fieldNameToValueMap["name"] as String

    val type: String
        get() = fieldNameToValueMap["type"] as String

    val arguments: String?
        get() = fieldNameToValueMap["arguments"] as String?
}
