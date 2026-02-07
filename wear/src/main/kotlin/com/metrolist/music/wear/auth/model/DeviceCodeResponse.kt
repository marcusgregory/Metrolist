package com.metrolist.music.wear.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from the device code request.
 */
@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code")
    val deviceCode: String,

    @SerialName("user_code")
    val userCode: String,

    @SerialName("verification_url")
    val verificationUrl: String,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("interval")
    val interval: Int
)
