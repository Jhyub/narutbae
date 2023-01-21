package dev.jhyub

import java.nio.file.Path

object EnvManager {
    val target: String = System.getenv("NARUTBAE_TARGET")
    val exposeAt: String = System.getenv("NARUTBAE_EXPOSE_AT")
    val storeAt: String = System.getenv("NARUTBAE_STORE_AT")
    val repoName: String = System.getenv("NARUTBAE_REPO_NAME")
}