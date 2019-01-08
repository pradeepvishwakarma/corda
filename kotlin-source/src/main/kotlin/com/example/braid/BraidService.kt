package com.example.braid

import io.vertx.core.Future
import net.corda.core.identity.CordaX500Name
import rx.Observable

/**
 * Service interface used to make testing using the braid-java-client achievable in the NodeDriver.kt
 * We use this to avoid having to constantly start up the nodejs client
 */
interface BraidService {
    fun generateIOUs(iouValue : Int, partyName : CordaX500Name, nos : Int = 50) : Future<Unit>
    fun subscribeVaultUpdates() : Observable<Long>
}