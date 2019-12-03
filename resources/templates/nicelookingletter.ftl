<#-- @ftlvariable name="data" type="com.example.EpisodeApiInfo" -->
<#--noinspection ALL-->
<#--noinspection HtmlUnknownTarget-->
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Letters</title>
    <script type="text/javascript" src="/chat/helperUtils.js"></script>
    <style>
        html,
        body,
        .grid-container {
            height: 100%;
            margin: 0;
            background-color: #17181b;
        }

        ul.letters {
            /* position: fixed; */
            padding-left: 0;
            box-shadow: 5px 0 14px 0 rgba(0, 0, 0, 0.2);
            margin-top: 0;
            margin-right: 15px;
        }

        a {
            text-decoration: none;
            color: #fff;
            font-family: sans-serif;
        }

        .li-poster {
            margin-right: 10px;
            margin-top: 5px;
            max-width: 130px;
            min-height: 200px;
            display: inline-block;
        }

        .list-item {
            display: block;
            position: relative;
            padding: .25rem .75rem;
            /*   line-height: 1rem; */
            line-height: 24px;
            font-size: .875rem;
            cursor: default;
            background: #17181b;
            /*   box-shadow: inset -1px 0 var(--sidebarBorder); */
        }

        .list-item:nth-child(odd) {
            background-color: #1f2025;
        }

        .grid-container {
            display: grid;
            grid-template-columns: 3fr 7fr;
            grid-template-rows: 1;
            grid-template-areas: "browse shows";
        }

        .browse {
            grid-area: browse;
        }

        .shows {
            grid-area: shows;
        }

        .poster-div > img {
            object-fit: contain;
            margin-left: auto;
            margin-right: auto;
            display: block;
            border-radius: 4px;
        }

        /* .show-title {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .show-anchor {
            font-size: 12px;
            display: inline-block;
            height: 100%;
            width: 100%;
            text-decoration: none;
        } */
        /* p.title {
            font-family: sans-serif;
            height: auto;
            margin: 6px 0 4px;
            color: #fff;
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            padding-bottom: 2px;
            text-align: center;
        } */
        .show-title {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            /*     width: 100%; */
        }

        .show-anchor {
            background-size: contain;
            font-size: 12px;
            display: inline-block;
            height: 100%;
            width: 100%;
            text-decoration: none;
        }

        p.title {
            font-family: sans-serif;
            height: auto;
            margin: 0;
            color: #fff;
            max-width: 100%;
            font-size: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            padding-bottom: 2px;
            text-align: center;
        }

        img.img-fluid.poster-full-width.lazy {
            border-radius: 4px;
        }

        .img-fluid {
            max-width: 100%;
            height: auto;
            border-radius: 4px;
        }

        .poster-full-width {
            width: 100%;
        }

    </style>
</head>

<body>
<!-- <script>
    document.addEventListener("DOMContentLoaded", function() {
        var lazyloadImages;
        if ("IntersectionObserver" in window) {
            lazyloadImages = document.querySelectorAll(".lazy");
            var imageObserver = new IntersectionObserver(function(entries, observer) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) {
                        var image = entry.target;
                        image.src = image.dataset.src;
                        image.classList.remove("lazy");
                        imageObserver.unobserve(image);
                    }
                });
            });
            lazyloadImages.forEach(function(image) {
                imageObserver.observe(image);
            });
        } else {
            var lazyloadThrottleTimeout;
            lazyloadImages = document.querySelectorAll(".lazy");

            function lazyload() {
                if (lazyloadThrottleTimeout) {
                    clearTimeout(lazyloadThrottleTimeout);
                }
                lazyloadThrottleTimeout = setTimeout(function() {
                    var scrollTop = window.pageYOffset;
                    lazyloadImages.forEach(function(img) {
                        if (img.offsetTop < (window.innerHeight + scrollTop)) {
                            img.src = img.dataset.src;
                            img.classList.remove('lazy');
                        }
                    });
                    if (lazyloadImages.length == 0) {
                        document.removeEventListener("scroll", lazyload);
                        window.removeEventListener("resize", lazyload);
                        window.removeEventListener("orientationChange", lazyload);
                    }
                }, 20);
            }
            document.addEventListener("scroll", lazyload);
            window.addEventListener("resize", lazyload);
            window.addEventListener("orientationChange", lazyload);
        }
    })

</script> -->
<div class="grid-container">
    <div class="browse">
        <ul class="letters">
            <li class="list-item"><a href="/shows/0-9">0-9</a></li>
            <li class="list-item"><a href="/shows/A">A</a></li>
            <li class="list-item"><a href="/shows/B">B</a></li>
            <li class="list-item"><a href="/shows/C">C</a></li>
            <li class="list-item"><a href="/shows/D">D</a></li>
            <li class="list-item"><a href="/shows/E">E</a></li>
            <li class="list-item"><a href="/shows/F">F</a></li>
            <li class="list-item"><a href="/shows/G">G</a></li>
            <li class="list-item"><a href="/shows/H">H</a></li>
            <li class="list-item"><a href="/shows/I">I</a></li>
            <li class="list-item"><a href="/shows/J">J</a></li>
            <li class="list-item"><a href="/shows/K">K</a></li>
            <li class="list-item"><a href="/shows/L">L</a></li>
            <li class="list-item"><a href="/shows/M">M</a></li>
            <li class="list-item"><a href="/shows/N">N</a></li>
            <li class="list-item"><a href="/shows/O">O</a></li>
            <li class="list-item"><a href="/shows/P">P</a></li>
            <li class="list-item"><a href="/shows/Q">Q</a></li>
            <li class="list-item"><a href="/shows/R">R</a></li>
            <li class="list-item"><a href="/shows/S">S</a></li>
            <li class="list-item"><a href="/shows/T">T</a></li>
            <li class="list-item"><a href="/shows/U">U</a></li>
            <li class="list-item"><a href="/shows/V">V</a></li>
            <li class="list-item"><a href="/shows/W">W</a></li>
            <li class="list-item"><a href="/shows/X">X</a></li>
            <li class="list-item"><a href="/shows/Y">Y</a></li>
            <li class="list-item"><a href="/shows/Z">Z</a></li>
        </ul>
    </div>
    <div class="shows">
        <#list data as item>
            <li class="li-poster">
                <div class="poster-div">
                    <a href="#" onclick="openEpisode('${item.url}')" class="thumb">
                        <img src="${item.image}" alt="" class="img-fluid poster-full-width lazy">
                        <span class="show-title"><p class="title">${item.name}</p></span>
                    </a>
                </div>
            </li>
        </#list>
    </div>
</div>
</body>

</html>