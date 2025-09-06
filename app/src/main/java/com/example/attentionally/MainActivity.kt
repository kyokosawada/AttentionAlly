package com.example.attentionally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.attentionally.ui.theme.AttentionAllyTheme
import com.example.attentionally.presentation.navigation.AttentionAllyNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttentionAllyTheme {
                AttentionAllyNavigation()
            }
        }
    }
}

