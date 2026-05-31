package com.appetizers.spotra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.appetizers.spotra.presentation.home.HomeScreen
import com.appetizers.spotra.presentation.theme.SpotraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpotraTheme {
                HomeScreen()
            }
        }
    }
}
