package com.example.it3c_grp11_andrei

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val name: String
)

data class Weather(
    val main: String,
    val description: String
)

data class Main(
    val temp: Double,
    val humidity: Int
)
