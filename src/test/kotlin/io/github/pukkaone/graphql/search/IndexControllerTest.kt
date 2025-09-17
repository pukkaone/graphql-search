package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IndexControllerTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var aliasService: AliasService

    @Test
    fun `when create index with alias then index exists`() {
        val index1 = "transaction-${faker.number().positive()}"
        createIndex(index1)

        var actualIndex = aliasService.findIndexForAlias("transaction-write")
        assertThat(actualIndex).isEqualTo(index1)

        val index2 = "transaction-${faker.number().positive()}"
        createIndex(index2)

        actualIndex = aliasService.findIndexForAlias("transaction-write")
        assertThat(actualIndex).isEqualTo(index2)
    }

    @Test
    fun `when assign alias then alias exists`() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        graphQlTester.document("""
            mutation {
              assignAlias(
                  index: "$index"
                  alias: "transaction"
              )
            }
            """)
            .execute()
            .path("assignAlias")
            .entity(Boolean::class.java)
            .isEqualTo(true)

        val actualIndex = aliasService.findIndexForAlias("transaction")
        assertThat(actualIndex).isEqualTo(index)
    }

    @Test
    fun `when get document then fetched document`() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        putDocument("transaction:1")

        graphQlTester.document("""
            query {
              transaction(
                  index: "transaction-write"
                  ids: "transaction:1"
              ) {
                property {
                  location {
                    address {
                      postal_code
                    }
                  }
                }
              }
            }
            """)
            .execute()
            .path("transaction[0].property.location.address.postal_code")
            .entity(String::class.java)
            .isEqualTo("90210")
    }
}
