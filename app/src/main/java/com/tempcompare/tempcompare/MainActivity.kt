package com.tempcompare.tempcompare

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tempcompare.tempcompare.data.WeatherApi
import com.tempcompare.tempcompare.data.WeatherApi.getWeatherDescription
import com.tempcompare.tempcompare.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // Color constants
    private val gironaColor by lazy { ContextCompat.getColor(this, R.color.girona_color) }
    private val brestColor by lazy { ContextCompat.getColor(this, R.color.brest_color) }
    private val textPrimary by lazy { ContextCompat.getColor(this, R.color.text_primary) }
    private val textSecondary by lazy { ContextCompat.getColor(this, R.color.text_secondary) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSwipeRefresh()
        loadWeatherData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadWeatherData()
        }
        binding.swipeRefresh.setColorSchemeColors(gironaColor, brestColor)
    }

    private fun loadWeatherData() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet))
            binding.swipeRefresh.isRefreshing = false
            return
        }
        
        showLoading(true)
        hideError()
        
        executor.execute {
            val gironaResult = WeatherApi.fetchGirona()
            val brestResult = WeatherApi.fetchBrest()
            
            handler.post {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
                
                if (gironaResult != null && brestResult != null) {
                    updateUI(gironaResult, brestResult)
                } else {
                    showError(getString(R.string.error_loading))
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private fun updateUI(girona: WeatherApi.WeatherResult, brest: WeatherApi.WeatherResult) {
        val tempDiff = girona.temperature - brest.temperature
        val warmerCity = if (tempDiff > 0) "Girona" else if (tempDiff < 0) "Brest" else "Same"
        val diffText = String.format(Locale.getDefault(), "%.1f°C", abs(tempDiff))
        
        // Update comparison header
        binding.tvComparisonHeader.text = getString(R.string.comparison_format, warmerCity, diffText)
        binding.tvComparisonHeader.setTextColor(if (tempDiff > 0) gironaColor else if (tempDiff < 0) brestColor else textSecondary)
        
        // Girona UI
        binding.tvGironaName.setTextColor(gironaColor)
        binding.tvGironaTemp.text = String.format(Locale.getDefault(), "%.1f°C", girona.temperature)
        binding.tvGironaFeelsLike.text = getString(R.string.feels_like_format, girona.temperature)
        binding.tvGironaHumidity.text = "${getHumidityEstimate(girona.weatherCode)}%"
        binding.tvGironaWind.text = String.format(Locale.getDefault(), "%.0f km/h", girona.windSpeed)
        
        // Brest UI
        binding.tvBrestName.setTextColor(brestColor)
        binding.tvBrestTemp.text = String.format(Locale.getDefault(), "%.1f°C", brest.temperature)
        binding.tvBrestFeelsLike.text = getString(R.string.feels_like_format, brest.temperature)
        binding.tvBrestHumidity.text = "${getHumidityEstimate(brest.weatherCode)}%"
        binding.tvBrestWind.text = String.format(Locale.getDefault(), "%.0f km/h", brest.windSpeed)
        
        // Last updated
        val now = Date()
        binding.tvLastUpdated.text = getString(R.string.last_updated_format, dateFormat.format(now))
    }
    
    // Open-Meteo doesn't provide humidity in current_weather, estimate based on weather code
    private fun getHumidityEstimate(code: Int): Int {
        return when (code) {
            0 -> 40  // Clear sky
            1, 2, 3 -> 60  // Partly cloudy
            45, 48 -> 90  // Fog
            51, 53, 55 -> 80  // Drizzle
            56, 57 -> 85  // Freezing drizzle
            61, 63, 65 -> 85  // Rain
            66, 67 -> 90  // Freezing rain
            71, 73, 75 -> 75  // Snow
            77 -> 70  // Snow grains
            80, 81, 82 -> 80  // Rain showers
            85, 86 -> 75  // Snow showers
            95 -> 85  // Thunderstorm
            96, 99 -> 90  // Thunderstorm with hail
            else -> 65
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}