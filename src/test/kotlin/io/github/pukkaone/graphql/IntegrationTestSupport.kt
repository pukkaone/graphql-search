package io.github.pukkaone.graphql

import java.math.BigDecimal
import net.datafaker.Faker
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
    protected lateinit var graphQlTester: HttpGraphQlTester

    protected fun createIndex(
        index: String,
        alias: String = "transaction-write",
    ) {
        graphQlTester.document("""
            mutation {
              createIndexWithAlias(
                  type: "Transaction"
                  index: "$index"
                  alias: "$alias"
              )
            }
            """)
            .execute()
            .path("createIndexWithAlias")
            .entity(Boolean::class.java)
            .isEqualTo(true)
    }

    protected fun putDocument(
        documentId: String,
        bathrooms: Double = 2.0,
        bedrooms: Int = 3,
        description: String = "DESCRIPTION",
        valuationPrice: BigDecimal? = BigDecimal.ONE,
    ) {
        graphQlTester.document("""
            mutation {
              putTransaction(
                  index: "transaction-write"
                  documents: {
                    transaction_urn: "$documentId"
                    listing: {
                      listing_urn: "listing:2"
                      description: "$description"
                      reserve_price: 2
                      valuation_price: $valuationPrice
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
                          lat: 34.084925
                          lon: -118.413516
                        }
                      }
                      bathrooms: $bathrooms
                      bedrooms: $bedrooms
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
}
