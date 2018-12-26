package com.example.braid

import com.example.api.ExampleApi
import com.example.state.IOUState
import io.vertx.core.Vertx
import net.corda.client.jackson.JacksonSupport
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria

class VaultService(private val serviceHub: AppServiceHub, private val vertx: Vertx) {
    companion object {

        var mapper = JacksonSupport.createNonRpcMapper()
    }

    fun subscribeAPI() :String{

        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status =  Vault.StateStatus.UNCONSUMED)
        val results = rpcOps.vaultTrackByWithPagingSpec(contractStateType = IOUState::class.java, criteria = criteria ,paging = PageSpecification(1,10))
        val updates= results.updates

        ExampleApi.logger.info("called API for subscription")
        val vaultSub = updates.subscribe {
            update ->
            val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status =  Vault.StateStatus.UNCONSUMED)
            val results=  rpcOps.vaultQueryBy<IOUState>(criteria=criteria,paging = PageSpecification(1,10))
            val recordCount = results.totalStatesAvailable
            ExampleApi.logger.info("API total record count is $recordCount")
        }
        return "subscribed"
    }

}
