package dev.jhyub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readSymbolicLink

suspend fun syncdb() {
    val base = "${EnvManager.storeAt}/.narutbae/symlinkbase"
    val target = "${EnvManager.storeAt}/.narutbae/${Instant.now().epochSecond}"
    coroutineScope {
        val dbfiles = listOf (
            ".db", ".db.sig", ".files", ".files.sig"
        )
        for(i in dbfiles) {
            launch {
                withContext(Dispatchers.IO) {
                    val client = HttpClient(CIO)
                    client.run {
                        download(
                            "${EnvManager.target}/${EnvManager.repoName}$i",
                            File("$target/${EnvManager.repoName}$i"),
                        )
                    }
                    println("Downloading ${EnvManager.repoName}$i done")
                }
            }
        }
        launch{
            withContext(Dispatchers.IO) {
                Files.createDirectory(Path(target))
                Files.list(Path(base)).forEach {
                    if(it.isSymbolicLink()) {
                        runBlocking(Dispatchers.IO) {
                            Path("$target/${it.fileName}").createSymbolicLinkPointingTo(it.readSymbolicLink())
                        }
                    }
                }
            }
        }
    }
    withContext(Dispatchers.IO) {
        val previous = Path(EnvManager.exposeAt).readSymbolicLink()
        Path("$target/self").createSymbolicLinkPointingTo(Path(target))
        Files.move(
            Path("$target/self"), Path(EnvManager.exposeAt),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        for(i in Files.list(previous)) {
            Files.delete(i)
        }
        Files.delete(previous)
    }
    println("Database sync done at ${Instant.now()}")
}
