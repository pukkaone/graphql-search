package io.github.pukkaone.graphql.search.schema

import graphql.language.Type
import graphql.schema.idl.TypeUtil

/**
 * Returns the base type name after unwrapping non-null and list wrappers.
 */
fun Type<*>.toBaseTypeName(): String {
    return TypeUtil.unwrapAll(this).name
}
