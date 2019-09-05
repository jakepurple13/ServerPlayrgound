<#-- @ftlvariable name="data" type="com.example.ShowInfo" -->
<!doctype html>
<html lang="en">
  <head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Hello, Bootstrap Table!</title>

    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css" integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS" crossorigin="anonymous">
    <link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.6.3/css/all.css" integrity="sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/" crossorigin="anonymous">
    <link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.15.4/dist/bootstrap-table.min.css">
  </head>
  <style>
    .select,
    #locale {
      width: 100%;
    }
    .like {
      margin-right: 10px;
    }
  </style>

  <div class="select">
    <select class="form-control" id="locale">
      <option disabled selected>Select</option>
      <option value='animetoon'>Cartoon</option>
      <option value='putlocker'>Putlocker</option>
      <option value='gogoanime'>Gogoanime</option>
      <option value='all'>All</option>
    </select>
  </div>
  <body>
  <div id="toolbar">
  </div>
    <table data-toggle="table"
    id="table"
    data-toolbar="#toolbar"
      data-search="true"
      data-click-to-select="true"
      data-show-pagination-switch="true"
        data-pagination="true"
      data-page-list="[10, 25, 50, 100, all]"
      data-show-refresh="true">
      <thead>
        <tr>
          <th data-field="id" data-sortable="true">#</th>
          <th>Name</th>
          <th>URL</th>
        </tr>
      </thead>
      <tbody>
        <#list 0..data?size-1 as i>
                    <tr>
                        <td>${i}</td>
                        <td>${data[i].name}</td>
                        <td>${data[i].url}</td>
                    </tr>
                </#list>
      </tbody>
    </table>

    <script src="https://code.jquery.com/jquery-3.3.1.min.js" integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js" integrity="sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut" crossorigin="anonymous"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js" integrity="sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k" crossorigin="anonymous"></script>
    <script src="https://unpkg.com/bootstrap-table@1.15.4/dist/bootstrap-table.min.js"></script>
  </body>
  <script>
    $('#table').bootstrapTable({
      onClickRow: function (row, element, field) {
        // ...
        var type = "";
        console.log(element[0]);
        var u = element[0].cells[2].innerHTML;

        if(u.includes("gogoanime")) {
            type = "g";
        } else if(u.includes("putlocker")) {
            type = "p";
        } else if(u.includes("animetoon")) {
            type = "a";
        }
        var location = u.split("/");
        var locate = location.filter(x => x != "").slice(-1)[0];
        window.open("/nsi/" + type + locate, '_blank');
      }
    })
  </script>
</html>