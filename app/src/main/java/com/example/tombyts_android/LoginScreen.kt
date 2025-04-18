package com.example.tombyts_android

import SimpleAuthPreferences
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val context = LocalContext.current
    val authPreferences = remember { SimpleAuthPreferences(context) }
    val storedToken = remember { mutableStateOf(authPreferences.getAuthToken()) }

    LaunchedEffect(key1 = storedToken.value) {
        if (!storedToken.value.isNullOrEmpty()) {
            navController.navigate("movieList/${storedToken.value}") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

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
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            LoginButton(username, password, navController, authPreferences,
                onLoginSuccess = { token ->
                    storedToken.value = token})
        }
    }
}

@Composable
fun LoginButton(
    username: String,
    password: String,
    navController: NavController,
    authPreferences: SimpleAuthPreferences,
    onLoginSuccess: (String) -> Unit
) {
    var apiResponse by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val apiService = Classes.ApiProvider.apiService

    Button(onClick = {
        coroutineScope.launch {
            try {
                val loginRequest = LoginRequest(username, password)
                val response = apiService.login(loginRequest)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val token = loginResponse?.token
                    if (token != null) {
                        authPreferences.saveAuthToken(token)
                        onLoginSuccess(token)
                        navController.navigate("movieList/$token")
                    } else {
                        apiResponse = "Login failed: Token is null"
                        Log.e("Login", apiResponse)
                    }
                } else {
                    apiResponse = "Login failed: ${response.code()} ${response.message()}"
                    Log.e("Login", apiResponse)
                }
            } catch (e: Exception) {
                apiResponse = "API Exception: ${e.message}"
                Log.e("blah", "API Exception", e)
            }
        }
    }) {
        Text("Login")
    }
}