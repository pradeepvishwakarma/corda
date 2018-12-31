<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Modified Example CorDapp(Kotlin) to show subscription and DB issue

The intention of this sample is to show a possible bug when using subscription feature from Service Hub. **The sample aims to show that if we try to use ServiceHub to subscribe to vault updates and then access the vault/db from the vault updates then there is an exception and the system hangs.The same functionality works well if using CordaRPCOps**

The basic IOU sample has been modified to show the issue and created a sample nodejs client to invoke api's to generate and observe the vault updates.

# Steps to Reproduce
1. Run the nodes using runnodes.bat
2. Switch to nodejs client app directory (Path : ./client-js/). In that we have created a sample nodejs app, which connects to braid server and performs basic operations.
3. To run the node app. Go to terminal and type **node app.js** then hit enter. Once the app is connected to the braid server, it will perform two actions
   
   a) Subscribe the vault for updates by executing **subscribeVaultUpdates**
   
   b) Generate 100 IOU's for the party by executing **generateIOUs**


Inspection of logs at _build\nodes\PartyB\logs_ will show **subscription stopped after a few IOUs (Approx 9) and then there was the following exception**

## Exception
```
ava.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30001ms.
	at com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:548) ~[HikariCP-2.5.1.jar:?]
	at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:186) ~[HikariCP-2.5.1.jar:?]
	at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:145) ~[HikariCP-2.5.1.jar:?]
	at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:83) ~[HikariCP-2.5.1.jar:?]
	at net.corda.nodeapi.internal.persistence.DatabaseTransaction$connection$2.invoke(DatabaseTransaction.kt:24) ~[corda-node-api-3.3-corda.jar:?]
	at net.corda.nodeapi.internal.persistence.DatabaseTransaction$connection$2.invoke(DatabaseTransaction.kt:16) ~[corda-node-api-3.3-corda.jar:?]
	at kotlin.UnsafeLazyImpl.getValue(Lazy.kt:153) ~[kotlin-stdlib-1.1.60.jar:1.1.60-release-55 (1.1.60)]
	at net.corda.nodeapi.internal.persistence.DatabaseTransaction.getConnection(DatabaseTransaction.kt) ~[corda-node-api-3.3-corda.jar:?]
```
