package com.example.braid

import com.example.flow.ExampleFlow
import com.example.state.IOUState
import io.bluebank.braid.core.async.all
import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.mapUnit
import io.bluebank.braid.core.async.onSuccess
import io.vertx.core.Future
import io.vertx.core.Vertx
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import rx.Observable
import rx.schedulers.Schedulers


class BraidServiceImpl(private val serviceHub: AppServiceHub, private val vertx: Vertx) : SingletonSerializeAsToken(), BraidService {
    companion object {
        val logger: Logger = loggerFor<BraidServiceImpl>()
    }

    override fun generateIOUs(iouValue: Int, partyName: CordaX500Name, nos: Int): Future<Unit> {
        logger.info("generateIOUs : Started")

        val party = serviceHub.networkMapCache.getNodeByLegalName(partyName)?.legalIdentities?.first()
            ?: throw IllegalArgumentException("Requested party node $partyName not found on network.")

        // we do this properly ... execute all flows in parallel, and wait for all to complete
        return (1..nos).map { id ->
            logger.info("IOU: $id")
            serviceHub.startFlow(ExampleFlow.Initiator(iouValue + id, party)).returnValue.toVertxFuture()
        }.all()
            .onSuccess {
                logger.info("generateIOUs : Finished")
            }
            .catch { err ->
                logger.error("failed to create IOUs", err)
            }
            .mapUnit() // map to a Future<Unit>
    }

    override fun subscribeVaultUpdates(): Observable<Long> {
        val pageSpec = PageSpecification(1, 1)
        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
        val results = serviceHub.vaultService.trackBy(contractStateType = IOUState::class.java, criteria = criteria, paging = pageSpec)
        // we do this as a single subscribable flow - no need to subscribe within a subscription
        return results.updates
            .observeOn(Schedulers.computation()) // this was the key technique to avoid jamming up corda's hibernate transactions
            .map { // for each event, recompute the total
                calculateTotalRecords()
            }
            .doOnNext {
                // and log it
                logger.info("subscribeVaultUpdates : TotalRecord : $it")
            }
    }

    private fun calculateTotalRecords(): Long {
        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val results = serviceHub.vaultService.queryBy<IOUState>(criteria = criteria, paging = PageSpecification(1, 1))
        return results.totalStatesAvailable
    }
}