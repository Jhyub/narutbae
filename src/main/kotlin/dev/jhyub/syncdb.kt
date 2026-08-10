package dev.jhyub

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.io.path.*

private fun deleteSnapshot(dir: Path) {
    Files.list(dir).use { entries -> entries.forEach { Files.delete(it) } }
    Files.delete(dir)
}

suspend fun syncdb() {
    val base = "${EnvManager.storeAt}/.narutbae/symlinkbase"
    val target = "${EnvManager.storeAt}/.narutbae/${Instant.now().epochSecond}"
    println("Starting repo sync")

    withContext(Dispatchers.IO) {
        Files.createDirectories(Path(target))
    }

    coroutineScope {
        val dbfiles = listOf(
            ".db", ".db.sig", ".files", ".files.sig"
        )
        for (i in dbfiles) {
            launch(Dispatchers.IO) {
                val name = "${EnvManager.repoName}$i"
                val before = Path("$target/$name")
                val after = Path("$target/$name.tmp")
                val live = Path("${EnvManager.exposeAt}/$name")

                // Carry the currently live file into the new base, so an unchanged
                // (or undownloadable) file still ends up in the snapshot
                if (Files.exists(live))
                    Files.copy(
                        live, before,
                        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING
                    )

                HttpClient(CIO).use { client ->
                    client.download("${EnvManager.target}$name", after.toFile())
                }

                // download() returns silently on a non-2xx response, leaving no file behind
                if (!Files.exists(after) || Files.size(after) == 0L) {
                    Files.deleteIfExists(after)
                    if (Files.exists(before))
                        println("Downloading $name failed, keeping the currently live copy")
                    else
                        println("Downloading $name failed and no live copy exists, skipping")
                    return@launch
                }
                println("Downloading $name.tmp done for target $target")

                if (!Files.exists(before)) {
                    println("Moving $name.tmp to $name")
                    Files.move(after, before, StandardCopyOption.ATOMIC_MOVE)
                } else if (Files.mismatch(before, after) != -1L) {
                    println("Overwriting $name and removing temporary file")
                    Files.move(
                        after, before,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                    )
                } else {
                    println("Removing $name.tmp; file is same")
                    Files.deleteIfExists(after)
                }
            }
        }
        launch(Dispatchers.IO) {
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

    if (!Files.exists(Path("$target/${EnvManager.repoName}.db"))) {
        println("Sync aborted: ${EnvManager.repoName}.db is unavailable, keeping the current snapshot")
        withContext(Dispatchers.IO) { deleteSnapshot(Path(target)) }
        return
    }

    withContext(Dispatchers.IO) {
        Path("$target/self").createSymbolicLinkPointingTo(Path(target))
        if (Path(EnvManager.exposeAt).isSymbolicLink()) {
            val previous = Path(EnvManager.exposeAt).readSymbolicLink()
            Files.move(
                Path("$target/self"), Path(EnvManager.exposeAt),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            deleteSnapshot(previous)
        } else {
            // exposeAt is still the plain directory of the initial state; ATOMIC_MOVE cannot
            // replace a directory (rename(2) fails with EISDIR), so let the JDK remove it first
            Files.move(
                Path("$target/self"), Path(EnvManager.exposeAt),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
    println("Database sync done at ${Instant.now()} for target $target")
}
