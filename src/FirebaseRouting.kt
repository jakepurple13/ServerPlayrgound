package com.example

import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.route

fun Route.firebaseRoute() {
    route("/userinfo") {
        static {
            // This marks index.html from the 'web' folder in resources as the default file to serve.
            defaultResource("firebase.html", "web")
            // This serves files from the 'web' folder in the application resources.
            resources("web")
        }
    }
}