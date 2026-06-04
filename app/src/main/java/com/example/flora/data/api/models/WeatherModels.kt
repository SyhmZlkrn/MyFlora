package com.example.flora.data.api.models

import com.google.gson.annotations.SerializedName

// OpenWeatherMap API response models
data class WeatherResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherDescription>,
    val wind: WindData
)

data class MainData(
    val temp: Double,
    val humidity: Int,
    @SerializedName("feels_like") val feelsLike: Double
)

data class WeatherDescription(
    val main: String,
    val description: String,
    val icon: String
)

data class WindData(
    val speed: Double
)

// Domain model for UI consumption
data class SimpleWeatherData(
    val cityName: String = "Loading...",
    val temperature: Double = 0.0,
    val description: String = "",
    val humidity: Int = 0,
    val uvIndex: Double = 0.0,
    val feelsLike: Double = 0.0,
    val windSpeed: Double = 0.0,
    val icon: String = "",
    val isRaining: Boolean = false,
    val weatherMain: String = ""
)
