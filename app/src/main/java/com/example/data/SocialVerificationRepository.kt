package com.example.data

import com.example.data.api.ApiClient
import com.example.data.api.SocialVerificationRequest

sealed class SocialVerificationResult {
    data class Submitted(val message: String) : SocialVerificationResult()
    data class Approved(val provider: String?) : SocialVerificationResult()
    data class Pending(val message: String) : SocialVerificationResult()
    data class Error(val message: String) : SocialVerificationResult()
    data object Offline : SocialVerificationResult()
}

class SocialVerificationRepository {

    suspend fun requestVerification(
        userId: String,
        provider: String,
        profileUrl: String
    ): SocialVerificationResult {
        if (!ApiClient.isApiConfigured()) {
            return SocialVerificationResult.Offline
        }
        return try {
            val response = ApiClient.barterApi.requestSocialVerification(
                SocialVerificationRequest(
                    userId = userId,
                    provider = provider,
                    profileUrl = profileUrl
                )
            )
            if (response.isSuccessful) {
                val body = response.body()
                when (body?.status?.uppercase()) {
                    "APPROVED", "VERIFIED" -> SocialVerificationResult.Approved(provider)
                    else -> SocialVerificationResult.Submitted(
                        body?.message ?: "Verification request submitted for review."
                    )
                }
            } else {
                SocialVerificationResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            SocialVerificationResult.Error(e.message ?: "Could not reach verification service")
        }
    }

    suspend fun fetchStatus(userId: String): SocialVerificationResult {
        if (!ApiClient.isApiConfigured()) {
            return SocialVerificationResult.Offline
        }
        return try {
            val response = ApiClient.barterApi.getSocialVerificationStatus(userId)
            if (!response.isSuccessful) {
                return SocialVerificationResult.Error("Status check failed (${response.code()})")
            }
            when (response.body()?.status?.uppercase()) {
                "APPROVED", "VERIFIED" -> SocialVerificationResult.Approved(response.body()?.provider)
                "PENDING", "REVIEW" -> SocialVerificationResult.Pending(
                    "Your social profile is being reviewed by our team."
                )
                else -> SocialVerificationResult.Pending("Verification status unavailable.")
            }
        } catch (e: Exception) {
            SocialVerificationResult.Error(e.message ?: "Could not check verification status")
        }
    }
}
