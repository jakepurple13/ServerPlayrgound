<#-- @ftlvariable name="data" type="com.example.ShowInfo" -->
<!DOCTYPE html>
<html>

<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>

    <style>
        * {
            box-sizing: border-box;
        }

        #myInput {
            background-image: url('/css/searchicon.png');
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
    </style>
</head>

<body>

<h2>Shows</h2>

<input type="text" id="myInput" onkeyup="myFunction()" placeholder="Search for names.." title="Type in a name">

<table id="table_format" cellspacing="0" width="100%">
    <tr class="header">
        <th style="width:10%;">#</th>
        <th style="width:60%;">Name</th>
        <th style="width:30%;">Url
            <select id='filterText' style='display:inline-block' onchange='filterText()'>
                <option disabled selected>Select</option>
                <option value='animetoon'>Cartoon</option>
                <option value='putlocker'>Putlocker</option>
                <option value='gogoanime'>Gogoanime</option>
                <option value='all'>All</option>
            </select>
        </th>
    </tr>
    <tbody id="myTable">
    <#list 0..data?size-1 as i>
        <tr class="content">
            <td>${i}</td>
            <td>${data[i].name}</td>
            <td>${data[i].url}</td>
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
                    tableText(this);
                };
        }
    }

    function tableText(tableCell) {
        var type = "";
        var u = tableCell.cells[2].innerHTML;

        if (u.includes("gogoanime")) {
            type = "g";
        } else if (u.includes("putlocker")) {
            type = "p";
        } else if (u.includes("animetoon")) {
            type = "a";
        }
        var location = u.split("/");
        var locate = location.filter(x = > x != ""
    ).
        slice(-1)[0];
        window.open("/nsi/" + type + locate, '_blank');
    }
</script>

</body>

</html>