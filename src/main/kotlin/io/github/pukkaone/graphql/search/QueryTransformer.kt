package io.github.pukkaone.graphql.search

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
        if (newQuery === MATCH_ALL_QUERY) {
            query = newQuery
        }

        query = QueryBuilders.boolQuery()
            .must(newQuery)
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

    private fun getRangeQuery(): RangeQueryBuilder {
        if (rangeQuery == null) {
            rangeQuery = QueryBuilders.rangeQuery(fieldPath())
        }

        return requireNotNull(rangeQuery)
    }

    private fun inOperator(values: List<Any>) {
        val termQuery = QueryBuilders.termsQuery(fieldPath(), values)
        merge(termQuery)
    }

    @Suppress("UNCHECKED_CAST")
    private fun visit(filter: Map<String, Any>) {
        for ((field, value) in filter) {
            when (field) {
                "and" ->
                    and(value as List<Map<String, Any>>)
                "not" ->
                    not(value as List<Map<String, Any>>)
                "or" ->
                    or(value as List<Map<String, Any>>)
                "eq" ->
                    eq(value)
                "exists" ->
                    exists(value)
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
                else -> {
                    path.add(field)
                    visit(value as Map<String, Any>)
                    path.removeLast()
                }
            }
        }

        rangeQuery?.let {
            merge(it)
            rangeQuery = null
        }
    }

    companion object {
        /**
         * Transforms to search engine query.
         *
         * @param filter
         *   filter input
         */
        fun toQuery(filter: Map<String, Any>): QueryBuilder {
            // If no filter provided, then match all documents.
            if (filter.isEmpty()) {
                return QueryBuilders.matchAllQuery()
            }

            val queryTransformer = QueryTransformer()
            queryTransformer.visit(filter)
            return queryTransformer.query
        }
    }
}
