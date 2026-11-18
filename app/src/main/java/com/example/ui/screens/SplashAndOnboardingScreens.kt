package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.models.AppLanguage
import com.example.models.CropType
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.cropLabel
import com.example.ui.theme.ForestDeep
import com.example.ui.theme.LeafGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    onContinueToOnboarding: () -> Unit,
    onContinueToLogin: () -> Unit,
    onAutoSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(onboardingCompleted) {
        delay(850L)
        if (!onboardingCompleted) {
            onContinueToOnboarding()
        } else {
            val cm = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = cm,
                onAuthSuccess = onAutoSignedIn,
                onUnauthenticated = onContinueToLogin,
                scope = scope
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen")
    ) {
        Image(
            painter = painterResource(R.drawable.bg_agroguard_leaf),
            contentDescription = "AgroGuard AI botanical smart farming backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ForestDeep.copy(alpha = 0.35f),
                            ForestDeep.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = stringResource(R.string.brand_title),
                        tint = LeafGreen,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.brand_title),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFD8F3DC),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(
                color = Color(0xFFB7E4C7),
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onFinishOnboarding: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val titles = listOf(
        stringResource(R.string.onboarding_title_1),
        stringResource(R.string.onboarding_title_2),
        stringResource(R.string.onboarding_title_3)
    )
    val descs = listOf(
        stringResource(R.string.onboarding_desc_1),
        stringResource(R.string.onboarding_desc_2),
        stringResource(R.string.onboarding_desc_3)
    )
    val icons = listOf(Icons.Default.Sensors, Icons.Default.Eco, Icons.Default.WaterDrop)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.brand_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageTogglePill(language = language, onClick = onToggleLanguage)
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Image(
            painter = painterResource(R.drawable.hero_agroguard_sunrise),
            contentDescription = "AgroGuard AI Smart Farming Sunrise Hero",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icons[page],
                            contentDescription = titles[page],
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = titles[page],
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = descs[page],
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (idx == page) 28.dp else 10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (idx == page) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (page > 0) {
                OutlinedButton(
                    onClick = { page-- },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.action_back))
                }
            }
            Button(
                onClick = {
                    if (page < 2) page++ else onFinishOnboarding()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    if (page < 2) stringResource(R.string.action_continue)
                    else stringResource(R.string.action_get_started)
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen")
    ) {
        Image(
            painter = painterResource(R.drawable.bg_agroguard_leaf),
            contentDescription = "AgroGuard AI login leaf backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ForestDeep.copy(alpha = 0.55f),
                            ForestDeep.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                LanguageTogglePill(language = language, onClick = onToggleLanguage)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = "AgroGuard AI",
                            tint = ForestDeep,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD8F3DC)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sign_in_google),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.auth_no_public_admin),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmerRegistrationScreen(
    initialName: String,
    initialEmail: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSubmitProfile: (name: String, district: String, phone: String, crops: List<CropType>) -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var district by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCrops by remember { mutableStateOf(setOf(CropType.PADDY)) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("farmer_registration_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.farmer_reg_title),
                style = MaterialTheme.typography.headlineSmall
            )
            LanguageTogglePill(language = language, onClick = onToggleLanguage)
        }

        Text(
            text = stringResource(R.string.farmer_reg_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (initialEmail.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Email",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$initialEmail (Role: Farmer)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.label_full_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_name_input")
        )

        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text(stringResource(R.string.label_district)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_district_input")
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.label_phone)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.label_crops_grown),
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropType.entries.forEach { crop ->
                val selected = crop in selectedCrops
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedCrops = if (selected && selectedCrops.size > 1) {
                            selectedCrops - crop
                        } else {
                            selectedCrops + crop
                        }
                    },
                    label = { Text(cropLabel(crop, language)) }
                )
            }
        }

        if (validationError != null) {
            Text(
                text = validationError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                if (fullName.trim().length < 2) {
                    validationError = "Please enter your name (at least 2 characters)."
                } else {
                    validationError = null
                    onSubmitProfile(fullName.trim(), district.trim(), phone.trim(), selectedCrops.toList())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("complete_registration_button")
        ) {
            Text(stringResource(R.string.action_continue))
        }

        TextButton(
            onClick = onSignOut,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.action_logout))
        }
    }
}
