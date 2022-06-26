
let port = 8081;
let stompClient;

function printResponse(response) {
    let responseObj = JSON.parse(response.body);

    let currentElement = document.getElementById("currentElementNumber");
    let currentFile = document.getElementById("currentFile");
    let totalElements = document.getElementById("totalElements");
    let percentage = document.getElementById("progress");

    currentElement.textContent = responseObj.currentElementNumber;
    currentFile.textContent = fitString(responseObj.currentFile);
    totalElements.textContent = responseObj.totalElements;
    let percentageValue = getPercentage(responseObj.currentElementNumber, responseObj.totalElements);
    percentage.innerHTML = percentageValue;
    percentage.setAttribute("style","width: " + percentageValue + "%");
    percentage.setAttribute("aria-valuenow", percentageValue);

    if (responseObj.enabled === false) {
        let spinnerUpdated = document.getElementById("spinner").className + " visually-hidden";
        spinner.setAttribute("class", spinnerUpdated);
        currentFile.setAttribute("class", "text-success");
    }
}

function fitString(string) {
    if (string.length > 60) {
        return "(...) " + string.slice(string.length - 55, string.length);
    } else return string;
}

    // connect and subscribe to message broker at given address
function connect() {
    const socket = new SockJS("http://localhost:" + port + "/notifications");
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function () {
        stompClient.subscribe("/user/notification/item", function (response) {
//            console.log(' Got ' + response);
//            mess = response;
            printResponse(response);
        });
        console.info("connected!")
    });
}

//disconnect from websocket
function disconnect() {
    if (stompClient) {
        stompClient.disconnect();
        stompClient = null;
        console.info("disconnected");
    }
}

// calculate percentage value
function getPercentage(current, total) {
       if (current > 0 && total > 0) {
            return Math.round((current / total) * 100);
       } else {
            return 100;
       }
}

// register client
function start() {
    if (stompClient) {
        stompClient.send("/swns/start", {});
    }
}

function runAuto(message) {
    if (stompClient) {
        stompClient.send("/swns/runauto", {message});
    }
}

// unregister client
function stop() {
    if (stompClient) {
        stompClient.send("/swns/stop", {});
    }
}

function connectAndReceive() {
    connect();
    setTimeout(() => {start();}, 1000);
}

function stopAndDisconnect() {
    stop();
    disconnect();
}

window.onload = connectAndReceive();

// on window closing remove client from listeners and disconnect from websocket
window.onbeforeunload = function() {
    stopAndDisconnect();
};