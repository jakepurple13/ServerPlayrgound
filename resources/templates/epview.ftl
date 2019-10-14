<#-- @ftlvariable name="data" type="com.example.EpisodeApiInfo" -->
<!doctype html>
<html>

<head>
    <title>${data.name}</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
          integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS" crossorigin="anonymous">
    <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.6.3/css/all.css"
          integrity="sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/" crossorigin="anonymous">
    <link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.15.4/dist/bootstrap-table.min.css">
    <style>

        .grid-container {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr 1fr;
            grid-template-rows: 0.5fr 1fr 1fr 1fr 1fr;
            grid-template-areas: "cover title title title" "cover description description description" "episodelist episodelist episodelist episodelist" "episodelist episodelist episodelist episodelist" "episodelist episodelist episodelist episodelist";
        }

        .cover {
            grid-area: cover;
            background-size: cover;
            display: flex;
            justify-content: center;
            align-items: center;
            overflow: hidden;
        }

        .title {
            grid-area: title;
        }

        .description {
            grid-area: description;
        }

        .episodelist {
            grid-area: episodelist;
        }

        .cover img {
            flex-shrink: 0;
            min-width: 100%;
            min-height: 100%;
        }

    </style>
</head>

<body>
<div class="grid-container">
    <div class="title">
        <h2>
            <label for="id-of-input" class="custom-checkbox">
                <input type="checkbox" id="id-of-input"/>
                <span>${data.name}</span>
            </label>
        </h2>
        <p><a href="${data.url}">${data.url}</a></p>
    </div>
    <div class="description">
        <p>${data.description}</p>
    </div>
    <div class="cover">
        <img class="imagecover" id="cover_image" src="${data.image}">
    </div>

    <div class="episodelist">
        <div id="toolbar">

        </div>
        <table data-toggle="table"
               data-toolbar="#toolbar"
               class="table-dark table-fixed"
               data-search="true"
               data-show-jump-to="true"
               data-checkbox-header="false"
               id="table"
               data-height="750"
               data-pagination="true"
               data-page-list="[10, 25, 50, 100, all]">
            <thead>
            <tr class="header">
                <th data-checkbox="true" data-checkbox-enabled="true"></th>
                <th data-field="name" data-width="300">Episode Name</th>
                <th data-field="url" data-width="300">Url</th>
                <th data-field="vidurl" data-width="300">Video Link</th>
            </tr>
            </thead>
            <tbody>
            <#list data.episodeList as i>
                <tr>
                    <td></td>
                    <td>${i.name}</td>
                    <td><a href="${i.url}">${i.url}</a></td>
                    <td>
                        <button class="btn btn-primary" id="vidlink${i?index}" value="${i.url}"
                                onclick="getVidLink(${i?index})">Get Video Link
                        </button>
                        <br><a target="_blank" id="showlink${i?index}">Link Will Be Here</a></td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

    <script src="https://code.jquery.com/jquery-3.3.1.min.js"
            integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js"
            integrity="sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut"
            crossorigin="anonymous"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js"
            integrity="sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k"
            crossorigin="anonymous"></script>
    <script src="https://unpkg.com/bootstrap-table@1.15.4/dist/bootstrap-table.min.js"></script>
    <script src="https://unpkg.com/bootstrap-table@1.15.4/dist/extensions/page-jump-to/bootstrap-table-page-jump-to.min.js"></script>
    <!-- Firebase App (the core Firebase SDK) is always required and must be listed first -->
    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-app.js"></script>

    <!-- Add Firebase products that you want to use -->
    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-auth.js"></script>

    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-database.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.2.0/firebase-firestore.js"></script>
</div>
</body>
<script>
    function getVidLink(num) {
        var url = $("#vidlink" + num).val();
        $("#showlink" + num).text("Retrieving");
        $.ajax({
            type: "GET",//or POST
            url: "/api/video/" + url.replaceAll("/", "_") + ".json",
            dataType: 'json',
            success: function (responsedata) {
                let s = $("#showlink" + num);
                s.attr("href", responsedata.videoLink);
                s.text("Link get!");
            }
        })
    }

    String.prototype.replaceAll = function (str1, str2, ignore) {
        return this.replace(new RegExp(str1.replace(/([\/\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, "\\$&"), (ignore ? "gi" : "g")), (typeof (str2) == "string") ? str2.replace(/\$/g, "$$$$") : str2);
    };

    $('#table').on('check.bs.table', function (row, element) {
        console.log(element);
        addEpisode(element);
    });

    $('#table').on('uncheck.bs.table', function (row, element) {
        console.log(element);
        removeEpisode(element);
    });

    function fav() {
        if (document.getElementById('id-of-input').checked) {
            favoriteShow();
        } else {
            unfavoriteShow();
        }
    }

    function favoriteShow() {
        firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc("${data.url}".replaceAll("/", "<"))
            .set({
                "name": "${data.name}",
                "url": "${data.url}",
                "showNum": ${data.episodeList?size}
            });
    }

    function unfavoriteShow() {
        firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc("${data.url}".replaceAll("/", "<"))
            .delete();
    }

    function createElementFromHTML(htmlString) {
        let div = document.createElement('div');
        div.innerHTML = htmlString.trim();

        // Change this to div.childNodes to support multiple top-level nodes
        return div.firstChild;
    }

    function addEpisode(element) {
        let e = createElementFromHTML(element.url);
        console.log(e.href);
        const database = firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc("${data.url}".replaceAll("/", "<"))
            .update({
                "episodeInfo": firebase.firestore.FieldValue.arrayUnion({
                    "name": "${data.url}",
                    "url": e.href
                })
            });
    }

    function removeEpisode(element) {
        let e = createElementFromHTML(element.url);
        console.log(e.href);
        const database = firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc("${data.url}".replaceAll("/", "<"))
            .update({
                "episodeInfo": firebase.firestore.FieldValue.arrayRemove({
                    "name": "${data.url}",
                    "url": e.href
                })
            });
    }

    function getUrl(info) {
        return "<a href=\"" + info.url + "\">" + info.url + "</a>"
    }

    function initApp() {
        firebase.auth().onAuthStateChanged(function (user) {
            if (user) {

                document.getElementById("id-of-input").addEventListener('change', (event) => {
                    if (event.target.checked) {
                        favoriteShow();
                    } else {
                        unfavoriteShow();
                    }
                });

                // [END_EXCLUDE]
                let url = "${data.url}";
                console.log(url);
                const database = firebase.firestore().collection(firebase.auth().currentUser.uid).doc(url.replaceAll("/", "<"));
                database.get().then(function (doc) {
                    if (doc.exists) {
                        document.getElementById('id-of-input').checked = true;
                        console.log("Document data:", doc.data());
                        let info = doc.data().episodeInfo.map(getUrl);
                        console.log(info);
                        $('#table').bootstrapTable('checkBy', {field: 'url', values: info});
                        $('#table').on('page-change.bs.table', function (number, size) {
                            $('#table').bootstrapTable('checkBy', {field: 'url', values: info});
                        });
                    } else {
                        // doc.data() will be undefined in this case
                        console.log("No such document!");
                    }
                }).catch(function (error) {
                    console.log("Error getting document:", error);
                });
            }
        });
    }

    window.onload = function () {
        const firebaseConfig = {
            //TODO: DONT FORGET HERE!!!
            apiKey: "AIzaSyDhqB2JjisKADWFPKuk_44-MmZP25_Eijs",
            authDomain: "chesstest-3cd2a.firebaseapp.com",
            databaseURL: "https://chesstest-3cd2a.firebaseio.com",
            projectId: "chesstest-3cd2a",
            storageBucket: "chesstest-3cd2a.appspot.com",
            messagingSenderId: "139228686141",
            appId: "1:139228686141:web:963e504a52c674f2eb4754"
        };
        // Initialize Firebase
        firebase.initializeApp(firebaseConfig);
        initApp();
    };

</script>

</html>