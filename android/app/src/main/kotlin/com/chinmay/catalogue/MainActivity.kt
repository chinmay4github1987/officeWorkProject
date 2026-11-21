package com.chinmay.catalogue

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chinmay.catalogue.core.designsystem.theme.BRAND_IS_DARK
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.navigation.CatalogueNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Single activity; every screen is a Compose destination. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The brand is dark whatever the system setting, so the status and navigation bar icons must be light,
        // otherwise they'd be dark-on-dark and unreadable on a phone set to light mode.
        if (BRAND_IS_DARK) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        } else {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)
        setContent {
            CatalogueTheme {
                CatalogueNavHost()
            }
        }
    }
}
