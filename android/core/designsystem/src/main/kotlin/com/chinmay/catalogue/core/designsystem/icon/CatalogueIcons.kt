package com.chinmay.catalogue.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Icons the bundled material-icons-core set lacks, drawn from Material Symbols' own path data (Apache 2.0) on the
 * same 24dp grid, so they sit perfectly next to the core icons.
 */
object CatalogueIcons {
    /** A minus sign (Material "remove"), for quantity steppers. */
    val Minus: ImageVector by lazy { icon("Catalogue.Minus", "M19,13H5v-2h14v2z") }

    /** Hamburger menu (Material "menu"): opens the side drawer. */
    val Menu: ImageVector by lazy { icon("Catalogue.Menu", "M3,18h18v-2H3v2zM3,13h18v-2H3v2zM3,6v2h18V6H3z") }

    /** Sun (Material "light_mode"): the light theme. */
    val Sun: ImageVector by lazy {
        icon(
            "Catalogue.Sun",
            "M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5s5,-2.24 5,-5S14.76,7 12,7L12,7z" +
                "M2,13l2,0c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1l-2,0c-0.55,0 -1,0.45 -1,1S1.45,13 2,13z" +
                "M20,13l2,0c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1l-2,0c-0.55,0 -1,0.45 -1,1S19.45,13 20,13z" +
                "M11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V2c0,-0.55 -0.45,-1 -1,-1S11,1.45 11,2z" +
                "M11,20v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1C11.45,19 11,19.45 11,20z" +
                "M5.99,4.58c-0.39,-0.39 -1.03,-0.39 -1.41,0c-0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0s0.39,-1.03 0,-1.41L5.99,4.58z" +
                "M18.36,16.95c-0.39,-0.39 -1.03,-0.39 -1.41,0c-0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0c0.39,-0.39 0.39,-1.03 0,-1.41L18.36,16.95z" +
                "M19.42,5.99c0.39,-0.39 0.39,-1.03 0,-1.41c-0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0L19.42,5.99z" +
                "M7.05,18.36c0.39,-0.39 0.39,-1.03 0,-1.41c-0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0L7.05,18.36z",
        )
    }

    /** Crescent moon (Material "dark_mode"): the dark theme. */
    val Moon: ImageVector by lazy {
        icon(
            "Catalogue.Moon",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26" +
                "c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3L12,3z",
        )
    }

    /** A box (Material "inventory_2", simplified): availability. */
    val Box: ImageVector by lazy {
        icon(
            "Catalogue.Box",
            "M20,2H4C3,2 2,2.9 2,4v3.01c0,0.72 0.43,1.34 1,1.69V20c0,1.1 1.1,2 2,2h14c0.9,0 2,-0.9 2,-2V8.7c0.57,-0.35 1,-0.97 1,-1.69V4" +
                "C22,2.9 21,2 20,2zM19,20H5V9h14V20zM20,7H4V4h16V7zM9,12h6v2H9V12z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
            .build()
}
