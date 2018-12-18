package com.example.api

import com.example.flow.ExampleFlow.Initiator
import com.example.flow.ExampleFlow.StressTestFlow
import com.example.flow.ExampleFlow.SubscriberFlow
import com.example.schema.IOUSchemaV1
import com.example.state.IOUState
import net.corda.core.contracts.TransactionState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.MAX_PAGE_SIZE
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("example")
class ExampleApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)


    @GET
    @Path("subscribe")
    @Produces(MediaType.APPLICATION_JSON)
    fun subscribeMe() : Map<String, Boolean>  {

        val status = rpcOps.startTrackedFlow(::SubscriberFlow).returnValue.getOrThrow()
        return mapOf("Status" to status)
    }

    @GET
    @Path("apisubscribe")
    @Produces(MediaType.TEXT_PLAIN)
    fun subscribeAPI() :String{

        val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status =  Vault.StateStatus.UNCONSUMED)
        val results = rpcOps.vaultTrackByWithPagingSpec(contractStateType = IOUState::class.java, criteria = criteria ,paging = PageSpecification(1,10))
        val updates= results.updates

        logger.info("called API for subscription")
        val vaultSub = updates.subscribe {
            update ->
            val criteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria(status =  Vault.StateStatus.UNCONSUMED)
            val results=  rpcOps.vaultQueryBy<IOUState>(criteria=criteria,paging = PageSpecification(1,10))
            val recordCount = results.totalStatesAvailable
            logger.info("API total record count is $recordCount")
        }
        return "subscribed"
    }



    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Displays all IOU states that exist in the node's vault.
     */
    @GET
    @Path("ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun getIOUs() = rpcOps.vaultQueryBy<IOUState>(criteria = QueryCriteria.LinearStateQueryCriteria(), paging = PageSpecification(1, MAX_PAGE_SIZE)).states

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-iou")
    fun createIOU(@QueryParam("iouValue") iouValue: Int, @QueryParam("partyName") partyName: CordaX500Name?): Response {
        if (iouValue <= 0 ) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'iouValue' must be non-negative.\n").build()
        }
        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build()
        }
        val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName) ?:
                return Response.status(BAD_REQUEST).entity("Party named $partyName cannot be found.\n").build()

        return try {
            rpcOps.startTrackedFlow(::StressTestFlow, iouValue, otherParty).returnValue.getOrThrow()

            Response.status(CREATED).entity("Transaction committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }
	
	/**
     * Displays all IOU states that are created by Party.
     */
    @GET
    @Path("my-ious")
    @Produces(MediaType.APPLICATION_JSON)
    fun myious(): Response {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val results = builder {
                var partyType = IOUSchemaV1.PersistentIOU::lenderName.equal(rpcOps.nodeInfo().legalIdentities.first().name.toString())
                val customCriteria = QueryCriteria.VaultCustomQueryCriteria(partyType)
                val criteria = generalCriteria.and(customCriteria)
                val results = rpcOps.vaultQueryBy<IOUState>(criteria = criteria, paging = PageSpecification(1, MAX_PAGE_SIZE)).states
                return Response.ok(results).build()
        }
    }
}