package com.example.it3c_grp11_andrei

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.it3c_grp11_andrei.ui.theme.IT3C_Grp11_ANDREITheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HomeActivity : ComponentActivity() {

    private val weatherApiKey = "b54fa4005b8a8ceb91779739b4f996f1"
    private val weatherUrl =
        "https://api.openweathermap.org/data/2.5/weather?q=Manila&appid=$weatherApiKey&units=metric"

    private val newsApiKey = "34e06c3101b643a2ae16ce894a70604c"
    private val newsUrl =
        "https://newsapi.org/v2/everything?q=agriculture OR fishery&language=en&apiKey=$newsApiKey"

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // ✅ Load dark mode preference locally
        AppThemeState.loadTheme(applicationContext)

        // ✅ Also fetch latest theme preference from Firestore
        val userId = auth.currentUser?.uid
        val firestore = FirebaseFirestore.getInstance()

        userId?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val themePref = doc.getString("theme")
                    val darkMode = themePref == "dark"
                    AppThemeState.isDarkMode.value = darkMode
                    AppThemeState.saveTheme(applicationContext, darkMode)
                }
        }

        setContent {
            IT3C_Grp11_ANDREITheme(darkTheme = AppThemeState.isDarkMode.value) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }

    @Composable
    fun HomeScreen() {
        val context = LocalContext.current
        val userEmail = auth.currentUser?.email ?: "Farmer"

        var temp by remember { mutableStateOf<Double?>(null) }
        var isWeatherLoading by remember { mutableStateOf(true) }

        var newsList by remember { mutableStateOf<List<String>>(emptyList()) }
        var isNewsLoading by remember { mutableStateOf(true) }

        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            fetchWeatherData {
                temp = it
                isWeatherLoading = false
            }
            fetchNews {
                newsList = it
                isNewsLoading = false
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg_agri),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xAAFFFFFF), Color(0xAAFFFFFF))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome, $userEmail!", style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌤️ Weather Update", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isWeatherLoading) {
                            Text("Loading weather...")
                        } else {
                            Text("Temperature in Manila: ${temp ?: "N/A"}°C")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📰 Agri & Fishery News", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isNewsLoading) {
                            Text("Loading news...")
                        } else if (newsList.isEmpty()) {
                            Text("No news available.")
                        } else {
                            newsList.take(5).forEach { title ->
                                Text("• $title", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)

                Button(onClick = {
                    context.startActivity(Intent(context, GrowActivity::class.java))
                }, modifier = buttonModifier) { Text("🌱 Grow") }

                Button(onClick = {
                    context.startActivity(Intent(context, ConnectActivity::class.java))
                }, modifier = buttonModifier) { Text("🌐 Connect") }

                Button(onClick = {
                    context.startActivity(Intent(context, TradeActivity::class.java))
                }, modifier = buttonModifier) { Text("♻️ Trade") }

                Button(onClick = {
                    context.startActivity(Intent(context, MarketActivity::class.java))
                }, modifier = buttonModifier) { Text("🛒 Market") }

                Button(onClick = {
                    context.startActivity(Intent(context, CartActivity::class.java))
                }, modifier = buttonModifier) { Text("🛍️ View Cart") }

                Button(onClick = {
                    context.startActivity(Intent(context, OrderHistoryActivity::class.java))
                }, modifier = buttonModifier) { Text("📦 Order History") }

                Button(onClick = {
                    context.startActivity(Intent(context, TradeResponseActivity::class.java))
                }, modifier = buttonModifier) { Text("📬 My Trade Responses") }

                Button(onClick = {
                    context.startActivity(Intent(context, MyTradeProposalsActivity::class.java))
                }, modifier = buttonModifier) { Text("📨 My Trade Proposals") }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(context, ProfileActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("👤 Profile / Settings")
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    private fun fetchWeatherData(onResult: (Double?) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(weatherUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = it.body?.string()
                    val temp = json?.let { body ->
                        JSONObject(body).getJSONObject("main").getDouble("temp")
                    }
                    onResult(temp)
                }
            }
        })
    }

    private fun fetchNews(onResult: (List<String>) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(newsUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = it.body?.string()
                    val list = mutableListOf<String>()
                    json?.let { body ->
                        val articles = JSONObject(body).getJSONArray("articles")
                        for (i in 0 until articles.length()) {
                            list.add(articles.getJSONObject(i).getString("title"))
                        }
                    }
                    onResult(list)
                }
            }
        })
    }
}
