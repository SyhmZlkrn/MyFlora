package com.example.flora.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.api.WeatherRepository
import com.example.flora.data.api.models.SimpleWeatherData
import com.example.flora.data.location.LocationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository()
    private val locationProvider = LocationProvider(application)

    private val _weather = MutableStateFlow(SimpleWeatherData())
    val weather: StateFlow<SimpleWeatherData> = _weather.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Update clock every second
        viewModelScope.launch {
            while (true) {
                val now = Date()
                _currentTime.value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
                _currentDate.value = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(now)
                delay(1000)
            }
        }
    }

    fun fetchWeather() {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val location = locationProvider.getLastLocation()
                repository.getWeather(location.latitude, location.longitude)
                    .onSuccess { _weather.value = it }
                    .onFailure {
                        // Keep default/last values, set description to error hint
                        if (_weather.value.cityName == "Loading...") {
                            _weather.value = SimpleWeatherData(
                                cityName = "Kuala Lumpur",
                                temperature = 29.0,
                                description = "Weather unavailable",
                                humidity = 80
                            )
                        }
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getWateringAdvice(): String {
        val temp = _weather.value.temperature
        val humidity = _weather.value.humidity
        return when {
            temp > 35 -> "Hot day! Water plants extra."
            humidity > 85 -> "High humidity. Skip watering."
            temp > 28 -> "Good watering day!"
            else -> "Normal watering schedule."
        }
    }
}
