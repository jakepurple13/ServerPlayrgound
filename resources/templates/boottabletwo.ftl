<#-- @ftlvariable name="data" type="com.example.ShowInfo" -->
<!doctype html>
<html lang="en">

<head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Hello, Bootstrap Table!</title>

    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
          integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS" crossorigin="anonymous">
    <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.6.3/css/all.css"
          integrity="sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/" crossorigin="anonymous">
    <link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.15.4/dist/bootstrap-table.min.css">

</head>
<style>

</style>

<body>
<div id="toolbar">
    <div class="input-group mb-3">
        <div class="input-group-prepend">
            <button id="remove" class="btn btn-primary">
                Random
            </button>
        </div>
        <select class="custom-select" id="locale" onchange='loadNewData()'>
            <option disabled selected>Select</option>
            <option value='favorites'>Favorites</option>
            <option value='/api/web/ra.json'>Recent Anime</option>
            <option value='/api/web/rl.json'>Recent TV Shows</option>
            <option value='/api/web/rc.json'>Recent Cartoon</option>
            <option value='/api/web/tgogoanime.json'>Anime</option>
            <option value='/api/web/tputlocker.json'>TV Shows</option>
            <option value='/api/web/ttoon.json'>Cartoon</option>
            <option value='/api/web/all.json'>All</option>
        </select>
        <div class="input-group-append">
            <button id="go_to_chat" class="btn btn-primary"><i class="glyphicon glyphicon-remove"></i>Chat</button>
        </div>
    </div>
    <button id="view_shows_by_letter" class="btn btn-primary">
        View Shows By Letter
    </button>
    <button id="sign_in" class="btn btn-primary">
        Login
    </button>
</div>
<table
        class="table-dark"
        data-toggle="table"
        id="table"
        data-show-export="true"
        data-toolbar="#toolbar"
        data-search="true"
        data-custom-search="customSearch"
        data-show-jump-to="true"
        data-checkbox-header="false"
        data-advanced-search="true"
        data-id-table="advancedTable"
        data-height="750"
        data-virtual-scroll="true"
        data-click-to-select="true"
        data-pagination="true"
        data-url="/api/web/all.json"
        data-page-list="[10, 25, 50, 100, all]"
        data-show-refresh="true">
    <thead>
    <tr>
        <th data-checkbox="true" data-checkbox-enabled="true"></th>
        <th data-field="name">Name</th>
        <th data-field="url">URL</th>
    </tr>
    </thead>
</table>

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
<script src="https://unpkg.com/bootstrap-table@1.15.4/dist/extensions/toolbar/bootstrap-table-toolbar.min.js"></script>

<script src="https://unpkg.com/tableexport.jquery.plugin/tableExport.min.js"></script>
<script src="https://unpkg.com/tableexport.jquery.plugin/libs/jsPDF/jspdf.min.js"></script>
<script src="https://unpkg.com/tableexport.jquery.plugin/libs/jsPDF-AutoTable/jspdf.plugin.autotable.js"></script>
<script src="https://unpkg.com/bootstrap-table@1.15.4/dist/extensions/export/bootstrap-table-export.min.js"></script>

<script type="text/javascript" src="/chat/helperUtils.js"></script>

<!-- Firebase App (the core Firebase SDK) is always required and must be listed first -->
<script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-app.js"></script>

<!-- Add Firebase products that you want to use -->
<script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-auth.js"></script>

<script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-database.js"></script>
<script src="https://www.gstatic.com/firebasejs/7.2.0/firebase-firestore.js"></script>

</body>
<script>

    function favoriteShow(element) {
        firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc(replaceAll(element.url, "/", "<"))
            .set({
                "name": element.name,
                "url": element.url,
                "showNum": 0
            });
    }

    function unfavoriteShow(element) {
        firebase.firestore().collection(firebase.auth().currentUser.uid)
            .doc(replaceAll(element.url, "/", "<"))
            .delete();
    }

    var table = $('#table');

    function getUrl(info) {
        return info.url//"<a href=\"" + info.url + "\">" + info.url + "</a>"
    }

    function getDocUrl(info) {
        return replaceAll(info.id, "<", "/");
    }

    function initApp() {
        firebase.auth().onAuthStateChanged(function (user) {
            if (user) {

                $('#sign_in').text("Logout");

                table.on('check.bs.table', function (row, element) {
                    console.log(element);
                    favoriteShow(element);
                });

                table.on('uncheck.bs.table', function (row, element) {
                    console.log(element);
                    unfavoriteShow(element);
                });

                // [END_EXCLUDE]
                const database = firebase.firestore().collection(firebase.auth().currentUser.uid);
                database.get().then(function (collection) {
                    console.log("Collection data:", collection);
                    let info = collection.docs.map(getDocUrl);
                    console.log(info);
                    table.bootstrapTable('checkBy', {field: 'url', values: info});
                    table.on('page-change.bs.table', function (number, size) {
                        table.bootstrapTable('checkBy', {field: 'url', values: info});
                    });
                }).catch(function (error) {
                    console.log("Error getting document:", error);
                });
            } else {
                $('#sign_in').text("Login");
            }
        });
    }

    window.onload = function () {
        // Initialize Firebase
        firebase.initializeApp(firebaseConfig);
        initApp();
    };

    function favDocs(info) {
        return {
            'name': info.data().name,
            'url': info.data().url
        }
    }

    function loadNewData() {
        var url = $('#locale').val();
        console.log(url);
        if (url === "favorites") {
            // [END_EXCLUDE]
            const database = firebase.firestore().collection(firebase.auth().currentUser.uid);
            database.get().then(function (collection) {
                table.bootstrapTable('load', collection.docs.map(favDocs));
            }).catch(function (error) {
                console.log("Error getting document:", error);
            });
        } else {
            table.bootstrapTable('refresh', {url: url});
        }
    }

    table.bootstrapTable({
        exportDataType: "all",
        onClickRow: function (row, element, field) {
            console.log(element[0]);
            var u = element[0].cells[1].innerHTML;
            openEpisode(u);
        }
    });

    $('#sign_in').click(function () {
        window.open("/userinfo", 'FUN', "width=500,height=500");
    });

    $('#remove').click(function () {
        $.getJSON({
            url: '/api/web/random.json',
            success: function (json) {
                openEpisode(json.url);
            }
        });
    });

    $('#go_to_chat').click(function () {
        window.open("/chat", '_blank');
    });

    $('#view_shows_by_letter').click(function () {
        window.open("/shows/0-9", '_blank');
    });

    function customSearch(data, text) {
        return data.filter(function (row) {
            return row.name.toLowerCase().indexOf(text.toLowerCase()) > -1
        })
    }

</script>

</html>