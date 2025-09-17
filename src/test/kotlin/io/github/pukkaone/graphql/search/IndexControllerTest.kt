package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.IntegrationTestSupport
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class IndexControllerTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var aliasService: AliasService

    private fun createIndex(index: String) {
        graphQlTester.document("""
            mutation {
              createIndexWithAlias(
                  type: "Transaction"
                  index: "$index"
                  alias: "transaction-write"
              )
            }
            """)
            .execute()
            .path("createIndexWithAlias")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }

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

    private fun putDocument(documentId: String) {
        graphQlTester.document("""
            mutation {
              putTransaction(
                  index: "transaction-write"
                  documents: {
                    transaction_urn: "$documentId"
                    listing: {
                      listing_urn: "listing:2"
                      description: "description"
                      reserve_price: 2
                      valuation_price: 1
                    }
                    property: {
                      property_urn: "property:3"
                      location: {
                        address: {
                          line: "9641 Sunset Blvd"
                          city: "Beverly Hills"
                          state: "CA"
                          postal_code: "90210"
                          country: "US"
                        }
                        position: {
                          lat: 34.08492556624647
                          lon: -118.4135163566971
                        }
                      }
                      bathrooms: 2
                      bedrooms: 3
                    }
                  }
              )
            }
            """)
            .execute()
            .path("putTransaction")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }

    @Test
    fun `when get document then fetched document`() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        putDocument("transaction:1")

        graphQlTester.document("""
            query {
              transaction(
                  index: "$index"
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

    @Test
    fun `when search documents then find hit`() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        putDocument("transaction:1")

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

        TimeUnit.SECONDS.sleep(1)
        graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction"
                  filter: {
                    property: {
                      location: {
                        address: {
                          state: {
                            eq: "CA"
                          }
                        }
                      }
                    }
                  }
                  sort: {
                    field: "property.bedrooms"
                    direction: ASC
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
                pageInfo {
                  endCursor
                  hasNextPage
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[0].node.transaction_urn")
            .entity(String::class.java)
            .isEqualTo("transaction:1")
    }
}
