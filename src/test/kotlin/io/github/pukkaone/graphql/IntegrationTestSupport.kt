package io.github.pukkaone.graphql

import net.datafaker.Faker
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.HttpGraphQlTester

@AutoConfigureMockMvc
@Import(ElasticsearchTestConfiguration::class, GraphQlTesterConfiguration::class)
@SpringBootTest
abstract class IntegrationTestSupport {

    protected val faker = Faker()

    @Autowired
    protected lateinit var elasticsearchClient: RestHighLevelClient

    @Autowired
    protected lateinit var graphQlTester: HttpGraphQlTester
}
