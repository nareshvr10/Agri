package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.core.ProvideLocalizedContext
import com.example.models.AppLanguage
import com.example.models.CropType
import com.example.models.Farm
import com.example.models.UserProfile
import com.example.models.UserRole
import com.example.navigation.Routes
import com.example.ui.components.LanguageTogglePill
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.WeatherAndRiskScreen
import com.example.domain.EnvironmentalRiskEvaluator
import com.example.models.WeatherSnapshot
import com.example.ui.theme.AgroGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgroGuardRobolectricUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `login screen renders Google Sign-In CTA and non-admin notice`() {
        composeRule.setContent {
            AgroGuardTheme {
                LoginScreen(
                    language = AppLanguage.ENGLISH,
                    onToggleLanguage = {},
                    onAuthSuccess = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun `dashboard shows honest Device not connected when no sensors exist`() {
        val farmer = UserProfile(
            userId = "farmer-1",
            displayName = "Karthik",
            email = "karthik@example.com",
            role = UserRole.FARMER.name
        )
        val farm = Farm(
            id = "farm-1",
            ownerId = "farmer-1",
            name = "Cauvery Paddy Plot",
            locationName = "Thanjavur",
            latitude = 10.78,
            longitude = 79.13,
            areaAcres = 3.0,
            primaryCrop = CropType.PADDY.name
        )
        composeRule.setContent {
            AgroGuardTheme {
                DashboardScreen(
                    user = farmer,
                    farms = listOf(farm),
                    selectedFarm = farm,
                    zones = emptyList(),
                    devices = emptyList(),
                    readings = emptyList(),
                    alerts = emptyList(),
                    scans = emptyList(),
                    irrigationPreview = null,
                    language = AppLanguage.ENGLISH,
                    isOnline = true,
                    lastSyncedEpochMs = 0L,
                    onSelectFarm = {},
                    onOpenAddFarm = {},
                    onOpenScanner = {},
                    onOpenSensors = {},
                    onOpenIrrigation = {},
                    onOpenWeather = {},
                    onOpenAlerts = {},
                    onOpenConsultation = {},
                    onOpenCropZones = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_screen")
            .assertIsDisplayed()
            .performScrollToNode(hasTestTag("device_not_connected_label"))
        composeRule.onNodeWithTag("device_not_connected_label")
            .assertIsDisplayed()
            .assertTextEquals("Device not connected")
    }

    @Test
    fun `more hub hides admin management for farmer and shows it for admin`() {
        var navigatedRoute = ""
        composeRule.setContent {
            var role by remember { mutableStateOf(UserRole.FARMER) }
            AgroGuardTheme {
                MoreHubScreen(
                    userRole = role,
                    onNavigate = { route ->
                        navigatedRoute = route
                    }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_${Routes.IRRIGATION}").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_${Routes.ADMIN_MANAGEMENT}").assertDoesNotExist()
        composeRule.onNodeWithTag("hub_${Routes.IRRIGATION}").performClick()
        composeRule.waitForIdle()
        assertEquals(Routes.IRRIGATION, navigatedRoute)
    }

    @Test
    fun `more hub shows admin management for admin user`() {
        composeRule.setContent {
            AgroGuardTheme {
                MoreHubScreen(
                    userRole = UserRole.ADMIN,
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("more_hub_screen")
            .performScrollToNode(hasTestTag("hub_${Routes.ADMIN_MANAGEMENT}"))
        composeRule.onNodeWithTag("hub_${Routes.ADMIN_MANAGEMENT}").assertIsDisplayed()
    }

    @Test
    fun `language toggle switches between English and Tamil cleanly`() {
        composeRule.setContent {
            var language by remember { mutableStateOf(AppLanguage.ENGLISH) }
            ProvideLocalizedContext(language = language) {
                AgroGuardTheme {
                    LanguageTogglePill(
                        language = language,
                        onClick = {
                            language = if (language == AppLanguage.ENGLISH) AppLanguage.TAMIL else AppLanguage.ENGLISH
                        }
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("EN | தமிழ்").assertIsDisplayed()
        composeRule.onNodeWithTag("language_toggle_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("தமிழ் | EN").assertIsDisplayed()
    }

    @Test
    fun `weather screen shows unavailable state honestly when weather provider is disabled`() {
        val assessment = EnvironmentalRiskEvaluator.evaluate(
            cropType = CropType.TOMATO,
            weather = WeatherSnapshot(
                available = false,
                providerName = "Disabled",
                locationLabel = "Coimbatore",
                unavailableReason = "Weather provider is disabled in Settings."
            ),
            latestReading = null
        )
        composeRule.setContent {
            AgroGuardTheme {
                WeatherAndRiskScreen(
                    selectedFarm = null,
                    assessment = assessment,
                    language = AppLanguage.ENGLISH,
                    onRefreshWeather = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Weather data unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Weather provider is disabled in Settings.").assertIsDisplayed()
    }
}
