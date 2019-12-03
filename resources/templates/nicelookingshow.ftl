<#-- @ftlvariable name="data" type="com.example.EpisodeApiInfo" -->
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>${data.name}</title>
    <style>
        body {
            margin: 0;
        }

        ul {
            list-style: none;
            padding-inline-start: 0;
        }

        a {
            text-decoration: none;
            color: #fff;
            font-family: sans-serif;
        }

        .grid-container {
            display: grid;
            width: 100%;
            height: 100vh;
            grid-template-columns: 1fr;
            grid-template-rows: 2fr 8fr;
            grid-template-areas: "top" "bottom";
        }

        .grid-container > header.top {
            grid-area: top;
            /*   background-color: #222f3e; */
            background-color: #1f2025;
            width: 100vw;
            height: 100%;
            min-height: 215px;
        }

        header.top > .img-container {
            float: left;
            display: block;
            width: 200px;
            padding-top: 1%;
            padding-left: 1%;
            padding-bottom: 1%;
        }

        header.top > .img-container > img {
            position: relative;
            display: block;
            border-radius: 4px;
            box-shadow: 0px 0px 10px;
        }

        .meta-data {
            position: relative;
            float: left;
        }

        .meta-data > .title {
            margin-top: 10px;
            color: #fff;
            font-size: 28px;
            font-family: sans-serif;
        }

        .grid-container > main.bottom {
            /* background-color: green; */
            grid-area: bottom;
            display: flex;
            flex-direction: row;
        }

        .grid-container > main.bottom {
            z-index: 1;
            padding-left: 20px;
            background-color: #17181b;
        }

        main.bottom > aside.seasons {
            display: flex;
            flex-shrink: 1;
            flex-grow: 1;
            /*     padding: 10px; */
            min-width: 165px;
            max-width: 165px;
            height: 100%;
        }

        .season-title {
            /*   font-size: 22px; */
            /*   overflow: hidden; */
            /*   text-overflow: ellipsis; */
            /*   white-space: nowrap; */
            /*   margin-bottom: 15px;
  margin-top: 2px;
  color: #fff;
  font-family: sans-serif; */
            font-size: 24px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-bottom: 15px;
            margin-top: 2px;
            color: #fff;
        }

        .seasons-list ul {
            /*   padding-top: 15px; */
            /*   display: block; */
            /*   height: calc(100vh - 340px); */
            display: block;
            cursor: pointer;
            direction: rtl;
            overflow-x: hidden;
            overflow-y: overlay;
        }

        .seasons-list > ul > li.tab-season {
            display: list-item;
            float: left;
            position: relative;
            width: calc(100% - 7px);
            padding: 0px 15px;
            /*   position: relative; */
        }

        .seasons-list > ul a {
            text-decoration: none;
            color: #fff;
            float: left;
            direction: ltr;
            flex-direction: row;
            font-family: sans-serif;
        }

        .seasons-list li:nth-child(odd) {
            background-color: red;
        }

        .seasons-list li.tab-season:not(:first-child) a {
            margin-top: 5%;
        }

        .show-info-container {
            width: 100%;
            display: grid;
            grid-gap: 10px;
            grid-template-columns: 15fr 85fr;
            grid-template-rows: 2fr;
            grid-template-areas: "seasons episodes";
        }

        .seasons-container {
            grid-area: seasons;
        }

        .episodes-container {
            grid-area: episodes;
        }

        .season-title-container {
            font-size: 24px;
            padding-top: 15px;
            color: #fff;
            font-family: sans-serif;
        }

        .episode-title-container {
            padding-top: 15px;
            color: #fff;
            font-family: sans-serif;
            font-size: 24px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-bottom: 15px;
            /* margin-top: 2px; */
            color: #fff;
            padding-left: 20px;
        }

        .seasons-container ul li {
            padding: 0px 15px;
        }

        .seasons-container ul li a {
            line-height: 34px;
        }

        .seasons-container li:nth-child(odd) {
            background-color: #26272d;
        }

        .seasons-container li:not(:first-child) {
            margin-top: 10px;
        }

        .episodes-container li {
            width: calc(100vh - 5px);
            padding: 0px 15px;
        }

        .episodes-container ul li a {
            /* position: absolute; */
            line-height: 34px;
        }

        .episodes-container ul li a span {
            margin: 0px 15px 0px 5px;
        }

        .episodes-container ul li a div {
            display: inline;
            white-space: nowrap;
            border-left: 1px solid #17181b;
            padding-left: 30px;
            max-width: calc(60vw - 265px);
            text-align: left;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .episodes-container li:nth-child(odd) {
            background-color: #26272d;
        }

        .episodes-container li:not(:first-child) {
            margin-top: 10px;
        }

        /* show info css; i.e year, genres, desc */
        p {
            margin: 0;
        }

        .meta-data .show-infos div {
            color: #fff;
            font-family: sans-serif;
        }

        span.year {
            background-color: #26272d;
            border-radius: 3px;
            padding: 2px 10px;
        }

        .meta-data .show-infos .show-year {
            padding-top: 5px;
            padding-bottom: 5px;
        }

        .meta-data .show-infos .show-genres {
            background-color: #26272d;
            border-radius: 3px;
            padding: 2px 10px;
            float: left;
        }

        .meta-data .show-infos .show-genres:not(:first-child) {
            margin-left: 5px;
        }

        .meta-data .show-infos .show-desc {
            overflow-wrap: break-word;
            float: left;
            overflow-x: hidden;
            margin-top: 5px;
        }

        .show-desc > p {
            max-width: calc(100vw - 500px);
            overflow-wrap: break-word;
            overflow-y: hidden;
            word-wrap: break-word;
        }

        .header-img {
            background-repeat: no-repeat;
            width: 100vw;
            position: absolute;
            background-size: cover;
            min-height: 205px;
            /* filter: blur(10px); */
            filter: blur(60px) brightness(0.7);
            background-position: center;
            opacity: 0;
        }

        .fade-in {
            opacity: 1;
            transition: opacity 0.3s ease-in;
        }

    </style>
</head>

<body>
<!-- <div id="grid-continer">
<header></header>
<main></main>
</div> -->
<div class="grid-container">
    <header class="top">
        <div class="header-cover">
            <div class="header-img fade-in" style="background-image: url('${data.url}');"></div>
        </div>
        <div class="img-container">
            <img src="${data.image}" alt="">
        </div>
        <div class="meta-data">
            <div class="title">${data.name}</div>
            <div class="show-infos">
                <div class="show-year"><a href="${data.url}"><span class="year">${data.url}</span></a></div>
                <div class="genres-container">
                    <label for="id-of-input" class="custom-checkbox">
                        <input type="checkbox" id="id-of-input"/>
                    </label>
                </div>
                <div class="show-desc"><p>${data.description}</p></div>
            </div>
        </div>
    </header>
    <main class="bottom">
        <div class="show-info-container">
            <div class="seasons-container">
                <div class="season-title-container">Seasons</div>
                <ul>
                    <li class="episode-li"><a href="">Coming Soon</a></li>
                    <#--<li class="episode-li"><a href="">Season 1</a></li>
                    <li class="episode-li"><a href="">Season 2</a></li>
                    <li class="episode-li"><a href="">Season 3</a></li>
                    <li class="episode-li"><a href="">Season 4</a></li>
                    <li class="episode-li"><a href="">Season 5</a></li>-->
                    <!-- <li class="episode-li"><a href="">Season 2</a></li>
                    <li class="episode-li"><a href="">Season 3</a></li>
                    <li class="episode-li"><a href="">Season 4</a></li> -->
                </ul>
            </div>
            <div class="episodes-container">
                <div class="episode-title-container">Episodes</div>
                <ul>
                    <#list data.episodeList as i>
                        <li class="episode-li">
                            <a href="${i.url}">
                                <label for="episodeWatched${i.url}" class="custom-checkbox">
                                    <input onclick="episodeChange(this, '${i.url}');" type="checkbox" id="episodeWatched${i.url}"/>
                                </label>
                                <span>${i.name}</span>
                                <div><a href="${i.url}">${i.url}</a></div>
                                <span>
                                    <button class="btn btn-primary" id="vidlink${i?index}" value="${i.url}"
                                            onclick="getVidLink(${i?index})">Get Video Link</button>
                                <br><a target="_blank" id="showlink${i?index}">Link Will Be Here</a>
                                </span>
                            </a>
                        </li>
                    </#list>
                    <#--<li class="episode-li">
                        <a href="/episode_src/87538">
                            <span>S1</span>
                            <span>E1</span>
                            <div>Final Exam</div>
                        </a>
                    </li>-->

                    <!-- <li class="episode-li"><a href="">The Gang Gets Racist</a></li>
                    <li class="episode-li"><a href="">Charlie Wants an Abortion</a></li>
                    <li class="episode-li"><a href="">Underage Drinking: A National Concern</a></li>
                    <li class="episode-li"><a href="">Charlie Has Cancer</a></li>
                    <li class="episode-li"><a href="">Gun Fever</a></li>
                    <li class="episode-li"><a href="">The Gang Finds a Dead Guy</a></li>
                    <li class="episode-li"><a href="">Charlie Got Molested</a></li> -->
                </ul>
            </div>
        </div>
        <!--     <aside class="seasons">
  <div class="season-title">Seasons</div>
  <div class="seasons-list">
    <ul>
      <li class="tab-season"><a href="">Season 1</a></li>
      <li class="tab-season"><a href="">Season 2</a></li>
      <li class="tab-season"><a href="">Season 3</a></li>
      <li class="tab-season"><a href="">Season 4</a></li>
      <li class="tab-season"><a href="">Season 5</a></li>
    </ul>
  </div>
</aside> -->
        <!--     <section class="episodes"></section> -->
    </main>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"
            integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
    <!-- Firebase App (the core Firebase SDK) is always required and must be listed first -->
    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-app.js"></script>

    <!-- Add Firebase products that you want to use -->
    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-auth.js"></script>

    <script src="https://www.gstatic.com/firebasejs/7.1.0/firebase-database.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.2.0/firebase-firestore.js"></script>
    <script type="text/javascript" src="/chat/helperUtils.js"></script>

    <script>
        let table = $('#table');

        function getVidLink(num) {
            const url = $("#vidlink" + num).val();
            $("#showlink" + num).text("Retrieving");
            $.ajax({
                type: "GET",//or POST
                url: "/api/video/" + url.replaceAll("/", "<") + ".json",
                dataType: 'json',
                success: function (responsedata) {
                    let s = $("#showlink" + num);
                    s.attr("href", responsedata.videoLink);
                    s.text("Link get!");
                }
            })
        }

        /*table.on('check.bs.table', function (row, element) {
            console.log(element);
            addEpisode(element);
        });

        table.on('uncheck.bs.table', function (row, element) {
            console.log(element);
            removeEpisode(element);
        });*/

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
                    "name": '${data.name}',
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
            //let e = createElementFromHTML(element.url);
            //console.log(e.href);
            firebase.firestore().collection(firebase.auth().currentUser.uid)
                .doc("${data.url}".replaceAll("/", "<"))
                .update({
                    "episodeInfo": firebase.firestore.FieldValue.arrayUnion({
                        "name": "${data.url}",
                        "url": element
                    })
                });
        }

        function removeEpisode(element) {
            //let e = createElementFromHTML(element.url);
            //console.log(e.href);
            firebase.firestore().collection(firebase.auth().currentUser.uid)
                .doc("${data.url}".replaceAll("/", "<"))
                .update({
                    "episodeInfo": firebase.firestore.FieldValue.arrayRemove({
                        "name": "${data.url}",
                        "url": element
                    })
                });
        }

        function episodeChange(event, url) {
            if (event.checked) {
                addEpisode(url);
            } else {
                removeEpisode(url);
            }
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
                            let info = doc.data().episodeInfo;//.map(getUrl);
                            console.log(info);
                            for(var ep = 0; ep<info.length;ep++) {
                                console.log(info[ep]);
                                document.getElementById('episodeWatched' + info[ep].url).checked = true;
                            }
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
            // Initialize Firebase
            firebase.initializeApp(firebaseConfig);
            initApp();
        };

    </script>
</div>
</body>

</html>