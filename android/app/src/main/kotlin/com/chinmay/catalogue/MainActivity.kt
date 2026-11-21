package com.chinmay.catalogue

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.data.repository.ThemeMode
import com.chinmay.catalogue.core.data.repository.ThemeRepository
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.navigation.CatalogueApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single activity; every screen is a Compose destination. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themes: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The look the customer chose in the drawer (sun / moon), not the phone's setting.
            val mode by themes.mode.collectAsStateWithLifecycle()
            val dark = mode == ThemeMode.DARK
            // Status and navigation bar icons follow the app's look: dark icons on light screens and vice versa.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            CatalogueTheme(darkTheme = dark) {
                CatalogueApp()
            }
        }
    }
}
