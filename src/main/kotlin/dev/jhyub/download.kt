package dev.jhyub

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

// Borrowed from ktor examples
// Licensed under APACHE License
// https://github.com/ktorio/ktor-documentation/blob/2.2.2/codeSnippets/snippets/client-download-file-range/src/main/kotlin/com/example/Downloader.kt

suspend fun HttpClient.download(url: String, at: File, chunkSize: Int = 1024 * 1024) {
    var head: HttpResponse? = null
    try {
        head = head(url)
    } catch (cte: ConnectTimeoutException) {
        println("Caught cte @head@download, will retry after 2 secs")
        delay(2000L)
        download(url, at, chunkSize)
    } catch (hrte: HttpRequestTimeoutException) {
        println("Caught hrte @head@download, will retry after 2 secs")
        delay(2000L)
        download(url, at, chunkSize)
    }
    if(head?.status?.isSuccess() != true) return
    val length = head.headers[HttpHeaders.ContentLength]?.toLong() as Long
    val lastByte = length - 1

    var start = at.length()
    withContext(Dispatchers.IO) {
        val output = FileOutputStream(at, false)
        while (true) {
            val end = min(start + chunkSize - 1, lastByte)
            println("bytes=$start-$end")
            try {
                withContext(Dispatchers.IO) {
                    val data = get(url) {
                        header("Range", "bytes=$start-$end")
                    }.body<ByteArray>()
                    output.write(data)
                }
            } catch (cte: ConnectTimeoutException) {
                println("Caught cte @get@ddownload, will retry after 2 secs")
                start -= chunkSize
                delay(2000L)
            } catch (hrte: HttpRequestTimeoutException) {
                println("Caught hrte @get@ddownload, will retry after 2 secs")
                start -= chunkSize
                delay(2000L)
            }
            if(end >= lastByte) break
            start += chunkSize
        }
        output.close()
    }
}