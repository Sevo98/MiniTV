package com.example.minitv.data.model

import com.google.gson.annotations.SerializedName


data class VideoInfo(
    @SerializedName("VideoId") val videoId: Int,
    @SerializedName("VideoIdentifier") val videoIdentifier: String,
    @SerializedName("OrderNumber") val orderNumber: Int
)