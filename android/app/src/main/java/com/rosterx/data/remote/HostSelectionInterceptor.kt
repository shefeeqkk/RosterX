package com.rosterx.data.remote

import com.rosterx.data.local.UserSessionManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val sessionManager: UserSessionManager
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val customBaseUrl = sessionManager.getBaseUrl()

        if (customBaseUrl.isNotBlank()) {
            val newHttpUrl: HttpUrl? = customBaseUrl.toHttpUrlOrNull()
            if (newHttpUrl != null) {
                val updatedUrl = request.url.newBuilder()
                    .scheme(newHttpUrl.scheme)
                    .host(newHttpUrl.host)
                    .port(newHttpUrl.port)
                    .build()
                request = request.newBuilder().url(updatedUrl).build()
            }
        }
        return chain.proceed(request)
    }
}
