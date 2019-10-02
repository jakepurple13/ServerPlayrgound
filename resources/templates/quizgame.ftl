<#-- @ftlvariable name="data" type="com.example.QuizInfo" -->
<!DOCTYPE html>
<html lang="en">
<head>
    <title>${data.enterTitle}</title>
    <style>
        body {
            font-family: Open Sans, serif;
        }

        h1 {
            text-align: center;
        }

        #title {
            text-decoration: underline;
        }

        #quiz_container {
            text-align: center;
        }

        #quiz {
            text-indent: 10px;
            display: none;
        }

        .button {
            border: 4px solid;
            border-radius: 5px;
            width: fit-content;
            padding-left: 5px;
            padding-right: 5px;
            position: relative;
            float: right;
            background-color: #DCDCDC;
            color: black;
            margin: 0 2px 0 2px;
        }

        .infoText {
            border: 4px solid;
            border-radius: 5px;
            width: fit-content;
            padding-left: 5px;
            padding-right: 5px;
            position: relative;
            float: left;
            background-color: #DCDCDC;
            color: black;
            margin: 0 2px 0 2px;
        }

        .button.active {
            background-color: #F8F8FF;
            color: #525252;
        }

        button {
            position: relative;
            float: right;
        }

        .button a {
            text-decoration: none;
            color: black;
        }

        dd {
            display: block;
            margin-left: 40px;
            color: #aaaaaa;
        }

        dt {
            display: block;
            color: #aaaaaa;
        }

        dl {
            display: block;
            margin: 1em 0;
        }

        #container {
            width: 75%;
            margin: auto;
            padding: 0 25px 40px 10px;
            background-color: #1E90FF;
            border: 4px solid #B0E0E6;
            border-radius: 5px;
            color: #FFFFFF;
            font-weight: bold;
            box-shadow: 5px 5px 5px #888;
        }

        ul {
            list-style-type: none;
            padding: 0;
            margin: 0;
        }

        #next {
            display: none;
        }

        #prev {
            display: none;
        }

        #start {
            display: none;
        }

        table.darkTable {
            font-family: "Arial Black", Gadget, sans-serif;
            border: 2px solid #000000;
            background-color: #4A4A4A;
            width: 80%;
            height: 200px;
            text-align: center;
            border-collapse: collapse;
            border-radius: 5px;
            /*the 5 below is what I added*/
            margin-top: 20px;
            left: 10%;
            right: 10%;
            margin-left: auto;
            margin-right: auto;
        }

        table.darkTable td, table.darkTable th {
            border: 1px solid #4A4A4A;
            padding: 3px 2px;
        }

        table.darkTable tbody td {
            font-size: 13px;
            color: #E6E6E6;
        }

        table.darkTable tr:nth-child(even) {
            background: #888888;
        }

        table.darkTable thead {
            background: #000000;
            border-bottom: 3px solid #000000;
        }

        table.darkTable thead th {
            font-size: 15px;
            font-weight: bold;
            color: #E6E6E6;
            text-align: center;
            border-left: 2px solid #4A4A4A;
        }

        table.darkTable thead th:first-child {
            border-left: none;
        }

        table.darkTable tfoot {
            font-size: 12px;
            font-weight: bold;
            color: #E6E6E6;
            background: #000000;
            background: -moz-linear-gradient(top, #404040 0%, #191919 66%, #000000 100%);
            background: -webkit-linear-gradient(top, #404040 0%, #191919 66%, #000000 100%);
            background: linear-gradient(to bottom, #404040 0%, #191919 66%, #000000 100%);
            border-top: 1px solid #4A4A4A;
        }

        table.darkTable tfoot td {
            font-size: 12px;
        }

    </style>
    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open Sans"/>
</head>
<body style="background-color: #202124">
<div id='container'>
    <div id='title'>
        <h1>${data.enterTitle}</h1>
    </div>
    <br/>
    <div id="quiz_container">
        <div id='quiz'></div>
    </div>
    <div class='infoText' id="quiz_count">0/0</div>
    <div class='button' id='next'><a href='#'>Next</a></div>
    <div class='button' id='prev'><a href='#'>Prev</a></div>
    <div class='button' id='start'><a href='#'>Start Over</a></div>
    <!-- <button class='' id='next'>Next</a></button>
    <button class='' id='prev'>Prev</a></button>
    <button class='' id='start'> Start Over</a></button> -->
</div>

<div id="highScoreList"></div>

<div class="modal fade" id="exampleModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="exampleModalLabel">${data.modalTitle}</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <div class="input-group input-group-sm mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text" id="image_changes">${data.modalTitle}</span>
                    </div>
                    <input type="text" id="quiz_choice" class="form-control" aria-label="Small"
                           aria-describedby="inputGroup-sizing-sm">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" id="closeGame" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" id="startGame" data-dismiss="modal">Go!</button>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="highScoreModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="highScoreLabel">Score</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <span class="input-group-text" id="scoreQuiz"></span>
                <span class="input-group-text" id="scoreScore"></span>
                <div class="input-group input-group-sm mb-3">
                    <div class="input-group-prepend">
                        <span class="input-group-text" id="scoreName">Enter Name</span>
                    </div>
                    <input type="text" id="highScoreName" class="form-control" aria-label="Small"
                           aria-describedby="inputGroup-sizing-sm">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" id="noSubmit" data-dismiss="modal">Close</button>
                <button type="button" class="btn btn-primary" id="submitScore">Submit</button>
            </div>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.3.1.min.js"
        integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
      integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
        crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/gasparesganga-jquery-loading-overlay@2.1.6/dist/loadingoverlay.min.js"></script>
<script type='text/javascript'>
    (function () {

        function highScoreListSetup() {
            $.ajax({
                url: '${data.highScoreLink}',
                type: "GET",
                dataType: "html",
                success: function (json) {
                    $("#highList").remove();
                    $("#highScoreList").html(json);
                }
            });
        }
        if("${data.highScoreLink}" !== "") {
            highScoreListSetup();
        }

        function submitHighScore() {
            $("#highScoreModal").LoadingOverlay("show", {
                image: "",
                text: "Submitting..."
            });
            $.ajax({
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                url: '${data.postHighScoreLink}',
                type: "POST",
                dataType: "json",
                data: JSON.stringify({
                    name: $('#highScoreName').val(),
                    artist: $('#quiz_choice').val(),
                    score: correct + "/" + questions.length
                }),
                success: function (json) {
                    let hsm = $("#highScoreModal");
                    hsm.LoadingOverlay("hide");
                    let text = json.submitted;
                    let displayText = "";
                    if (text) {
                        displayText = "Submitted Successful";
                    } else {
                        displayText = "Submitted Failed";
                    }
                    $.LoadingOverlay("show", {
                        image: "",
                        text: displayText
                    });
                    setTimeout(function () {
                        if (text) {
                            $('#highScoreModal').modal('hide');
                            highScoreListSetup();
                        }
                        $.LoadingOverlay("hide");
                    }, 2000);
                }
            });
        }

        function scoreModalSetup() {
            document.getElementById("scoreScore").innerText = correct + "/" + questions.length;
            document.getElementById("scoreQuiz").innerText = $('#quiz_choice').val();
        }

        let questions;

        let questionCounter = 0; //Tracks question number
        let selections = []; //Array containing user choices
        let correct = 0;
        const quiz = $("#quiz"); //Quiz div object

        function startGame() {
            $("#container").LoadingOverlay("show", {
                background: "#B0E0E6"
            });
            $.ajax({
                url: '${data.questionUrl}',
                type: "GET",
                dataType: "json",
                success: function (json) {
                    $("#container").LoadingOverlay("hide");
                    questions = json;
                    $("#start").hide();
                    // Display initial question
                    displayNext();
                },
                error: function (a, b, c) {
                    $("#container").LoadingOverlay("hide");
                    alert("Something went wrong. Please try again")
                }
            });
        }

        function closeGame() {
            $("#start").show();
        }

        $('#exampleModal').modal('show');
        document.getElementById("startGame").onclick = startGame;
        document.getElementById("closeGame").onclick = closeGame;
        document.getElementById("submitScore").onclick = submitHighScore;
        if("${data.postHighScoreLink}" !== "") {
            document.getElementById("submitScore").hidden = true;
        }

        $("#start").show();

        // Click handler for the 'next' button
        $("#next").on("click", function (e) {
            e.preventDefault();
            // Suspend click listener during fade animation
            if (quiz.is(":animated")) {
                return false;
            }
            choose();

            // If no user selection, progress is stopped
            if (isNaN(selections[questionCounter])) {
                alert("Please make a selection!");
            } else {
                questionCounter++;
                displayNext();
            }
        });

        // Click handler for the 'prev' button
        $("#prev").on("click", function (e) {
            e.preventDefault();

            if (quiz.is(":animated")) {
                return false;
            }
            choose();
            questionCounter--;
            displayNext();
        });

        // Click handler for the 'Start Over' button
        $("#start").on("click", function (e) {
            $('#exampleModal').modal('show');
            e.preventDefault();

            if (quiz.is(":animated")) {
                return false;
            }
            questionCounter = 0;
            selections = [];
            correct = 0;
            //displayNext();
            //$("#start").hide();
        });

        // Animates buttons on hover
        $(".button").on("mouseenter", function () {
            $(this).addClass("active");
        });
        $(".button").on("mouseleave", function () {
            $(this).removeClass("active");
        });

        // Creates and returns the div that contains the questions and
        // the answer selections
        function createQuestionElement(index) {
            const qElement = $("<div>", {
                id: "question"
            });

            const header = $("<h2>Question " + (index + 1) + ":</h2>");
            qElement.append(header);

            const question = $("<p>").append(questions[index].question);
            qElement.append(question);

            const radioButtons = createRadios(index);
            qElement.append(radioButtons);

            return qElement;
        }

        // Creates a list of the answer choices as radio inputs
        function createRadios(index) {
            const radioList = $("<ul style='text-align: left;display: inline-block'>");
            let item;
            let input = "";
            for (let i = 0; i < questions[index].choices.length; i++) {
                item = $("<li>");
                input = '<input type="radio" id="' + questions[index].choices[i] + '" name="answer" value=' + i + " />";
                let s = '<label for="' + questions[index].choices[i] + '">' + questions[index].choices[i] + '</label>';
                input += s;
                item.append(input);
                radioList.append(item);
            }
            return radioList;
        }

        // Reads the user selection and pushes the value to an array
        function choose() {
            selections[questionCounter] = +$('input[name="answer"]:checked').val();
        }

        // Displays next requested element
        function displayNext() {
            $('#quiz_count').text(questionCounter + "/" + questions.length);
            quiz.fadeOut(function () {
                $("#question").remove();

                if (questionCounter < questions.length) {
                    const nextQuestion = createQuestionElement(questionCounter);
                    quiz.append(nextQuestion).fadeIn();
                    if (!isNaN(selections[questionCounter])) {
                        $("input[value=" + selections[questionCounter] + "]").prop(
                            "checked",
                            true
                        );
                    }

                    // Controls display of 'prev' button
                    if (questionCounter === 1) {
                        $("#prev").show();
                    } else if (questionCounter === 0) {
                        $("#prev").hide();
                        $("#next").show();
                    }
                } else {
                    const scoreElem = displayScore();
                    quiz.append(scoreElem).fadeIn();
                    $("#next").hide();
                    $("#prev").hide();
                    $("#start").show();
                    scoreModalSetup();
                    $('#highScoreModal').modal('show');
                }
            });
        }

        // Computes score and returns a paragraph element to be displayed
        function displayScore() {
            const score = $("<p>", {id: "question"});

            let numCorrect = 0;
            for (let i = 0; i < selections.length; i++) {
                if (questions[i].choices[selections[i]] === questions[i].correctAnswer) {
                    numCorrect++;
                }
            }

            let aAndQ = "";
            for (let i = 0; i < selections.length; i++) {
                aAndQ += i + ') Your Pick: ' + questions[i].choices[selections[i]] + ' | Correct Answer: ' + questions[i].correctAnswer + "<br /><br />"
            }
            correct = numCorrect;
            score.append(
                "You got " +
                numCorrect +
                " questions out of " +
                questions.length +
                " right!!!<br /><br />" +
                aAndQ
            );
            return score;
        }
    })();
</script>
</body>
</html>