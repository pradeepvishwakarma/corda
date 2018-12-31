<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Modified Example CorDapp(Kotlin) to show subscription and DB issue

The intention of this sample is to show a possible bug when using subscription feature from Service Hub. **The sample aims to show that if we try to use ServiceHub to subscribe to vault updates and then access the vault/db from the vault updates then there is an exception and the system hangs.The same functionality works well if using CordaRPCOps**

The basic IOU sample has been modified to show the issue and created a sample nodejs client to invoke api's to generate and observe the vault updates.

# Steps to Reproduce
1. Run the nodes using runnodes.bat
2. Switch to nodejs client app (Path : ./client-js/). In that we have created a sample nodejs app, which connect to braid server and perform basic operations.
3. To run the node app. Go to terminal and type **node app.js** + hit enter. Once it is connected, it will perform two action
   a) Subscribe the vault updates by executing **subscribeVaultUpdates**	
   b) Generate 100 iou's for the party by executing **generateIOUs**

Inspection of logs will show **subscription stopped after a few IOUs (Approx 9)**
