
class LiveReload{
    static start(args) {

        var testUrl = "__RELOAD_URL__"
        var reloadUrl = "__RELOAD_URL__"

        if(!testUrl || testUrl.trim().length === 0)
            testUrl = document.href

        console.log("Reload4sScalaJS: Use " + reloadUrl + " to reload URL and " + testUrl + " to test if server is up.")

        var args  = {
        	port:  __PORT__,
            debug: false,
            tryLimit: 20,
            startTimeout: 1000,
            retryTimeout: 300,
            testUrl: testUrl,
            reloadUrl: reloadUrl
        }

        if(args.debug)
            console.log("Reload4sScalaJS:", JSON.stringify(args))

        var url = "ws://localhost:"+args.port+"/ws"

        let liveReload = new WebSocket(url)

        function checkServerIsUp(successCb, errorCb){
            var xmlHttp = new XMLHttpRequest();
            xmlHttp.onreadystatechange = function() {
                if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
                    successCb()
                else
                    errorCb()
                    //callback(xmlHttp.responseText);
            }
            xmlHttp.open("GET", args.testUrl, true); // true for asynchronous
            xmlHttp.send(null);
        }

        function pageReload(max){

            if(max <= 0){
                console.error("Reload4sScalaJS: reload limit found")
                return
            }

            checkServerIsUp(() => {

                if(args.reloadUrl && args.reloadUrl.trim().length > 0)
                    location.href = args.reloadUrl
                else
                    location.reload()

            }, () => {
                (function (i){
                    setTimeout(() => {
                        pageReload(i)
                    }, args.retryTimeout)
                })(max-1)
            })
        }

        liveReload.onopen = function (event) {
            console.log("Reload4sScalaJS: Enabled.")
        }
        liveReload.onclose = function (event) {
            console.log("Reload4sScalaJS: Closed.")
        }
        liveReload.onmessage = function (event) {
            let data = JSON.parse(event.data)
            let eventKey = data.event
            if (eventKey === "ping") {
                return
            }
            if (eventKey === "reload") {
                liveReload.close()
                console.log("Reload4sScalaJS: call to reload message received.")
                setTimeout(() => {
                    pageReload(args.tryLimit)
                }, args.startTimeout)
            } else if (eventKey === "log") {
                let level = data.level
                let message = data.message
                if (level === "info") console.info(message)
                else if (level === "log") console.log(message)
                else if (level === "warn") console.warn(message)
                else if (level === "error") console.error(message)
                else console.log("Reload4sScalaJS:", message)
            } else if(eventKey === "alive") {
            	console.log("Reload4sScalaJS: Channel is alive.")
            } else {
                console.log("Reload4sScalaJS: Unknown message: " + data)
            }
        }
        
    };

}

(function(){
	LiveReload.start()
}())
