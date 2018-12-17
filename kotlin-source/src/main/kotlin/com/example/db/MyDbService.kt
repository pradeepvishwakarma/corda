package com.example.db

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService

/**
 * A database service subclass for handling db operations.
 *
 * @param services The node's service hub.
 */
@CordaService
class MyDbService(services: ServiceHub) : DatabaseService(services) {



    fun getTotalRecords(): Long? {

        val query = """
            SELECT COUNT(*) Total FROM iou_states
        """.trimIndent()

        val params = emptyMap<Int, Any>()
        val results = executeQuery(query, params) { it -> it.getLong("Total")}

        if (results.isEmpty()) {
            return null
        }

        val value = results.single()
        return value
    }

}