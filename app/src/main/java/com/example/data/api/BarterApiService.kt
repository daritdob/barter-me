package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class SocialVerificationRequest(
    val userId: String,
    val provider: String,
    val profileUrl: String
)

@JsonClass(generateAdapter = true)
data class SocialVerificationResponse(
    val status: String,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SocialVerificationStatus(
    @Json(name = "user_id") val userId: String,
    val status: String,
    val provider: String? = null
)

interface BarterApiService {
    @POST("verification/social")
    suspend fun requestSocialVerification(
        @Body request: SocialVerificationRequest
    ): Response<SocialVerificationResponse>

    @GET("verification/social/{userId}")
    suspend fun getSocialVerificationStatus(
        @Path("userId") userId: String
    ): Response<SocialVerificationStatus>
}
