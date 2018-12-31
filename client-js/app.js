const Proxy = require('braid-client').Proxy;

const corda = new Proxy({url: 'http://localhost:8081/api/'}, onOpen, onClose, onError, {strictSSL: false})

function onOpen() {
    console.log("connected")

    // Observe the total iou's created in vault
    corda.braidService.subscribeVaultUpdates(re => console.log("Total Record: "+ re))

    // Generate 100 iou's for the party
    corda.braidService.generateIOUs(10, "O=PartyA,L=London,C=GB", 100)

    // Get total number of iou's available in vault.
    //corda.braidService.calculateTotalRecords().then(re => console.log(re))

}
function onClose() {
    console.log("closed")
}

function onError(err) {
    console.error(err)
}

/*

Branch : It's our lumedic enrollment poc branch. (task/flow-observer-node)

Server : Run the cordapp's as usual.

Client : We have added a folder called "client-js". please follow the step to run the client as
1) Open CMD and change to client-js directory to run the node(v.9 above) command.
2) Install the dependency form package.json : npm install
3) Execute the app.js file : node app.js

*/