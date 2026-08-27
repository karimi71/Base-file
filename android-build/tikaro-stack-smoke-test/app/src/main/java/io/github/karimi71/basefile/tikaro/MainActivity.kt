package io.github.karimi71.basefile.tikaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Compile and link framework-facing parts of the intended Tikaro stack.
        BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        DocumentFile.fromFile(filesDir)
        runCatching { ExifInterface(filesDir.resolve("missing.jpg")) }
        WorkManager.getInstance(this).enqueueUniqueWork(
            "tikaro-smoke",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TikaroWorker>().build()
        )
        Json.encodeToString(BackupPayload(dayKey = "1404-01-01", completed = false))

        setContent { TikaroSmokeApp() }
    }
}

@Composable
fun TikaroSmokeApp() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Tikaro offline stack")
                        Button(onClick = { navController.navigate("details/1404-01-01") }) {
                            Text("Open day")
                        }
                    }
                }
                composable("details/{dayKey}") { entry ->
                    Text("Day ${entry.arguments?.getString("dayKey")}")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TikaroSmokePreview() {
    TikaroSmokeApp()
}
