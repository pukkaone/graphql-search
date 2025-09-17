package io.github.pukkaone.graphql.search

import io.github.pukkaone.graphql.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
class GroupByTest : IntegrationTestSupport() {

    @BeforeAll
    fun beforeAll() {
        val index = "transaction-${faker.number().positive()}"
        createIndex(index)

        putDocument(documentId = "transaction:1", bathrooms = 1.0, bedrooms = 1)
        putDocument(documentId = "transaction:2", bathrooms = 2.0, bedrooms = 1)
        putDocument(documentId = "transaction:3", bathrooms = 3.0, bedrooms = 2)
    }

    @Test
    fun `when group by then count by each field value`() {
        val buckets = graphQlTester
            .document(
                """
                query {
                  transactionSearch(
                      index: "transaction-write"
                      groupBy: {
                        property: {
                          bedrooms: {
                            terms: {
                              first: 10
                            }
                          }
                        }
                      }
                  ) {
                    groupBy {
                      property {
                        bedrooms {
                          key
                          count
                        }
                      }
                    }
                  }
                }
                """
            )
            .execute()
            .path("transactionSearch.groupBy.property.bedrooms")
            .entityList(Bucket::class.java)
            .get()
        assertThat(buckets).containsExactlyInAnyOrder(
            Bucket("1", 2),
            Bucket("2", 1)
        )
    }
}
