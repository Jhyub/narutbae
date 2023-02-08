package dev.jhyub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.io.path.*

suspend fun syncdb() {
    val base = "${EnvManager.storeAt}/.narutbae/symlinkbase"
    val target = "${EnvManager.storeAt}/.narutbae/${Instant.now().epochSecond}"
    println("Starting repo sync")
    coroutineScope {
        val dbfiles = listOf(
            ".db", ".db.sig", ".files", ".files.sig"
        )
        for (i in dbfiles) {
            launch(Dispatchers.IO) {
                val client = HttpClient(CIO)
                client.download(
                    "${EnvManager.target}/${EnvManager.repoName}$i",
                    File("$target/${EnvManager.repoName}$i"),
                )
                client.close()
                println("Downloading ${EnvManager.repoName}$i done")
            }
        }
        launch(Dispatchers.IO) {
            Files.createDirectory(Path(target))
            for (it in Files.list(Path(base))) {
                if (it.isSymbolicLink()) {
                    if (!it.readSymbolicLink().exists())
                        continue
                    Path("$target/${it.fileName}").createSymbolicLinkPointingTo(it.readSymbolicLink())
                }
            }
        }
    }

    withContext(Dispatchers.IO) {
        Path("$target/self").createSymbolicLinkPointingTo(Path(target))
        if (Path(EnvManager.exposeAt).isSymbolicLink()) {
            val previous = Path(EnvManager.exposeAt).readSymbolicLink()
            Files.move(
                Path("$target/self"), Path(EnvManager.exposeAt),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            for (i in Files.list(previous)) {
                Files.delete(i)
            }
            Files.delete(previous)
        } else {
            Files.move(
                Path("$target/self"), Path(EnvManager.exposeAt),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
    println("Database sync done at ${Instant.now()}")
}
