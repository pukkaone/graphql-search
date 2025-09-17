package io.github.pukkaone.graphql.search

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.pukkaone.graphql.search.model.SortField
import io.github.pukkaone.graphql.search.model.SortGeoDistance
import io.github.pukkaone.graphql.search.model.SortScript
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.sort.ScoreSortBuilder
import org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType
import org.elasticsearch.search.sort.SortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.domain.Sort
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

private val OBJECT_MAPPER: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

/**
 * Translates to search engine sort.
 */
object SortTransformer {

    private fun toFieldSort(field: String): SortBuilder<*> {
        return if (field == ScoreSortBuilder.NAME) {
            SortBuilders.scoreSort()
        } else {
            SortBuilders.fieldSort(field)
        }
    }

    private fun toGeoDistanceSort(field: String, geoDistance: SortGeoDistance): SortBuilder<*> {
        return SortBuilders.geoDistanceSort(
            field,
            geoDistance.center.latitude,
            geoDistance.center.longitude,
        )
            .unit(DistanceUnit.MILES)
    }

    private fun toScriptSort(inputScript: SortScript): SortBuilder<*> {
        val arguments: Map<String, Any> = inputScript.arguments
            ?.let { OBJECT_MAPPER.readValue(it, object : TypeReference<Map<String, Any>>() {}) }
            ?: emptyMap()
        val elasticsearchScript = Script(
            ScriptType.STORED,
            null,
            inputScript.name,
            arguments,
        )
        return SortBuilders.scriptSort(elasticsearchScript, ScriptSortType.fromString(inputScript.type))
    }

    private fun toSortOrder(inputOrder: Sort.Direction): SortOrder {
        return SortOrder.valueOf(inputOrder.name)
    }

    /**
     * Translates to search engine sort.
     *
     * @param inputSort
     *   to translate from
     * @return search engine sort
     */
    fun toSort(inputSort: SortField): SortBuilder<*> {
        val sort: SortBuilder<*> = if (inputSort.geoDistance != null) {
            toGeoDistanceSort(requireNotNull(inputSort.field), requireNotNull(inputSort.geoDistance))
        } else if (inputSort.script != null) {
            toScriptSort(requireNotNull(inputSort.script))
        } else if (inputSort.field != null) {
            toFieldSort(requireNotNull(inputSort.field))
        } else {
            error("Must specify one of: field, geoDistance, script")
        }

        return sort.order(toSortOrder(inputSort.direction))
    }
}
