package dev.jhyub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
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
                    "${EnvManager.target}/${EnvManager.repoName}$i.tmp",
                    File("$target/${EnvManager.repoName}$i.tmp"),
                )
                client.close()
                println("Downloading ${EnvManager.repoName}$i.tmp done for target $target")

                val md = MessageDigest.getInstance("MD5")
                val before = Path("$target/${EnvManager.repoName}$i")
                val after = Path("$target/${EnvManager.repoName}$i.tmp")

                if(Files.exists(before)) {
                    val beforeChecksum = DigestInputStream(Files.newInputStream(before), md).readBytes()
                    val afterChecksum = DigestInputStream(Files.newInputStream(after), md).readBytes()

                    if (!beforeChecksum.contentEquals(afterChecksum)) {
                        println("Overwriting ${EnvManager.repoName}$i and removing temporary file")
                        Files.move(after, before, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } else {
                        println("Removing ${EnvManager.repoName}$i.tmp; file is same")
                    }
                    Files.deleteIfExists(after)
                } else {
                    println("Moving ${EnvManager.repoName}$i.tmp to ${EnvManager.repoName}$i")
                    Files.move(after, before, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                }

            }
        }
        launch(Dispatchers.IO) {
            Files.createDirectory(Path(target))
            val baseLs = Files.list(Path(base))
            for (it in baseLs) {
                if (it.isSymbolicLink()) {
                    if (!it.readSymbolicLink().exists())
                        continue
                    Path("$target/${it.fileName}").createSymbolicLinkPointingTo(it.readSymbolicLink())
                }
            }
            baseLs.close()
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
            val previousLs = Files.list(previous)
            for (i in previousLs) {
                Files.delete(i)
            }
            previousLs.close()
            Files.delete(previous)
        } else {
            Files.move(
                Path("$target/self"), Path(EnvManager.exposeAt),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
    println("Database sync done at ${Instant.now()} for target $target")
}
