package com.manobseba.foundation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.fragment.app.FragmentActivity
import com.manobseba.foundation.config.configureAppCheck
import com.manobseba.foundation.ui.ManobsebaApp

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure Firebase App Check with launch intent before any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            ManobsebaApp(windowSizeClass = windowSizeClass)
        }
    }
}
