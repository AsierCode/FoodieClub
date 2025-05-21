package com.example.foodieclub.data.model

import com.google.gson.annotations.SerializedName

data class UsuarioDto(
    @SerializedName("firebaseUid") val firebaseUid: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("nombreMostrado") val nombreMostrado: String?,
    @SerializedName("fotoUrl") val fotoUrl: String?
)