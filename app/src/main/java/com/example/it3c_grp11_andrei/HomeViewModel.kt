package com.example.it3c_grp11_andrei

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HomeViewModel : ViewModel() {

    var temp: Double? = null
    var newsList: List<String> = emptyList()

    fun fetchWeatherData(onResult: (Double?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.openweathermap.org/data/2.5/weather?q=Manila&appid=b54fa4005b8a8ceb91779739b4f996f1&units=metric")
                        .build()
                    val response = client.newCall(request).execute()
                    response.use {
                        val json = it.body?.string()
                        json?.let { body ->
                            JSONObject(body).getJSONObject("main").getDouble("temp")
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
            withContext(Dispatchers.Main) {
                temp = result
                onResult(result)
            }
        }
    }

    fun fetchNews(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://newsapi.org/v2/everything?q=agriculture OR fishery&language=en&apiKey=34e06c3101b643a2ae16ce894a70604c")
                        .build()
                    val response = client.newCall(request).execute()
                    val json = response.body?.string()
                    val list = mutableListOf<String>()
                    json?.let { body ->
                        val articles = JSONObject(body).getJSONArray("articles")
                        for (i in 0 until articles.length()) {
                            list.add(articles.getJSONObject(i).getString("title"))
                        }
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }
            withContext(Dispatchers.Main) {
                newsList = result
                onResult(result)
            }
        }
    }
}
