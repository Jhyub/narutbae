package dev.jhyub

import java.nio.file.Path

object EnvManager {
	val host: String = System.getenv("NARUTBAE_HOST")
	val port: Int = System.getenv("NARUTBAE_PORT").toInt()
    val target: String = System.getenv("NARUTBAE_TARGET")
    val exposeAt: String = System.getenv("NARUTBAE_EXPOSE_AT")
    val storeAt: String = System.getenv("NARUTBAE_STORE_AT")
    val repoName: String = System.getenv("NARUTBAE_REPO_NAME")
    val syncInterval: Long = System.getenv("NARUTBAE_SYNC_INTERVAL").toLong()
    val gcInterval: Long = System.getenv("NARUTBAE_GC_INTERVAL").toLong()
    val gcDays: Long = System.getenv("NARUTBAE_GC_DAYS").toLong()
}
