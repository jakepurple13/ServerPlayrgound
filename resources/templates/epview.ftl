<#-- @ftlvariable name="data" type="com.example.EpisodeApiInfo" -->
<!DOCTYPE html>
<html>

<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
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

    <h2>${data.name}</h2>

    <p>${data.description}</p>

    <p><a href="${data.url}">${data.url}</a></p>

    <img id="cover_image" src="clear.gif">

    <table id="myTable">
        <tr class="header">
            <th style="width:60%;">Episode Name</th>
            <th style="width:40%;">Url</th>
        </tr>
        <#list data.episodeList as i>
            <tr>
                <td>${i.name}</td>
                <td><a href="${i.url}">${i.url}</a></td>
            </tr>
        </#list>
    </table>

    <script>
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
                var location = tableCell.cells[1].innerHTML.replace(" ", "-");
                //alert(location);
                //window.open("/nsi/" + location, '_blank');
                //window.location.href = "/nsi/" + location;
            }

            function loadImage(imageUrl) {
                var image = document.getElementById("cover_image");
                var downloadingImage = new Image();
                downloadingImage.onload = function(){
                    image.src = this.src;
                };
                downloadingImage.src = imageUrl;
            }
            loadImage("${data.image}");
    </script>

</body>

</html>