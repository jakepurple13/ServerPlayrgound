// Global variable to hold the websocket.
let socket = null;
let disconnectMessage = false;

let retryAttempts = 0;

let tribute;
let tributeEntered = false;

Prism.plugins.NormalizeWhitespace.setDefaults({
    'remove-trailing': true,
    'remove-indent': true,
    'left-trim': true,
    'right-trim': true,
    /*'break-lines': 80,
    'indent': 2,
    'remove-initial-line-feed': false,
    'tabs-to-spaces': 4,
    'spaces-to-tabs': 4*/
});

const myPreset = BbobPresetHTML5.extend(tags => ({
    ...tags,
    code: (node, core) => ({
        ...tags.code(node, core),
        /*content: '<code class="language-' + node.attrs.lang + '">' + node.content.join('') + "</code>"*/
        /*content: '<code class="lang-' + node.attrs.lang + '">' + node.content.join('') + "</code>"*/
        /*content: '<code class="line-numbers lang-java">' + node.content.join('') + '</code>'*/
        content: '<code class="line-numbers lang-' + node.attrs.lang + '">' + node.content.join('') + '</code>'
        /*content: '<code class="' + node.attrs.lang + '">' + node.content.join('') + "</code>"*/
        /*content: Prism.highlight(node.content.join(''), Prism.languages.javascript)*/
    })
}));
const core = BbobCore(myPreset());

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

        /*let jsonList = $.getJSON({
            url: '/api/web/all.json',
            success: function(json) {
                return json;
            }
        });*/

        setUpAutocomplete();

    };

    // If the connection was closed gracefully (either normally or with a reason from the server),
    // we have this handler to notify to the user via the messages container.
    // Also we will retry a connection after 5 seconds.
    socket.onclose = function (evt) {
        tribute.detach(document.getElementById('commandInput'));
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
        if (retryAttempts <= 5) {
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

function remoteSearch(text, cb) {
    let URL = '/api/web/all.json';
    let xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                let data = JSON.parse(xhr.responseText);
                cb(data);
            } else if (xhr.status === 403) {
                cb([]);
            }
        }
    };
    xhr.open("GET", URL + '?q=' + text, true);
    xhr.send();
}

function setUpAutocomplete() {
    tribute = new Tribute({
        // symbol that starts the lookup
        trigger: '@',
        // class added in the flyout menu for active item
        selectClass: 'highlight',
        // function called on select that returns the content to insert
        selectTemplate: function (item) {
            return item.original.name;
        },
        // template for displaying item in menu
        menuItemTemplate: function (item) {
            return item.original.name;
        },
        // specify an alternative parent container for the menu
        menuContainer: document.body,
        // column to search against in the object (accepts function or string)
        lookup: 'name',
        // column that contains the content to insert by default
        fillAttr: 'name',
        // REQUIRED: array of objects to match
        values: function (text, cb) {
            remoteSearch(text, users => cb(users));
        },
        // specify whether the menu should be positioned.  Set to false and use in conjuction with menuContainer to create an inline menu
        // (defaults to true)
        positionMenu: false,
        // turn tribute into an autocomplete
        //autocompleteMode: true,
    });
    tribute.attach(document.getElementById('commandInput'));
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
    line.innerHTML = parseBBCode(obj.message);
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
    if (obj.type === DOWNLOADING) {
        console.log(obj.data);
    } else if (obj.type === TYPING_INDICATOR) {
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

        if (!document.hasFocus()) {
            notify(obj.user.name, obj.message, obj.user.image);
        }

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
    Prism.highlightAll();
}

function notify(user, message, image) {
    Push.create(user, {
        body: message.substring(0, 30),
        icon: image,
        timeout: 4000,
        onClick: function () {
            window.focus();
            this.close();
        }
    });
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
            document.getElementById("preview_message").innerHTML = "";
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
            isTyping: text.length !== 0 && socket.readyState === 1 && !text.includes("/pm")
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
    $('.collapse').collapse('toggle');
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

function getCaret(el) {
    if (el.selectionStart) {
        return el.selectionStart;
    } else if (document.selection) {
        el.focus();
        let r = document.selection.createRange();
        if (r == null) {
            return 0;
        }
        let re = el.createTextRange(), rc = re.duplicate();
        re.moveToBookmark(r.getBookmark());
        rc.setEndPoint('EndToStart', re);
        return rc.text.length;
    }
    return 0;
}

function setCaretPosition(elemId, caretPos) {
    let elem = document.getElementById(elemId);

    if (elem != null) {
        if (elem.createTextRange) {
            let range = elem.createTextRange();
            range.move('character', caretPos);
            range.select();
        } else {
            if (elem.selectionStart) {
                elem.focus();
                elem.setSelectionRange(caretPos, caretPos);
            } else
                elem.focus();
        }
    }
}

String.prototype.splice = function (idx, rem, str) {
    return this.slice(0, idx) + str + this.slice(idx + Math.abs(rem));
};


function insertText(textToInsert, middle) {
    let command = document.getElementById("commandInput");
    command.value = command.value.toString().splice(getCaret(command), 0, textToInsert);
    setCaretPosition("commandInput", command.value.toString().indexOf(textToInsert) + middle);
}

function onColor() {
    insertText("[style color=\"\"][/style]", 16);
}

function onItalics() {
    insertText("[i][/i]", 3);
}

function onBold() {
    insertText("[b][/b]", 3);
}

function onUnderline() {
    insertText("[u][/u]", 3);
}

function onStrikethrough() {
    insertText("[s][/s]", 3);
}

function onImg() {
    insertText("[img][/img]", 5);
}

function onCode() {
    insertText("[code lang=\"\"][/code]", 14);
}

function onURL() {
    insertText("[url=][/url]", 6);
}

function preventFocus(id) {
    // Prevent capturing focus by the button.
    $(id).on('mousedown', function (event) {
        event.preventDefault();
    });
}

function setTextButtonUp(id, method) {
    document.getElementById(id).onclick = method;
    preventFocus("#" + id);
}

const inputElement = document.getElementById("commandInput");

function previewText() {
    document.getElementById("preview_message").innerHTML = parseBBCode(inputElement.value);//.replace(/\n\r?/g, '<br />'));//.replace(/\n/g, "<br />"));
    let objDiv = document.getElementById("preview_message");
    objDiv.scrollTop = objDiv.scrollHeight;
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

    //stylize text
    setTextButtonUp("color_text", onColor);
    setTextButtonUp("italics_text", onItalics);
    setTextButtonUp("bold_text", onBold);
    setTextButtonUp("underline_text", onUnderline);
    setTextButtonUp("strikethrough_text", onStrikethrough);
    setTextButtonUp("img_text", onImg);
    setTextButtonUp("code_text", onCode);
    setTextButtonUp("url_text", onURL);

    document.getElementById("commandInput").onkeypress = function (e) {
        if (e.code === 'Enter') {
            if (!tributeEntered) {
                if (e.shiftKey) {
                    //Don't do anything
                    //this.value.replace(/\n\r?/g, '<br />');
                    //this.value = this.value.replace(/\n\r?/g, '<br />');
                } else {
                    e.preventDefault();
                    onSend();
                }
            }
            tributeEntered = false;
        } else if (e.ctrlKey) {
            switch (e.key) {
                case 'i':
                    onItalics();
                    break;
                case 'z':
                    onBold();
                    break;
                case 'c':
                    onColor();
                    break;
                case 'u':
                    onUnderline();
                    break;
                case 's':
                    onStrikethrough();
                    break;
                default:
                    break;
            }
        }
        onSendTyping();
    };

    document.getElementById("commandInput").oninput = function () {
        previewText();
        Prism.highlightAll();
    };

    document.getElementById('commandInput').addEventListener('tribute-replaced', function (e) {
        console.log('Original event that triggered text replacement:', e.detail.event);
        console.log('Matched item:', e.detail.item);
        tributeEntered = true;
    });

}

function parseBBCode(text) {
    const parsed = core.process(text, {
        render: BbobHtml.render,
    });
    return parsed.html;
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
