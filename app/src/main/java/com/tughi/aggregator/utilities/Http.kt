package com.tughi.aggregator.utilities

import com.tughi.aggregator.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

object Http {

    val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(UserAgentInterceptor())
            .build()

    class UserAgentInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                    .header("User-Agent", "Aggregator/${BuildConfig.VERSION_NAME}")
                    .build()
            return chain.proceed(request)
        }
    }

}