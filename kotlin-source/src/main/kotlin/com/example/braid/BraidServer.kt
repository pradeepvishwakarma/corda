package com.example.braid


import io.bluebank.braid.corda.BraidConfig
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor


@CordaService
class BraidServer(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    companion object {
        private val log = loggerFor<BraidServer>()
    }

    init {
        val vertx = Vertx.vertx()

        BraidConfig()
                .withVertx(vertx)
                // Include a flow on the Braid server.
                // .withFlow(WhoAmIFlow::class.java)

                // Include a service on the Braid server.
               .withService("vaultService", VaultService(serviceHub, vertx))
                // The port the Braid server listens on.
                .withPort(8091)
                // Using http instead of https.
                .withHttpServerOptions(HttpServerOptions().setSsl(false))
                // Start the Braid server.
                .bootstrapBraid(serviceHub, Handler { result ->
                    if (result.failed()) {
                        log.error("failed to start up braid service", result.cause())
                    } else {
                        log.info("started braid service")
                    }
                })
    }
}