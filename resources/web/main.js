// Global variable to hold the websocket.
let socket = null;
let disconnectMessage = false;
let previewMessage = false;

let retryAttempts = 0;

/**
 * This function is in charge of connecting the client.
 */
function connect() {
    // First we create the socket.
    // The socket will be connected automatically asap. Not now but after returning to the event loop,
    // so we can register handlers safely before the connection is performed.
    console.log("Begin connect");
    socket = new WebSocket("ws://" + window.location.host + "/chat/ws");

    // We set a handler that will be executed if the socket has any kind of unexpected error.
    // Since this is a just sample, we only report it at the console instead of making more complex things.
    socket.onerror = function () {
        console.log("socket error");
    };

    // We set a handler upon connection.
    // What this does is to put a text in the messages container notifying about this event.
    socket.onopen = function () {
        console.log("Connected");
        //var connected = "{\"user\":{\"name\":\"Server\",\"image\":\"https://www.w3schools.com/w3images/bandmember.jpg\"},\"message\":\"Connected\",\"type\":\"SERVER\"}"
        //write("Connected");
        //write(connected);
        disconnectMessage = false;
        retryAttempts = 0;
    };

    // If the connection was closed gracefully (either normally or with a reason from the server),
    // we have this handler to notify to the user via the messages container.
    // Also we will retry a connection after 5 seconds.
    socket.onclose = function (evt) {
        // Try to gather an explanation about why this was closed.
        let explanation = "";
        if (evt.reason && evt.reason.length > 0) {
            explanation = "reason: " + evt.reason;
        } else {
            explanation = "without a reason specified";
        }

        // Notify the user using the messages container.
        if (!disconnectMessage) {
            let disconnected = "{\"time\":\"Now\",\"user\":{\"name\":\"Server\",\"image\":\"https://www.w3schools.com/w3images/bandmember.jpg\"},\"message\":\"" + "Disconnected with close code " + evt.code + " and " + explanation + "\",\"type\":\"SERVER\"}";
            write(disconnected);
            disconnectMessage = true;
        }
        //write("Disconnected");
        // Try to reconnect after 5 seconds.
        if(retryAttempts<=5) {
            retryAttempts++;
            setTimeout(connect, 5000);
        } else {
            alert("Please refresh the window to try to reconnect")
        }
    };

    // If we receive a message from the server, we want to handle it.
    socket.onmessage = function (event) {
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

function setUpUserList(userDiv, chatter) {
    const userContainer = document.createElement("div");
    userContainer.className = "user-container";

    const pDiv = document.createElement("div");
    pDiv.className = "user_name_layer";
    const pTag = document.createElement("p");
    pTag.innerText = chatter.name;
    pTag.className = "userShow";
    pDiv.appendChild(pTag);

    const imageDiv = document.createElement("div");
    imageDiv.className = "user_image_layer";
    const userImg = document.createElement("img");
    userImg.src = chatter.image;
    userImg.className = "user_image_img";
    imageDiv.appendChild(userImg);

    userContainer.appendChild(imageDiv);
    userContainer.appendChild(pDiv);

    userContainer.addEventListener("click", function () {
        const input = document.getElementById("commandInput");
        input.value = "/pm " + chatter.name + " ";
        input.focus();
    });

    userDiv.appendChild(userContainer);
}

function messageSetUp(div, obj) {
    const imgDiv = document.createElement("div");
    imgDiv.className = "profile_image_layer";
    const img = document.createElement("img");
    img.src = obj.user.image;
    img.className = "container_img";
    imgDiv.appendChild(img);
    const timeTag = document.createElement("p");
    timeTag.className = "time-left";
    timeTag.innerHTML = obj.time;
    imgDiv.appendChild(document.createElement("br"));
    imgDiv.appendChild(timeTag);

    const textDiv = document.createElement("div");
    textDiv.className = "text_layer inner";
    const line = document.createElement("p");
    line.className = "message";
    line.innerHTML = obj.message;
    textDiv.appendChild(line);
    div.appendChild(imgDiv);
    div.appendChild(textDiv);

    div.className = "main-grid-container";
    return line;
}

function episodeSetUp(div, obj) {
    const episode = JSON.parse(obj.data)[0];
    div.className = "container";

    const container = document.createElement("div");
    container.className = "grid-container";

    const imageLayer = document.createElement("div");
    imageLayer.className = "image_layer";
    const imgTag = document.createElement("img");
    imgTag.src = episode.image;
    imgTag.className = "episode_image";
    imageLayer.appendChild(imgTag);

    const titleLayer = document.createElement("div");
    titleLayer.className = "title_layer";
    const titleTag = document.createElement("p");
    titleTag.innerHTML = episode.name;
    titleLayer.appendChild(titleTag);

    const descriptionLayer = document.createElement("div");
    descriptionLayer.className = "description_layer";
    const descriptionTag = document.createElement("p");
    descriptionTag.innerHTML = episode.description;
    descriptionLayer.appendChild(descriptionTag);

    container.appendChild(descriptionLayer);
    container.appendChild(titleLayer);
    container.appendChild(imageLayer);

    container.addEventListener("click", function () {
        const u = episode.url;
        openEpisode(u);
    });
    div.appendChild(container);
}

const INFO = "INFO";
const SERVER = "SERVER";
const EPISODE = "EPISODE";
const MESSAGE = "MESSAGE";
const TYPING_INDICATOR = "TYPING_INDICATOR";
const DOWNLOADING = "DOWNLOADING";
const PREVIEW = "PREVIEW";

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
    const obj = JSON.parse(message);
    if(obj.type===PREVIEW) {
        document.getElementById("preview_message").innerHTML = obj.message;
    } else if(obj.type===DOWNLOADING) {
        console.log(obj.data);
    } else if(obj.type === TYPING_INDICATOR) {
        const typingTag = document.getElementById("typing_indicate");
        typingTag.innerText = obj.message;
    } else if (obj.type === INFO) {
        const userDiv = document.getElementById("current_users");
        while (userDiv.hasChildNodes()) {
            userDiv.firstChild.remove()
        }
        for (let user in obj.data) {
            let chatter = obj.data[user];
            console.log(chatter);
            setUpUserList(userDiv, chatter)
        }
    } else {
        const div = document.createElement("div");

        const line = messageSetUp(div, obj);

        if (obj.type === MESSAGE) {
            if (obj.data === "pm") {
                div.className += " pm-dark";
            } else {
                div.className += " message-normal";
            }
        } else if (obj.type === SERVER) {
            div.className += " message-normal";
        } else if (obj.type === EPISODE) {
            episodeSetUp(div, obj)
        } else {

        }

        // Then we get the 'messages' container that should be available in the HTML itself already.
        const messagesDiv = document.getElementById("messages");
        // We adds the text
        messagesDiv.appendChild(div);
        // We scroll the container to where this text is so the use can see it on long conversations if he/she has scrolled up.
        messagesDiv.scrollTop = line.offsetTop;
    }
}

/**
 * Function in charge of sending the 'commandInput' text to the server via the socket.
 */
function onSend() {
    const input = document.getElementById("commandInput");
    // Validates that the input exists
    if (input) {
        const text = input.value.trim();
        // Validates that there is a text and that the socket exists
        if (text && socket) {
            // Sends the text
            socket.send(text);
            // Clears the input so the user can type a new command or text to say
            input.value = "";
        }
    }
}

function onSendTyping() {
    const input = document.getElementById("commandInput");
    // Validates that the input exists
    if (input) {
        const text = input.value.trim();
        // Validates that there is a text and that the socket exists
        let typing = {
            isTyping: text.length!==0 && socket.readyState===1 && !text.includes("/pm")
        };
        actionSend("Typing", typing);
    }
}

function onProfileChange() {

    const username = document.getElementById("username_change");
    // Validates that the input exists
    if (username) {
        let text = username.value;
        // Validates that there is a text and that the socket exists
        if (text && socket) {
            // Sends the text
            socket.send("/user " + text);
            // Clears the input so the user can type a new command or text to say
            //username.value = "";
        }
    }

    const image = document.getElementById("image_change");
    // Validates that the input exists
    if (image) {
        let text = image.value;
        // Validates that there is a text and that the socket exists
        if (text && socket) {
            // Sends the text
            socket.send("/image " + text);
            // Clears the input so the user can type a new command or text to say
            //image.value = "";
        }
    }
}

function onDownloadMessages() {
    let downloading = {
        download: true
    };
    actionSend("Download", downloading);
}

function onPreviewMessage() {
    if(document.getElementById("commandInput").value.trim()) {
        if (!previewMessage) {
            let preview = {
                text: document.getElementById("commandInput").value.trim()
            };
            actionSend("Preview", preview);
            $('.collapse').collapse('show');
        } else {
            document.getElementById("preview_message").innerHTML = "";
            $('.collapse').collapse('hide');
        }
        previewMessage = !previewMessage;
    } else {
        previewMessage = false;
        document.getElementById("preview_message").innerHTML = "";
        $('.collapse').collapse('hide');
    }
}

function actionSend(type, data) {
    if (socket) {
        let d = {
            type: type,
            json: JSON.stringify(data)
        };
        socket.send(JSON.stringify(d));
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
    // To change information
    document.getElementById("saveChanges").onclick = onProfileChange;
    //to download all the messages
    document.getElementById("download_messages").onclick = onDownloadMessages;
    //to preview your message
    document.getElementById("preview_text").onclick = onPreviewMessage;
    // If we pressed the 'enter' key being inside the 'commandInput', send the message to improve accessibility and making it nicer.
    /*document.getElementById("commandInput").onkeydown = function (e) {
        if (e.keyCode === 13) {
            onSend();
        } else {
            //onSendTyping();
        }
    };*/
    document.getElementById("commandInput").onkeyup= function (e) {
        if (e.keyCode === 13) {
            onSend();
        }
        onSendTyping();
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
