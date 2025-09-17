package io.github.pukkaone.graphql

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot application entry point.
 */
@SpringBootApplication
class ServerApplication

/**
 * Spring Boot application entry point.
 *
 * @param args
 *   command line arguments
 */
fun main(args: Array<String>) {
    runApplication<ServerApplication>(args = args)
}
