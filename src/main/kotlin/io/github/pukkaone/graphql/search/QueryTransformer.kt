package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.search.model.GeoDistanceRange
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.RangeQueryBuilder

private val MATCH_ALL_QUERY = QueryBuilders.matchAllQuery()

/**
 * Transforms to search engine query.
 */
class QueryTransformer {

    private val path = mutableListOf<String>()
    private var query: QueryBuilder = MATCH_ALL_QUERY
    private var rangeQuery: RangeQueryBuilder? = null

    private fun fieldPath(): String {
        return path.joinToString(".")
    }

    private fun merge(newQuery: QueryBuilder) {
        if (query === MATCH_ALL_QUERY) {
            query = newQuery
        } else {
            query.let {
                if (it is BoolQueryBuilder) {
                    it.must(newQuery)
                } else {
                    query = QueryBuilders.boolQuery()
                        .must(query)
                        .must(newQuery)
                }
            }
        }
    }

    private fun and(filters: List<Map<String, Any>>) {
        if (filters.isEmpty()) {
            return
        }

        val boolQuery = QueryBuilders.boolQuery()
        for (filter in filters) {
            val subQuery = toQuery(filter)
            boolQuery.must(subQuery)
        }

        merge(boolQuery)
    }

    private fun not(filters: List<Map<String, Any>>) {
        if (filters.isEmpty()) {
            return
        }

        val boolQuery = QueryBuilders.boolQuery()
            .minimumShouldMatch(1)
        for (filter in filters) {
            val subQuery = toQuery(filter)
            boolQuery.mustNot(subQuery)
        }

        merge(boolQuery)
    }

    private fun or(filters: List<Map<String, Any>>) {
        if (filters.isEmpty()) {
            return
        }

        val boolQuery = QueryBuilders.boolQuery()
            .minimumShouldMatch(1)
        for (filter in filters) {
            val subQuery = toQuery(filter)
            boolQuery.should(subQuery)
        }

        merge(boolQuery)
    }

    private fun contains(values: List<Any>) {
        if (values.isEmpty()) {
            return
        }

        val boolQuery = QueryBuilders.boolQuery()
            .minimumShouldMatch(1)
        val field = fieldPath()
        for (value in values) {
            val subQuery = QueryBuilders.matchQuery(field, value)
            boolQuery.should(subQuery)
        }

        merge(boolQuery)
    }

    private fun eq(value: Any) {
        val termQuery = QueryBuilders.termQuery(fieldPath(), value)
        merge(termQuery)
    }

    private fun exists(value: Any) {
        val existsQuery = QueryBuilders.existsQuery(fieldPath())
        if (value == false) {
            val boolQuery = QueryBuilders.boolQuery()
                .mustNot(existsQuery)
            merge(boolQuery)
        } else {
            merge(existsQuery)
        }
    }

    private fun geoDistance(value: Map<String, Any>) {
        val geoDistanceRange = GeoDistanceRange(value)
        val geoDistanceQuery = QueryBuilders.geoDistanceQuery(fieldPath())
            .distance(geoDistanceRange.lte.toString(), geoDistanceRange.unit)
            .point(geoDistanceRange.center.latitude, geoDistanceRange.center.longitude)
        merge(geoDistanceQuery)
    }

    private fun getRangeQuery(): RangeQueryBuilder {
        if (rangeQuery == null) {
            rangeQuery = QueryBuilders.rangeQuery(fieldPath())
        }

        return requireNotNull(rangeQuery)
    }

    private fun mergeRangeQuery() {
        rangeQuery?.let {
            merge(it)
            rangeQuery = null
        }
    }

    private fun inOperator(values: List<Any>) {
        val termQuery = QueryBuilders.termsQuery(fieldPath(), values)
        merge(termQuery)
    }

    @Suppress("UNCHECKED_CAST")
    private fun pushField(field: String, value: Any) {
        path.add(field)
        transformOperatorToQuery(value as Map<String, Any>)
        path.removeLast()
    }

    @Suppress("UNCHECKED_CAST")
    private fun transformOperatorToQuery(filter: Map<String, Any>) {
        for ((key, value) in filter) {
            when (key) {
                "and" ->
                    and(value as List<Map<String, Any>>)
                "not" ->
                    not(value as List<Map<String, Any>>)
                "or" ->
                    or(value as List<Map<String, Any>>)
                "contains" ->
                    contains(value as List<Any>)
                "eq" ->
                    eq(value)
                "exists" ->
                    exists(value)
                "geo_distance" ->
                    geoDistance(value as Map<String, Any>)
                "gt" ->
                    getRangeQuery().gt(value)
                "gte" ->
                    getRangeQuery().gte(value)
                "in" ->
                    inOperator(value as List<Any>)
                "lt" ->
                    getRangeQuery().lt(value)
                "lte" ->
                    getRangeQuery().lte(value)
                else ->
                    pushField(key, value)
            }
        }

        mergeRangeQuery()
    }

    companion object {
        /**
         * Transforms to search engine query.
         *
         * @param filter
         *   filter input
         */
        fun toQuery(filter: Map<String, Any>): QueryBuilder {
            val queryTransformer = QueryTransformer()
            queryTransformer.transformOperatorToQuery(filter)
            return queryTransformer.query
        }
    }
}
