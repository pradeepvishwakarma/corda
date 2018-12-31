package com.example.braid

import io.bluebank.braid.corda.BraidConfig
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import java.text.SimpleDateFormat
import java.util.*


@CordaService
class BraidServer(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    companion object {
        private val log = loggerFor<BraidServer>()
    }

    init {
        BraidConfig.fromResource(configFileName)?.bootstrap()
    }

    private fun BraidConfig.bootstrap() {

        val vertx = Vertx.vertx()
        this.withVertx(vertx)
                .withService("braidService", BraidService(serviceHub, vertx))
                .withHttpServerOptions(HttpServerOptions().setSsl(false))
                .bootstrapBraid(serviceHub, Handler { result ->
                    if (result.failed()) {
                        log.error("failed to start up braid service", result.cause())
                    } else {
                        log.info("started braid service")
                    }
                })

        vertx.setPeriodic(500) {
            vertx.eventBus().publish("time", SimpleDateFormat("HH:mm:ss").format(Date()))
        }
    }

    /**
     * config file name based on the node legal identity
     */
    private val configFileName: String
        get() {
            val name = serviceHub.myInfo.legalIdentities.first().name.organisation
            return "braid-$name.json"
        }
}

