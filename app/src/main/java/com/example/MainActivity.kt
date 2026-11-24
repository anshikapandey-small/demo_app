package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wonderart.app.WonderartApplication
import com.wonderart.app.navigation.WonderartAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WonderartApplication).container
        setContent {
            WonderartAppRoot(container = container)
        }
    }
}
