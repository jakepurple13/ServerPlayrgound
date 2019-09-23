<#-- @ftlvariable name="data" type="com.example.EpisodeApiInfo" -->
<#--noinspection ALL-->
<#--noinspection HtmlUnknownTarget-->
<!DOCTYPE html>
<html>

<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="/chat/helperUtils.js"></script>

    <style>
        * {
            box-sizing: border-box;
        }

        #myInput {
            background-position: 10px 10px;
            background-repeat: no-repeat;
            width: 100%;
            font-size: 16px;
            padding: 12px 20px 12px 40px;
            border: 1px solid #ddd;
            margin-bottom: 12px;
        }

        #myTable {
            border-collapse: collapse;
            width: 100%;
            border: 1px solid #ddd;
            font-size: 18px;
        }

        #myTable th,
        #myTable td {
            text-align: left;
            padding: 12px;
        }

        #myTable tr {
            border-bottom: 1px solid #ddd;
        }

        #myTable tr.header,
        #myTable tr:hover {
            background-color: #f1f1f1;
        }

        .container_img {
            float: left;
            max-width: 60px;
            width: 100%;
            margin-right: 20px;
            border-radius: 50%;
        }

    </style>
</head>

<body>

<h2>Shows</h2>

<div class="input-group mb-3">
    <div class="btn-group" role="group" aria-label="Basic example">
        <button type="button" class="btn btn-secondary">
            <a href="/shows/0-9">0-9</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/a">A</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/b">B</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/c">C</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/d">D</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/e">E</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/f">F</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/g">G</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/h">H</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/i">I</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/j">J</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/k">K</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/l">L</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/m">M</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/n">N</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/o">O</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/p">P</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/q">Q</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/r">R</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/s">S</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/t">T</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/u">U</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/v">V</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/w">W</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/x">X</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/y">Y</a>
        </button>
        <button type="button" class="btn btn-secondary">
            <a href="/shows/z">Z</a>
        </button>
    </div>
</div>

<input type="text" id="myInput" onkeyup="myFunction()" placeholder="Search for names..." title="Type in a name">

<table id="table_format" cellspacing="0" width="100%">
    <tr class="header">
        <th style="width: 20%;">Image</th>
        <th style="width: 10%">Name</th>
        <th style="width: 60%">Description</th>
        <th style="width: 10%">Url</th>
    </tr>
    <tbody id="myTable">
    <#list data as item>
        <tr class="content">
            <td><img class="container_img" src="${item.image}" alt="image"/></td>
            <td>${item.name}</td>
            <td>${item.description}</td>
            <td>${item.url}</td>
        </tr>
    </#list>
    </tbody>
</table>

<script>

    function filterText() {
        var rex = new RegExp($('#filterText').val());
        if (rex == "/all/") {
            clearFilter()
        } else {
            $('.content').hide();
            $('.content').filter(function () {
                return rex.test($(this).text());
            }).show();
        }
    }

    function clearFilter() {
        $('.filterText').val('');
        $('.content').show();
    }

    function myFunction() {
        var input, filter, table, tr, td, i, txtValue;
        input = document.getElementById("myInput");
        filter = input.value.toUpperCase();
        table = document.getElementById("myTable");
        tr = table.getElementsByTagName("tr");
        for (i = 0; i < tr.length; i++) {
            td = tr[i].getElementsByTagName("td")[1];
            if (td) {
                txtValue = td.textContent || td.innerText;
                if (txtValue.toUpperCase().indexOf(filter) > -1) {
                    tr[i].style.display = "";
                } else {
                    tr[i].style.display = "none";
                }
            }
        }
    }

    var table = document.getElementById("myTable");
    if (table != null) {
        for (var i = 0; i < table.rows.length; i++) {
            for (var j = 0; j < table.rows[i].cells.length; j++)
                table.rows[i].onclick = function () {
                    //tableText(this);
                    var u = this.cells[3].innerHTML;
                    openEpisode(u);
                };
        }
    }
</script>

</body>

</html>