package com.example.tombyts_android

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.navigation.NavController
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var username by remember { mutableStateOf("tom") }
            var password by remember { mutableStateOf("password") }

            Text("Welcome to Tombyts")
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                placeholder = { Text("Enter your username") }
            )

            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Check
                    else Icons.Filled.CheckCircle
                    // Please provide localized description for accessibility
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            TestApiButton()
            LoginButton(username, password, navController)
        }
    }
}

//@Composable
//fun TestApiButton() {
//    val coroutineScope = rememberCoroutineScope()
//    val apiService = Classes.ApiProvider.apiService
//    val context = LocalContext.current
//
//    Button(onClick = {
//        coroutineScope.launch {
//            try {
//                val response = apiService.getResponse()
//                if (response.isSuccessful) {
//                    val responseBody = response.body()
//                    Log.d("blah", "API Response: $responseBody")
//                    Toast.makeText(context, "API Response: $responseBody", Toast.LENGTH_LONG).show()
//                } else {
//                    Log.e("blah", "API Error: ${response.code()} ${response.message()}")
//                    Toast.makeText(context, "API Error: ${response.code()} ${response.message()}", Toast.LENGTH_LONG).show()
//                }
//            }
//            catch (e: Exception) {
//                Log.e("blah", "API Exception", e)
//                Toast.makeText(context, "API Exception: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }) {Text("Test API")}
//}

@Composable
fun TestApiButton() {
    val coroutineScope = rememberCoroutineScope()
    val apiService = Classes.ApiProvider.getSubtitleApiService()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var subtitleText by remember { mutableStateOf("") }

    Button(onClick = {
        coroutineScope.launch {
            try {
                // Hardcoded movie title and language for testing
                val movieTitle = "Dune.Part.Two.2024.1080p.WEBRip.x264.AAC-[YTS.MX]"
                val language = "en"
                val token = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getString("auth_token", "") ?: ""

                if (token.isNotEmpty()) {
                    val response = apiService.getSubtitles(movieTitle, language, "Bearer $token")
                    if (response.isSuccessful) {
                        val subtitleContent = response.body() ?: ""
                        val lines = subtitleContent.split("\n")
                        val firstFewLines = lines.take(5).joinToString("\n") // Get first 5 lines
                        Toast.makeText(context, "Subtitle:\n$firstFewLines", Toast.LENGTH_LONG).show()
                        subtitleText = subtitleContent
                        showDialog = true
                    } else {
                        Toast.makeText(context, "Failed to fetch subtitles: ${response.code()}", Toast.LENGTH_LONG).show()
                        Log.e("blah", "Failed to fetch subtitles: ${response.code()} ${response.message()}")
                    }
                } else {
                    Toast.makeText(context, "No token found. Please login.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("blah", "Error fetching subtitles", e)
            }
        }
    }) {
        Text("Test Subtitles")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Subtitle") },
            text = { Text(subtitleText) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}


@Composable
fun LoginButton(username: String, password: String, navController: NavController) {
    var apiResponse by remember { mutableStateOf("") } // State to hold API response
    val coroutineScope = rememberCoroutineScope()
    val apiService = Classes.ApiProvider.apiService
    val context = LocalContext.current

    Button(onClick = {
        coroutineScope.launch {
            try {
                val loginRequest = LoginRequest(username, password)
                val response = apiService.login(loginRequest)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val token = loginResponse?.token
                    if (token != null) {
                        val sharedPreferences =
                            context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("auth_token", token)
                        editor.apply()

                        navController.navigate("movieList/$token") // Pass token as argument
                    } else {
                        apiResponse = "Login failed: Token is null"
                    }
                } else {
                    apiResponse = "Login failed: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                apiResponse = "API Exception: ${e.message}"
                Log.e("blah", "API Exception", e)
            }
        }
    }) {
        Text("Login")
    }

    // Display API response (optional)
    Text(apiResponse)
}