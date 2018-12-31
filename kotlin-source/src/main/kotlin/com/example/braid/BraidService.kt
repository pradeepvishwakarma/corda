package com.example.braid

import com.example.db.MyDbService
import com.example.flow.ExampleFlow
import com.example.state.IOUState
import io.bluebank.braid.corda.services.transaction
import io.vertx.core.Vertx
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import rx.Observable

class BraidService(private val serviceHub: AppServiceHub, private val vertx : Vertx) : SingletonSerializeAsToken() {


    fun generateIOUs(iouValue : Int, partyName : CordaX500Name, nos : Int = 50){

        val party = serviceHub.networkMapCache.getNodeByLegalName(partyName)?.legalIdentities?.first()
                ?: throw IllegalArgumentException("Requested party node $partyName not found on network.")

        for (i in 1..nos) {
            serviceHub.startFlow(ExampleFlow.Initiator(iouValue + i, party))
        }
    }

    fun subscribeVaultUpdates() : Observable<Long> {

        return Observable.create { subscriber ->

            val pageSpec = PageSpecification(1, 10)
            val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
            val results = serviceHub.vaultService.trackBy(contractStateType = IOUState::class.java, criteria = criteria, paging = pageSpec)
            val updates= results.updates

            val vaultSub = updates.subscribe {
                update ->
                val totalRecord = calculateTotalRecords()
                subscriber.onNext(totalRecord)
            }

            val consumer = vertx.eventBus().consumer<String>("time")
            consumer.handler {
                if(subscriber.isUnsubscribed) consumer.unregister()
            }
        }
    }

    fun calculateTotalRecords() : Long {

        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status =  Vault.StateStatus.UNCONSUMED)
        val results=  serviceHub.vaultService.queryBy<IOUState>(criteria=criteria,paging = PageSpecification(1,10))
        val totalRecord =  results.totalStatesAvailable
        return totalRecord


        /*
        val total = (serviceHub as ServiceHubInternal).database.transaction {
            val dbService = serviceHub.cordaService(MyDbService::class.java)
             dbService.getTotalRecords()
        }
        return total ?: 0*/
    }
}