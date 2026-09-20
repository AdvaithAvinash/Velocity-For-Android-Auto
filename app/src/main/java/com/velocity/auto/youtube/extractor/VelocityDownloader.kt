package com.velocity.auto.youtube.extractor

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response as ExtractorResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * The HTTP plumbing NewPipeExtractor needs but doesn't ship - it just hands
 * us a generic [ExtractorRequest]/[ExtractorResponse] pair and we're free to
 * fetch it however we like, so we use plain OkHttp instead of pulling in a
 * second heavier client.
 */
class VelocityDownloader private constructor() : Downloader() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: ExtractorRequest): ExtractorResponse {
        val dataToSend = request.dataToSend()
        val body = dataToSend?.toRequestBody(null)

        val builder = OkHttpRequest.Builder()
            .method(request.httpMethod(), body)
            .url(request.url())
            .header("User-Agent", USER_AGENT)

        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { value -> builder.addHeader(name, value) }
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCAPTCHA challenge requested", request.url())
            }
            return ExtractorResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString()
            )
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        val instance: VelocityDownloader by lazy { VelocityDownloader() }
    }
}
