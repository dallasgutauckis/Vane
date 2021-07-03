package com.dallasgutauckis.vane.api.twitch

import android.util.Log
import arrow.core.Either
import arrow.core.left
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class TwitchApi(clientId: String) {

    private val retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .addNetworkInterceptor {
                    it.proceed(
                        it.request().newBuilder()
                            .addHeader("Client-Id", clientId)
                            .build()
                    )
                }
                .addInterceptor(HttpLoggingInterceptor { Log.v("TwitchApi", it) }.apply { level = HttpLoggingInterceptor.Level.BODY })
                .build()
        )
        .baseUrl("https://api.twitch.tv/helix/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val streamService by lazy { retrofit.create(StreamService::class.java) }

    fun getUsers(authToken: String, users: List<String>): Either<Exception, PaginatedResponse<List<Stream>>> {
        return streamService.getStreams("Bearer $authToken", users)
            .execute()
            .let {
                Either.conditionally(it.isSuccessful, { Exception() }, { it.body()!! })
            }
    }

    interface StreamService {
        @GET("streams?first=20")
        fun getStreams(@Header("Authorization") authToken: String, @Query("user_login") logins: List<String>): Call<PaginatedResponse<List<Stream>>>
    }

    @JsonClass(generateAdapter = true)
    data class PaginatedResponse<T>(
        @Json(name = "data") val data: T,
        @Json(name = "pagination") val pagination: Pagination?
    ) {
        @JsonClass(generateAdapter = true)
        data class Pagination(
            @Json(name = "cursor") val cursor: String?
        )
    }

    @JsonClass(generateAdapter = true)
    data class Stream(
        @Json(name = "id") val id: String,
        @Json(name = "user_id") val user_id: String,
        @Json(name = "user_login") val user_login: String,
        @Json(name = "user_name") val user_name: String,
        @Json(name = "game_id") val game_id: String,
        @Json(name = "game_name") val game_name: String,
        // "live" or "" in case of error
        @Json(name = "type") val type: String,
        @Json(name = "title") val title: String,
        @Json(name = "viewer_count") val viewer_count: Int,
        @Json(name = "started_at") val started_at: String,
        @Json(name = "thumbnail_url") val thumbnail_url: String,
        @Json(name = "tag_ids") val tag_ids: List<String>,
        @Json(name = "is_mature") val is_mature: Boolean,
    )
}
