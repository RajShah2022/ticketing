"use strict";

var usernamePage = document.querySelector("#username-page");
var chatPage = document.querySelector("#chat-page");
var usernameForm = document.querySelector("#usernameForm");
var seatsArea = document.querySelector("#seatsArea");
var connectingElement = document.querySelector(".connecting");

var stompClient = null;
var username = null;
var allSeats = null;

var colors = [
    "#2196F3",
    "#32c787",
    "#00BCD4",
    "#ff5652",
    "#ffc107",
    "#ff85af",
    "#FF9800",
    "#39bbb0",
];

function connect(event) {
    username = document.querySelector("#name").value.trim();

    if (username) {
        usernamePage.classList.add("hidden");
        chatPage.classList.remove("hidden");

        var socket = new SockJS("/ws");
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    // Subscribe to the Public Topic
    stompClient.subscribe("/topic/public", onSeatsReceived);

    // Tell your username to the server
    stompClient.send(
        "/app/seat.addUser",
        {},
        JSON.stringify({ sender: username, type: "JOIN" })
    );
    connectingElement.classList.add("hidden");
}

function onError(error) {
    connectingElement.textContent =
        "Could not connect to WebSocket server. Please refresh this page to try again!";
    connectingElement.style.color = "red";
}

function sendUpdates(item) {
    console.log("item -> ", item);
    if (stompClient) {
        stompClient.send("/app/seat.updateStatus", {}, JSON.stringify(item));
    }
}

function onSeatsReceived(payload) {
    var seats = JSON.parse(payload.body);
    allSeats = seats;
    seats =[...seats].sort((first, second) => first.id > second.id);
    if (seats.type !== "JOIN" && seats.type !== "LEAVE") {
        seatsArea.innerHTML = "";
        seats.forEach((item, index) => {
            const li = document.createElement("li");
            const label = document.createElement("label");
            label.textContent = item.seatNumber;
            li.appendChild(label);
            if (item.status == "AVAILABLE") {
                const button = document.createElement("button");
                button.textContent = "Reserve this seat";
                button.addEventListener("click", () => {
                    sendUpdates({
                        sender: username,
                        seatId: item.id,
                        status: "RESERVED",
                    });
                });
                li.appendChild(button);
            }
            if (item.status == "RESERVED" && username === item.lastModifiedBy) {
                const bookButton = document.createElement("button");
                bookButton.textContent = "Book this seat";
                bookButton.addEventListener("click", () => {
                    sendUpdates({
                        sender: username,
                        seatId: item.id,
                        status: "BOOKED",
                    });
                });
                const cancelButton = document.createElement("button");
                cancelButton.textContent = "Cancel this seat";
                cancelButton.addEventListener("click", () => {
                    sendUpdates({
                        sender: username,
                        seatId: item.id,
                        status: "AVAILABLE",
                    });
                });
                li.appendChild(bookButton);
                li.appendChild(cancelButton);
            }
            if (item.status == "BOOKED" && username === item.lastModifiedBy) {
                var textElement = document.createElement('p');
                var messageText = document.createTextNode("(Booked by you");
                textElement.appendChild(messageText);
                li.appendChild(textElement);
            } else if (item.status == "BOOKED" && username !== item.lastModifiedBy){
                var textElement = document.createElement('p');
                var messageText = document.createTextNode("(Booked by someone else");
                textElement.appendChild(messageText);
                li.appendChild(textElement);
            }
            seatsArea.appendChild(li);
        });
    }
    seatsArea.scrollTop = seatsArea.scrollHeight;
}

usernameForm.addEventListener("submit", connect, true);
