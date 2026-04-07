package com.neurodrive.smartcar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.neurodrive.smartcar.ui.theme.NeuroDriveTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import android.util.Log  // ✅ Add this import
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // ✅ Check Google Play Services
        checkGooglePlayServices()

        setContent {
            NeuroDriveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    AppNavigation()
                }
            }
        }
    }

    private fun checkGooglePlayServices() {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d("PlayServices", "✅ Google Play Services is available")
        } else {
            Log.e("PlayServices", "❌ NOT available. Code: $resultCode")
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(this, resultCode, 1001)?.show()
            }
        }
    }


}
// In your Activity or Fragment
