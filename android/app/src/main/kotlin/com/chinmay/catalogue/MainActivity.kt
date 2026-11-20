package com.chinmay.catalogue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.navigation.CatalogueNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Single activity; every screen is a Compose destination. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CatalogueTheme {
                CatalogueNavHost()
            }
        }
    }
}
