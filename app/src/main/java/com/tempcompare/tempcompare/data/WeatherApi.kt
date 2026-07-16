package com.tempcompare.tempcompare.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object WeatherApi {
    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val GIRONA_LAT = 41.9831
    private const val GIRONA_LON = 2.8249
    private const val BREST_LAT = 48.3904
    private const val BREST_LON = -4.4861
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()
    
    private val gson = GsonBuilder().create()
    
    data class OpenMeteoResponse(
        val current_weather: CurrentWeather,
        val timezone: String
    )
    
    data class CurrentWeather(
        val temperature: Double,
        val windspeed: Double,
        val winddirection: Double,
        val weathercode: Int,
        val time: String
    )
    
    data class WeatherResult(
        val cityName: String,
        val country: String,
        val temperature: Double,
        val windSpeed: Double,
        val windDirection: Double,
        val weatherCode: Int,
        val lastUpdated: String,
        val timezone: String
    )
    
    fun fetchWeatherForCity(lat: Double, lon: Double, cityName: String, country: String): WeatherResult? {
        val url = "$BASE_URL?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto"
        val request = Request.Builder().url(url).build()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }
                val body = response.body?.string() ?: return null
                val meteoResponse = gson.fromJson(body, OpenMeteoResponse::class.java)
                
                return WeatherResult(
                    cityName = cityName,
                    country = country,
                    temperature = meteoResponse.current_weather.temperature,
                    windSpeed = meteoResponse.current_weather.windspeed,
                    windDirection = meteoResponse.current_weather.winddirection,
                    weatherCode = meteoResponse.current_weather.weathercode,
                    lastUpdated = meteoResponse.current_weather.time,
                    timezone = meteoResponse.timezone
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    fun fetchGirona(): WeatherResult? = fetchWeatherForCity(GIRONA_LAT, GIRONA_LON, "Girona", "ES")
    fun fetchBrest(): WeatherResult? = fetchWeatherForCity(BREST_LAT, BREST_LON, "Brest", "FR")
    
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear, partly cloudy, overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow fall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}