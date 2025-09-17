package io.github.pukkaone.graphql

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@TestConfiguration
class GraphQlTesterConfiguration {

    @Bean
    fun graphQlTester(
        applicationContext: WebApplicationContext,
    ): HttpGraphQlTester {
        val client = MockMvcWebTestClient.bindToApplicationContext(applicationContext)
            .configureClient()
            .baseUrl("/graphql/v2019_12_31")
            .build()
        return HttpGraphQlTester.create(client)
    }
}
