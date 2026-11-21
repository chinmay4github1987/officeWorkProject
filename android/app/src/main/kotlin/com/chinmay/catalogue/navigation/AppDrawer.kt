package com.chinmay.catalogue.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.R
import com.chinmay.catalogue.core.data.repository.ThemeMode
import com.chinmay.catalogue.core.designsystem.component.ThemeSwitch
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category

/** Where a drawer item leads. :app maps these to navigation, so the drawer stays a plain, testable composable. */
sealed interface DrawerDestination {
    data object Home : DrawerDestination
    data object Search : DrawerDestination
    data object Cart : DrawerDestination
    data object SignIn : DrawerDestination
    data class CategoryPage(val category: Category) : DrawerDestination
}

/**
 * The side drawer: who's signed in, the main places (Home, Search, Cart), every category, sign in/out, and the
 * light/dark switch (sun / moon) at the bottom.
 */
@Composable
fun AppDrawer(
    state: DrawerUiState,
    onNavigate: (DrawerDestination) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
    val itemPadding = Modifier.padding(horizontal = Spacing.md)
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
            DrawerHeader(state)
            Spacer(Modifier.height(Spacing.sm))

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_home)) },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(DrawerDestination.Home) },
                colors = itemColors,
                modifier = itemPadding,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_search)) },
                icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                selected = false,
                onClick = { onNavigate(DrawerDestination.Search) },
                colors = itemColors,
                modifier = itemPadding,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.drawer_cart)) },
                icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                badge = {
                    if (state.cartCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary) {
                            Text(state.cartCount.coerceAtMost(99).toString())
                        }
                    }
                },
                selected = false,
                onClick = { onNavigate(DrawerDestination.Cart) },
                colors = itemColors,
                modifier = itemPadding,
            )

            if (state.categories.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.drawer_categories),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm).semantics { heading() },
                )
                val accents = CatalogueTheme.extendedColors.categoryAccents
                state.categories.forEachIndexed { index, category ->
                    NavigationDrawerItem(
                        label = { Text(category.name) },
                        icon = {
                            // A small pastel dot in the category's tile colour, matching the tiles on Home.
                            Box(Modifier.size(14.dp).background(accents[index % accents.size].first, CircleShape))
                        },
                        selected = false,
                        onClick = { onNavigate(DrawerDestination.CategoryPage(category)) },
                        colors = itemColors,
                        modifier = itemPadding,
                    )
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
            if (state.user == null) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_sign_in)) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    selected = false,
                    onClick = { onNavigate(DrawerDestination.SignIn) },
                    colors = itemColors,
                    modifier = itemPadding,
                )
            } else {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_sign_out)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = onSignOut,
                    colors = itemColors,
                    modifier = itemPadding,
                )
            }

            // Appearance: the sun / moon switch.
            Column(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(R.string.drawer_appearance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeSwitch(darkSelected = state.themeMode == ThemeMode.DARK, onSelect = onDarkThemeChange)
            }
        }
    }
}

/** The account at the top: initials in a teal circle with name and email, or a friendly "Welcome" when signed out. */
@Composable
private fun DrawerHeader(state: DrawerUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .background(CatalogueTheme.extendedColors.heroGradient, MaterialTheme.shapes.large)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val user = state.user
        Box(
            Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (user != null) {
                val initials = (user.firstName.take(1) + user.lastName.take(1)).ifBlank { user.username.take(1) }.uppercase()
                Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                user?.fullName ?: stringResource(R.string.drawer_welcome),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                user?.email ?: stringResource(R.string.drawer_welcome_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
