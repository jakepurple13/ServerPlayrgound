// Global variable to hold the websocket.
var socket = null;

/**
 * This function is in charge of connecting the client.
 */
function connect() {
    // First we create the socket.
    // The socket will be connected automatically asap. Not now but after returning to the event loop,
    // so we can register handlers safely before the connection is performed.
    console.log("Begin connect");
    socket = new WebSocket("wss://" + window.location.host + "/chat/ws");

    // We set a handler that will be executed if the socket has any kind of unexpected error.
    // Since this is a just sample, we only report it at the console instead of making more complex things.
    socket.onerror = function() {
        console.log("socket error");
    };

    // We set a handler upon connection.
    // What this does is to put a text in the messages container notifying about this event.
    socket.onopen = function() {
        console.log("Connected");
        //var connected = "{\"user\":{\"name\":\"Server\",\"image\":\"https://www.w3schools.com/w3images/bandmember.jpg\"},\"message\":\"Connected\",\"type\":\"SERVER\"}"
        //write("Connected");
        //write(connected);
    };

    // If the connection was closed gracefully (either normally or with a reason from the server),
    // we have this handler to notify to the user via the messages container.
    // Also we will retry a connection after 5 seconds.
    socket.onclose = function(evt) {
        // Try to gather an explanation about why this was closed.
        var explanation = "";
        if (evt.reason && evt.reason.length > 0) {
            explanation = "reason: " + evt.reason;
        } else {
            explanation = "without a reason specified";
        }

        // Notify the user using the messages container.
        write("Disconnected with close code " + evt.code + " and " + explanation);
        // Try to reconnect after 5 seconds.
        setTimeout(connect, 5000);
    };

    // If we receive a message from the server, we want to handle it.
    socket.onmessage = function(event) {
        console.log(event.data);
        received(event.data.toString());
    };
}

/**
 * Handle messages received from the sever.
 *
 * @param message The textual message
 */
function received(message) {
    // Out only logic upon message receiving is to output in the messages container to notify the user.
    write(message);
}

/**
 * Writes a message in the HTML 'messages' container that the user can see.
 *
 * @param message The message to write in the container
 */
function write(message) {
    // We first create an HTML paragraph and sets its class and contents.
    // Since we are using the textContent property.
    // No HTML is processed and every html-related character is escaped property. So this should be safe.
    /*var line = document.createElement("p");
    line.className = "message";
    line.innerHTML = message;*/
    var obj = JSON.parse(message);
    var div = document.createElement("div");
    if(obj.type==="MESSAGE") {
        div.className = "container darker";
    } else if(obj.type==="SERVER") {
        div.className = "container";
    } else {
        div.className = "container";
    }
    var img = document.createElement("img");
    img.src = obj.user.image;
    img.style = "width:100%;";
    var line = document.createElement("p");
    line.className = "message";
    line.innerHTML = obj.message;
    div.appendChild(img);
    div.appendChild(line);

    // Then we get the 'messages' container that should be available in the HTML itself already.
    var messagesDiv = document.getElementById("messages");
    // We adds the text
    messagesDiv.appendChild(div);
    // We scroll the container to where this text is so the use can see it on long conversations if he/she has scrolled up.
    messagesDiv.scrollTop = line.offsetTop;
}

/**
 * Function in charge of sending the 'commandInput' text to the server via the socket.
 */
function onSend() {
    var input = document.getElementById("commandInput");
    // Validates that the input exists
    if (input) {
        var text = input.value;
        // Validates that there is a text and that the socket exists
        if (text && socket) {
            // Sends the text
            socket.send(text);
            // Clears the input so the user can type a new command or text to say
            input.value = "";
        }
    }
}

function onProfileChange() {

    console.log("Hello");

    var username = document.getElementById("username_change");
        // Validates that the input exists
        if (username) {
            var text = username.value;
            // Validates that there is a text and that the socket exists
            if (text && socket) {
                // Sends the text
                socket.send("/user " + text);
                // Clears the input so the user can type a new command or text to say
                //username.value = "";
            }
        }

    var image = document.getElementById("image_change");
        // Validates that the input exists
        if (image) {
            var text = image.value;
            // Validates that there is a text and that the socket exists
            if (text && socket) {
                // Sends the text
                socket.send("/image " + text);
                // Clears the input so the user can type a new command or text to say
                //image.value = "";
            }
        }

}

/**
 * The initial code to be executed once the page has been loaded and is ready.
 */
function start() {
    // First, we should connect to the server.
    connect();

    // If we click the sendButton, let's send the message.
    document.getElementById("sendButton").onclick = onSend;

    document.getElementById("saveChanges").onclick = onProfileChange;
    // If we pressed the 'enter' key being inside the 'commandInput', send the message to improve accessibility and making it nicer.
    document.getElementById("commandInput").onkeydown = function(e) {
        if (e.keyCode === 13) {
            onSend();
        }
    };
}

/**
 * The entry point of the client.
 */
function initLoop() {
    // Is the sendButton available already? If so, start. If not, let's wait a bit and rerun this.
    if (document.getElementById("sendButton")) {
        start();
    } else {
        setTimeout(initLoop, 300);
    }
}

// This is the entry point of the client.
initLoop();