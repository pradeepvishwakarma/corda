package com.example

import com.example.braid.BraidService
import io.bluebank.braid.client.BraidClient
import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.onSuccess
import io.vertx.core.Vertx
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.CountDownLatch

/**
 * This file is exclusively for being able to run your nodes through an IDE.
 * Do not use in a production environment.
 */
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("DSL Test")
    val user = User("user1", "test", permissions = setOf("ALL"))
    driver(DriverParameters(waitForAllNodesToFinish = true, startNodesInProcess = true)) {
        val partyA = CordaX500Name("PartyA", "London", "GB")
        val partyB = CordaX500Name("PartyB", "New York", "US")
        val nodeFutures = listOf(
                startNode(
                        providedName = partyA,
                        customOverrides = mapOf("rpcSettings.address" to "localhost:10008", "rpcSettings.adminAddress" to "localhost:10048", "webAddress" to "localhost:10009"),
                        rpcUsers = listOf(user)),
                startNode(
                        providedName = partyB,
                        customOverrides = mapOf("rpcSettings.address" to "localhost:10011", "rpcSettings.adminAddress" to "localhost:10051", "webAddress" to "localhost:10012"),
                        rpcUsers = listOf(user)))

        nodeFutures.map { it.getOrThrow() }

        // use braid-corda-client to directly connect to the service an carry out the test here
        val vertx = Vertx.vertx()

        val client = BraidClient.createClient(
          BraidClientConfig(
            serviceURI = URI("http://localhost:8081/api/braidService/braid"),
            trustAll = true,
            verifyHost = false,
            tls = false
          ), vertx)

        val myService = client.bind(BraidService::class.java)
        val total = 99
        val countDown = CountDownLatch(99)
        myService.subscribeVaultUpdates().subscribe { it ->
            println("received count: $it")
            countDown.countDown()
            if (countDown.count == 0L) {
                log.info("*** SUCCESSFULLY received $total events")
            }
        }
        myService.generateIOUs(1, partyA, total)
            .onSuccess {
                log.info("generated 100 IOUs")
            }
            .catch { err ->
                log.error("failed to generate 100 IOUs", err)
            }
    }
}

