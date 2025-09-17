package io.github.pukkaone.graphql.configuration

import io.github.pukkaone.graphql.scalar.CustomGraphQLScalarTypes
import io.github.pukkaone.graphql.search.SearchProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

/**
 * Configures search GraphQL.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SearchProperties::class)
class SearchConfiguration {

    @Bean
    fun customScalarTypeConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { runtimeWiringBuilder ->
            for (scalarType in CustomGraphQLScalarTypes.scalarTypes) {
                runtimeWiringBuilder.scalar(scalarType)
            }
        }
    }
}
