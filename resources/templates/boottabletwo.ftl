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


<div class="demo-layout mdl-layout mdl-js-layout mdl-layout--fixed-header">

    <!-- Header section containing title -->
    <header class="mdl-layout__header mdl-color-text--white mdl-color--light-blue-700">
        <div class="mdl-cell mdl-cell--12-col mdl-cell--12-col-tablet mdl-grid">
            <div class="mdl-layout__header-row mdl-cell mdl-cell--12-col mdl-cell--12-col-tablet mdl-cell--8-col-desktop">
                <a href="/"><h3>Firebase Authentication</h3></a>
            </div>
        </div>
    </header>

    <main class="mdl-layout__content mdl-color--grey-100">
        <div class="mdl-cell mdl-cell--12-col mdl-cell--12-col-tablet mdl-grid">

            <!-- Container for the demo -->
            <div class="mdl-card mdl-shadow--2dp mdl-cell mdl-cell--12-col mdl-cell--12-col-tablet mdl-cell--12-col-desktop">
                <div class="mdl-card__title mdl-color--light-blue-600 mdl-color-text--white">
                    <h2 class="mdl-card__title-text">Google Authentication with Popup</h2>
                </div>
                <div class="mdl-card__supporting-text mdl-color-text--grey-600">
                    <p>Sign in with your Google account below.</p>

                    <!-- Button that handles sign-in/sign-out -->
                    <button class="mdl-button mdl-js-button mdl-button--raised" id="quickstart-sign-in">Sign in with Google</button>

                    <!-- Container where we'll display the user details -->
                    <div class="quickstart-user-details-container">
                        Firebase sign-in status: <span id="quickstart-sign-in-status">Unknown</span>
                        <div>Firebase auth <code>currentUser</code> object value:</div>
                        <pre><code id="quickstart-account-details">null</code></pre>
                        <div>Google OAuth Access Token:</div>
                        <pre><code id="quickstart-oauthtoken">null</code></pre>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>

</body>
<script>

    /**
     * Function called when clicking the Login/Logout button.
     */
    // [START buttoncallback]
    function toggleSignIn() {
        if (!firebase.auth().currentUser) {
            // [START createprovider]
            var provider = new firebase.auth.GoogleAuthProvider();
            // [END createprovider]
            // [START addscopes]
            provider.addScope('https://www.googleapis.com/auth/contacts.readonly');
            // [END addscopes]
            // [START signin]
            firebase.auth().signInWithPopup(provider).then(function(result) {
                // This gives you a Google Access Token. You can use it to access the Google API.
                var token = result.credential.accessToken;
                // The signed-in user info.
                var user = result.user;
                // [START_EXCLUDE]
                document.getElementById('quickstart-oauthtoken').textContent = token;
                // [END_EXCLUDE]
            }).catch(function(error) {
                // Handle Errors here.
                var errorCode = error.code;
                var errorMessage = error.message;
                // The email of the user's account used.
                var email = error.email;
                // The firebase.auth.AuthCredential type that was used.
                var credential = error.credential;
                // [START_EXCLUDE]
                if (errorCode === 'auth/account-exists-with-different-credential') {
                    alert('You have already signed up with a different auth provider for that email.');
                    // If you are using multiple auth providers on your app you should handle linking
                    // the user's accounts here.
                } else {
                    console.error(error);
                }
                // [END_EXCLUDE]
            });
            // [END signin]
        } else {
            // [START signout]
            firebase.auth().signOut();
            // [END signout]
        }
        // [START_EXCLUDE]
        document.getElementById('quickstart-sign-in').disabled = true;
        // [END_EXCLUDE]
    }
    // [END buttoncallback]
    /**
     * initApp handles setting up UI event listeners and registering Firebase auth listeners:
     *  - firebase.auth().onAuthStateChanged: This listener is called when the user is signed in or
     *    out, and that is where we update the UI.
     */
    function initApp() {
        // Listening for auth state changes.
        // [START authstatelistener]
        firebase.auth().onAuthStateChanged(function(user) {
            if (user) {
                // User is signed in.
                var displayName = user.displayName;
                var email = user.email;
                var emailVerified = user.emailVerified;
                var photoURL = user.photoURL;
                var isAnonymous = user.isAnonymous;
                var uid = user.uid;
                var providerData = user.providerData;
                // [START_EXCLUDE]
                document.getElementById('quickstart-sign-in-status').textContent = 'Signed in';
                document.getElementById('quickstart-sign-in').textContent = 'Sign out';
                document.getElementById('quickstart-account-details').textContent = JSON.stringify(user, null, '  ');
                // [END_EXCLUDE]

                var database = firebase.database().ref(firebase.auth().currentUser.uid);
                database.once('value').then(function(snapshot) {
                    var username = (snapshot.val() && snapshot.val().username) || 'Anonymous';
                    // ...
                    console.log(snapshot.val());
                    var realData = JSON.parse(snapshot.val());
                    console.log(realData);
                    document.getElementById('quickstart-account-details').textContent = realData;
                });
            } else {
                // User is signed out.
                // [START_EXCLUDE]
                document.getElementById('quickstart-sign-in-status').textContent = 'Signed out';
                document.getElementById('quickstart-sign-in').textContent = 'Sign in with Google';
                document.getElementById('quickstart-account-details').textContent = 'null';
                document.getElementById('quickstart-oauthtoken').textContent = 'null';
                // [END_EXCLUDE]
            }
            // [START_EXCLUDE]
            document.getElementById('quickstart-sign-in').disabled = false;
            // [END_EXCLUDE]
        });
        // [END authstatelistener]
        document.getElementById('quickstart-sign-in').addEventListener('click', toggleSignIn, false);
    }
    window.onload = function() {
        var firebaseConfig = {
            //TODO: DONT FORGET HERE!!!
        };
        // Initialize Firebase
        firebase.initializeApp(firebaseConfig);
        initApp();
    };

    var table = $('#table');

    function loadNewData() {
        var url = $('#locale').val();
        console.log(url);
        table.bootstrapTable('refresh', {url: url});
    }

    table.bootstrapTable({
        exportDataType: "all",
        onClickRow: function (row, element, field) {
            console.log(element[0]);
            var u = element[0].cells[1].innerHTML;
            openEpisode(u);
        }
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