<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Modified Example CorDapp to show subscription and DB issue

The intention of this sample is to show a possible bug when using subscription feature from Service Hub. **The sample aims to show that if we try use ServiceHub to subscribe to DB updates and then access the DB from the subscription update then there is an exception and the system hangs.The same functionality works well if using CordaRPCOps**

The basic IOU sample has been modified to show the issue. We have modified the sample to _create 30 IOUs_ at one time and given two buttons for subscription.

Clicking on **Subscribe Vault** will subscribe to updates using the Service Hub and the system might stop functioning after a few IOU creations.
Clicking on **API Subscribe Vault** will subscribe to updates using the CordaRPCOps and the system will continue working well

# Steps to Reproduce
1. Run the nodes using runnodes.bat
2. Open webserver of one of the parties say Party B on localhost:10012
3. Open up the example UI on http://localhost:10012/web/example/
4. Click on CreateIOU to create an IOU with amount less than 70. Click on create 30 IOU buttons . This will create 30 IOUs. Refresh screen and wait for 30 IOUs to get created before proceeding.
5. Click on **API Subscribe Vault**. Corresponding code can be seen at *com/example/api/ExampleApi.kt fun subscribeAPI()*
6. Again click on CreateIOU and create as many IOUs . Everything works well. _Logs can be checked for PartyB Web server under build\nodes\PartyB\logs\web/_**  to see that subscription is working by checking for the following text:-  **API total record count is**
7. Click on **Subscribe Vault**. Corresponding code can be seen at ...kotlin\com\example\flow\ExampleFlow.kt SubscriberFlow()
8. Click on CreateIOU and create another 30 IOUs. **The system will not be able to create all 30 IOUs and will stop after creating a few**. If subscription updates work then corresponding log gets generated:-  **total record count is**. 

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

