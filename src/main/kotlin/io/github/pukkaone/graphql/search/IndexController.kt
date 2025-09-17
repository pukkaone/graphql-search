package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.search.schema.ProtoSchema
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.ContextValue
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

/**
 * Processes mutations on search index.
 */
@Controller
class IndexController(
    private val aliasService: AliasService,
    private val createIndexWithAliasUseCase: CreateIndexWithAliasUseCase
) {
    /**
     * Creates a search index and assigns an alias to it.
     */
    @MutationMapping
    fun createIndexWithAlias(
        @Argument type: String,
        @Argument index: String,
        @Argument alias: String,
        @ContextValue protoSchema: ProtoSchema,
    ): Boolean {
        return createIndexWithAliasUseCase(type, index, alias, protoSchema)
    }

    /**
     * Assigns an alias to a search index.  If the alias already exists, then moves it to the new search index.
     */
    @MutationMapping
    fun assignAlias(
        @Argument index: String,
        @Argument alias: String,
    ): Boolean {
        aliasService.assignAlias(index, alias)
        return true
    }
}
