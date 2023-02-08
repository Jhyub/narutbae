package dev.jhyub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.io.File
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.exists

fun main(): Unit = runBlocking {
    EnvManager
    launch(Dispatchers.Unconfined) {
        while (true) {
            launch { syncdb() }
            delay(Duration.ofMinutes(EnvManager.syncInterval))
        }
    }
    launch(Dispatchers.Unconfined) {
        kotlinx.coroutines.delay(10L)
        GarbageCollector.read()
        while (true) {
            launch { GarbageCollector.job() }
            delay(Duration.ofMinutes(EnvManager.gcInterval))
        }
    }
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/${EnvManager.repoName}/{fileName}") {
            val fileName = call.parameters["fileName"]
            val localFilePath = "${EnvManager.exposeAt}/$fileName"
            fileName?.let {
                if (Path(localFilePath).exists()) {
                    call.respondRedirect("/${EnvManager.repoName}/downloads/$fileName")
                } else {
                    if (!Path("${EnvManager.storeAt}/$fileName").exists()) {
                        launch {
                            val client = HttpClient(CIO)
                            try {
                                client.download("${EnvManager.target}$it", File("${EnvManager.storeAt}/$it"))
                                client.download("${EnvManager.target}$it.sig", File("${EnvManager.storeAt}/$it.sig"))
                                if (Path("${EnvManager.storeAt}/$it").exists())
                                    Path("${EnvManager.storeAt}/.narutbae/symlinkbase/$it")
                                        .createSymbolicLinkPointingTo(Path("${EnvManager.storeAt}/$it"))
                                if (Path("${EnvManager.storeAt}/$it.sig").exists())
                                    Path("${EnvManager.storeAt}/.narutbae/symlinkbase/$it.sig")
                                        .createSymbolicLinkPointingTo(Path("${EnvManager.storeAt}/$it.sig"))
                            } catch (_: Exception) {
                            }
                            client.close()
                        }
                    }
                    call.respondRedirect("${EnvManager.target}$it")
                }
                GarbageCollector.update(it)
            }
        }
    }
}