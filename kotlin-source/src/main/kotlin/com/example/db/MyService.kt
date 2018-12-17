package com.example.db

import com.example.flow.ExampleFlow
import com.example.state.IOUState
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class MyService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    /*
    init {

        val pageSpec = PageSpecification(1, 10)
        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
        val results = serviceHub.vaultService.trackBy(contractStateType = IOUState::class.java, criteria = criteria, paging = pageSpec)
        val updates= results.updates

        val vaultSub = updates.subscribe {
            update -> processUpdates(update)
        }
    }

    fun processUpdates(update : Vault.Update<IOUState>){
        val dbService = serviceHub.cordaService(MyDbService::class.java)
        val totalRecord = dbService.getTotalRecords()
    }
    */

    fun generateIOUs(iouValue : Int, otherParty : Party, nos : Int = 100){

        for (i in 1..nos) {
            serviceHub.startFlow(ExampleFlow.Initiator(iouValue + i, otherParty))
        }
    }
}