<#-- @ftlvariable name="data" type="com.example.ShowInfo" -->
<html>

    <body>

        <form method="post">

            <select name="show_type" id="show_type">
                <option value="all">All</option>
                <option value="ls">Live Action</option>
                <option value="as">Anime</option>
                <option value="cs">Cartoon</option>
            </select>

            <input type="text" name="show_name" id="show_name" placeholder="Enter Show Name">

            <button type="submit">Submit</button>

        </form>

        <ol>
        <#list data as item>
            <li>${item}</li>
        </#list>
        </ol>
    </body>

</html>