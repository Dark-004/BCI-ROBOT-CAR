package com.neurodrive.smartcar.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

@Composable
fun TestFirebaseScreen(
    viewModel: FirebaseAuthRepository = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Firebase Connection Test", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    val testResult = viewModel.register(
                        email = "test@example.com",
                        password = "Test123!",
                        fullName = "Test User",
                        username = "testuser"
                    )
                    result = if (testResult.isSuccess) {
                        "✅ Firebase Connected!"
                    } else {
                        "❌ Error: ${testResult.exceptionOrNull()?.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Registration")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(result)
    }
}