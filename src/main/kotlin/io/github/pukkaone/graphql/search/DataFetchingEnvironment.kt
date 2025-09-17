package io.github.pukkaone.graphql.search

import graphql.schema.DataFetchingEnvironment

/**
 * Gets argument value, or default value if not present.
 */
fun <T> DataFetchingEnvironment.getArgumentOrElse(key: String, defaultValue: () -> T): T {
    return (this.getArgument(key) as T?) ?: defaultValue()
}

/**
 * Gets required argument value.
 *
 * @throws IllegalArgumentException
 *   if argument is not present
 */
fun <T> DataFetchingEnvironment.requireArgument(key: String): T {
    return requireNotNull(this.getArgument(key) as T?)
}
