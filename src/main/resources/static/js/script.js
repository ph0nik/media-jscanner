
let port = 44222;
let stompClient;

const checkBox = document.getElementsByClassName("query-select");

function setEvent() {
    Array.from(checkBox).forEach((item, index) => {
        item.addEventListener("change", event => {
            document.getElementById("row-selected" + index).className = (item.checked) ?
            document.getElementById("row-selected" + index).className.replace("border-white", "border-info") :
            document.getElementById("row-selected" + index).className.replace("border-info", "border-white");

        })
    })
}

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

    if (responseObj.link != "") {
        insertRowData(responseObj.currentFile, responseObj.link, responseObj.currentElementNumber);
    }

    if (responseObj.enabled === false) {
        let spinnerUpdated = document.getElementById("spinner").className + " visually-hidden";
        spinner.setAttribute("class", spinnerUpdated);
        currentFile.setAttribute("class", "text-success");
        showConfirmAndAbortButtons();
//        showFinishButton();
    }
}

const rowTemplate = `
            <tr>
                <td colspan="1" class="text-center col-1">
                    <span>{{index}}</span>
                </td>
                <td colspan="3">
                    <span  class="fs-6 fw-bold text-muted">{{sourcePath}}</span>
                    <div class="flex">
                        <i class="fa-solid fa-link"></i>
                        <span class="fs-6 fw-normal">{{linkPath}}</span>
                    </div>
                </td>
            </tr>
`;

function insertRowData(sourcePath, linkPath, index) {
    var table = document.getElementById("incoming_links");
    let newRowHtml = rowTemplate
    .replace('{{index}}', index)
    .replace('{{sourcePath}}', sourcePath)
    .replace('{{linkPath}}', linkPath);

    table.querySelector('tbody').insertAdjacentHTML('beforeend', newRowHtml);
}

function showConfirmAndAbortButtons() {
    let confirmUpdated = document.getElementById("confirm_form").className = "visible";
    confirm_form.setAttribute("class", confirmUpdated);
    let abortUpdated = document.getElementById("abort_form").className = "visible";
    abort_form.setAttribute("class", abortUpdated);
}

function showFinishButton() {
    let finalButtonUpdated = document.getElementById("follow_button").className = "visible";
    follow_button.setAttribute("class", finalButtonUpdated);
}

function fitString(string) {
    if (string.length > 60) {
        return "(...) " + string.slice(string.length - 55, string.length);
    } else return string;
}

    // connect and subscribe to message broker at given address
function connect() {
    const socket = new SockJS("/notifications");
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

window.onload = function() {
    connectAndReceive();
    setEvent(); // init event listeners for list elements
}

// on window closing remove client from listeners and disconnect from websocket
window.onbeforeunload = function() {
    stopAndDisconnect();
};