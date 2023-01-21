package dev.jhyub

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

// Borrowed from ktor examples
// Licensed under APACHE License
// https://github.com/ktorio/ktor-documentation/blob/2.2.2/codeSnippets/snippets/client-download-file-range/src/main/kotlin/com/example/Downloader.kt

suspend fun HttpClient.download(url: String, at: File, chunkSize: Int = 1024*1024) {
    val length = head(url).headers[HttpHeaders.ContentLength]?.toLong() as Long
    val lastByte = length - 1

    var start = at.length()
    val output = withContext(Dispatchers.IO) {
        FileOutputStream(at, false)
    }

    while (true) {
        val end = min(start + chunkSize - 1, lastByte)
        val data = get(url) {
            header("Range", "bytes-$start-$end")
        }.body<ByteArray>()
        withContext(Dispatchers.IO) {
            output.write(data)
        }
        if(end >= lastByte) break
        start += chunkSize
    }
}