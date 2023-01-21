package dev.jhyub

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.math.min

fun main(): Unit = runBlocking {
    EnvManager
    launch(Dispatchers.Unconfined) {
        while(true) {
            launch { syncdb() }
            delay(Duration.ofMinutes(10L))
        }
    }
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/{fileName}") {
            val fileName = call.parameters["fileName"]
            fileName?.let {
                launch {
                    val client = HttpClient(CIO)
                    client.download("${EnvManager.target}/$it", File("${EnvManager.storeAt}/$it"))
                    Path("${EnvManager.storeAt}/.narutbae/symlinkbase/$it")
                        .createSymbolicLinkPointingTo(Path("${EnvManager.storeAt}/$it"))
                }
                call.respondRedirect("${EnvManager.target}/$it")
            }
        }
    }
}