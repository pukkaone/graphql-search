package io.github.pukkaone.graphql.configuration

import graphql.Scalars
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.github.pukkaone.graphql.apiversion.ApiVersionGraphQlHttpHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.server.WebGraphQlHandler

/**
 * Configures API versioning by URL path.
 */
@Configuration(proxyBeanMethods = false)
class ApiVersionConfiguration {

    /**
     * Defines a fake GraphQlSource bean so Spring Boot auto-configuration does not try to define one.
     * Spring Boot auto-configuration would throw an exception because there are no schema files in
     * the default location `classpath:graphql/**/`.
     */
    @Bean
    fun fakeGraphQlSource(): GraphQlSource {
        val query = GraphQLObjectType.Builder()
            .name("Query")
            .field(
                GraphQLFieldDefinition.Builder()
                    .name("queryTypeMustDefineAtLeastOneField")
                    .type(Scalars.GraphQLBoolean)
            )
        val schema = GraphQLSchema.newSchema()
            .query(query)
            .build()
        return GraphQlSource.builder(schema)
            .build()
    }

    @Bean
    fun graphQlHttpHandler(webGraphQlHandler: WebGraphQlHandler): ApiVersionGraphQlHttpHandler {
        return ApiVersionGraphQlHttpHandler(webGraphQlHandler)
    }
}
