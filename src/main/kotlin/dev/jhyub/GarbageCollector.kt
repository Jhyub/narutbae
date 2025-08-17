package dev.jhyub

import dev.jhyub.EnvManager.target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readSymbolicLink

object GarbageCollector {
    val path = EnvManager.storeAt + "/.narutbae/access"
    var map = mutableMapOf<String, Long>()

    suspend fun read() {
        withContext(Dispatchers.IO) {
            val f = File(path)
            if (!f.exists()) return@withContext
            val fis = FileInputStream(f)
            val br = BufferedReader(InputStreamReader(fis))
            map = Json.decodeFromString(br.readLine())
            br.close()
            fis.close()
        }
    }

    suspend fun write() {
        withContext(Dispatchers.IO) {
            val f = File(path)
            val fos = FileOutputStream(f)
            fos.write(Json.encodeToString(map).toByteArray())
            fos.close()
        }
    }

    suspend fun update(name: String) {
        map[name] = Instant.now().toEpochMilli()
        write()
    }

    suspend fun job() {
        val now = Instant.now()
        coroutineScope {
            for ((k, v) in map) {
                println("GarbageCollector looking at $k")
                if (k in listOf(
                        "${EnvManager.repoName}.db", "${EnvManager.repoName}.db.sig",
                        "${EnvManager.repoName}.files", "${EnvManager.repoName}.files.sig"
                    ) || !Path("${EnvManager.storeAt}/$k").exists()
                )
                    continue
                if (Duration.between(Instant.ofEpochMilli(v), now).toDays() >= EnvManager.gcDays) {
                    launch(Dispatchers.IO) {
                        println("Deleting $k")
                        if(Path(EnvManager.exposeAt).isSymbolicLink()) {
                            println("Looking for expose symlink... ${Path(EnvManager.exposeAt).readSymbolicLink()}")
                            Files.deleteIfExists(Path("${Path(EnvManager.exposeAt).readSymbolicLink()}/$k"))
                        }
                        println("Deleted $k symlink at expose")
                        Files.deleteIfExists(Path("${EnvManager.storeAt}/.narutbae/symlinkbase/$k"))
                        println("Deleted $k symlink at symlinkbase")
                        Files.deleteIfExists(Path("${EnvManager.storeAt}/$k"))
                        println("Deleted $k file at store")
                        println("Deleting $k done")
                    }
                }
            }
            syncdb()
        }
    }
}