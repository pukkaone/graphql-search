package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class SearchTest : IntegrationTestSupport() {

    @BeforeAll
    fun beforeAll() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        putDocument(documentId = "transaction:1", bathrooms = 1.0, bedrooms = 1, description = "Hello, world")
        putDocument(documentId = "transaction:2", bathrooms = 2.0, bedrooms = 2, valuationPrice = null)
        putDocument(documentId = "transaction:3", bathrooms = 3.0, bedrooms = 3)
    }

    @Test
    fun `when filter contains then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    listing: {
                      description: {
                        contains: "hello"
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1")
    }

    @Test
    fun `when filter eq then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        eq: 1
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1")
    }

    @Test
    fun `when filter exists false then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    listing: {
                      valuation_price: {
                        exists: false
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2")
    }

    @Test
    fun `when filter exists true then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    listing: {
                      valuation_price: {
                        exists: true
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1", "transaction:3")
    }

    @Test
    fun `when filter geo_distance then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      location: {
                        position: {
                          geo_distance: {
                            center: {
                              lat: 35
                              lon: -118.413516
                            }
                            lte: 70
                            unit: MILES
                          }
                        }
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1", "transaction:2", "transaction:3")
    }

    @Test
    fun `when filter gt then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        gt: 1
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2", "transaction:3")
    }

    @Test
    fun `when filter gte then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        gte: 2
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2", "transaction:3")
    }

    @Test
    fun `when filter in then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        in: [1, 2]
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1", "transaction:2")
    }

    @Test
    fun `when filter lt then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        lt: 3
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1", "transaction:2")
    }

    @Test
    fun `when filter lte then find hit`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        lte: 2
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:1", "transaction:2")
    }

    @Test
    fun `when filter multiple operators on same field then execute range query`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bedrooms: {
                        gte: 2
                        lte: 3
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2", "transaction:3")
    }

    @Test
    fun `when filter multiple fields then evaluate logical and`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    property: {
                      bathrooms: {
                        lte: 2.0
                      }
                      bedrooms: {
                        gte: 2
                      }
                    }
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2")
    }

    @Test
    fun `when filter and then all conditions are true`() {
        val transactions = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    and: [
                      {
                        property: {
                          bathrooms: {
                            gte: 2.0
                          }
                        }
                      }
                      {
                        property: {
                          bedrooms: {
                            gte: 2
                          }
                        }
                      }
                    ]
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions.get()).containsOnly("transaction:2", "transaction:3")
    }

    @Test
    fun `when filter not then all conditions are false`() {
        val transactions1 = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    not: [
                      {
                        property: {
                          bathrooms: {
                            lte: 1.0
                          }
                        }
                      }
                      {
                        property: {
                          bedrooms: {
                            gte: 3
                          }
                        }
                      }
                    ]
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions1.get()).containsOnly("transaction:2")
    }

    @Test
    fun `when filter or then at least one condition is true`() {
        val transactions1 = graphQlTester.document("""
            query {
              transactionConnection(
                  index: "transaction-write"
                  filter: {
                    or: [
                      {
                        property: {
                          bathrooms: {
                            lte: 1.0
                          }
                        }
                      }
                      {
                        property: {
                          bedrooms: {
                            gte: 3
                          }
                        }
                      }
                    ]
                  }
              ) {
                edges {
                  node {
                    transaction_urn
                  }
                }
              }
            }
            """)
            .execute()
            .path("transactionConnection.edges[*].node.transaction_urn")
            .entityList(String::class.java)
        assertThat(transactions1.get()).containsOnly("transaction:1", "transaction:3")
    }
}
