package com.example.flora.data.api

import com.example.flora.BuildConfig
import com.example.flora.data.api.models.SimpleWeatherData

class WeatherRepository(private val api: WeatherApiService = WeatherApiService.create()) {

    suspend fun getWeather(lat: Double, lon: Double): Result<SimpleWeatherData> {
        return try {
            val response = api.getCurrentWeather(lat, lon, BuildConfig.OPENWEATHER_API_KEY)
            val weather = response.weather.firstOrNull()
            val main = weather?.main ?: ""
            val desc = weather?.description ?: ""
            val rainKeywords = listOf("rain", "drizzle", "thunderstorm", "shower")
            val isRaining = rainKeywords.any { kw ->
                main.contains(kw, ignoreCase = true) || desc.contains(kw, ignoreCase = true)
            }
            Result.success(
                SimpleWeatherData(
                    cityName = response.name,
                    temperature = response.main.temp,
                    description = desc.replaceFirstChar { it.uppercase() },
                    humidity = response.main.humidity,
                    feelsLike = response.main.feelsLike,
                    windSpeed = response.wind.speed,
                    icon = weather?.icon ?: "",
                    isRaining = isRaining,
                    weatherMain = main
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
