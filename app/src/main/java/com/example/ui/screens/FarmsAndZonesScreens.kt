package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.domain.CropGuideCatalog
import com.example.models.AppLanguage
import com.example.models.CropType
import com.example.models.CropZone
import com.example.models.ExpertDirectoryEntry
import com.example.models.Farm
import com.example.models.GrowthStage
import com.example.models.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.cropLabel
import com.example.ui.components.formatEpochShort
import com.example.ui.components.stageLabel
import com.google.android.gms.location.LocationServices

@Composable
fun FarmManagementScreen(
    farms: List<Farm>,
    selectedFarmId: String,
    experts: List<ExpertDirectoryEntry>,
    userRole: UserRole,
    language: AppLanguage,
    onSelectFarm: (String) -> Unit,
    onOpenAddFarm: () -> Unit,
    onOpenZones: () -> Unit,
    onShareFarmWithExpert: (Farm, String) -> Unit
) {
    var shareTargetFarm by remember { mutableStateOf<Farm?>(null) }

    Scaffold(
        floatingActionButton = {
            if (userRole != UserRole.EXPERT) {
                FloatingActionButton(
                    onClick = onOpenAddFarm,
                    modifier = Modifier.testTag("fab_add_farm")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.screen_add_farm))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("farm_management_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (farms.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Agriculture,
                        title = stringResource(R.string.dashboard_no_farms_title),
                        description = if (userRole == UserRole.EXPERT) {
                            "No farms have been explicitly shared with your Expert account yet."
                        } else {
                            stringResource(R.string.dashboard_no_farms_desc)
                        },
                        action = {
                            if (userRole != UserRole.EXPERT) {
                                Button(onClick = onOpenAddFarm) {
                                    Text(stringResource(R.string.screen_add_farm))
                                }
                            }
                        }
                    )
                }
            } else {
                items(farms, key = { it.id }) { farm ->
                    val isSelected = farm.id == selectedFarmId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = farm.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "${farm.locationName} • ${farm.areaAcres} acres • ${cropLabel(farm.resolvedCrop(), language)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "Coordinates: ${"%.4f".format(farm.latitude)}, ${"%.4f".format(farm.longitude)} • Shared experts: ${farm.sharedExpertIds.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    onSelectFarm(farm.id)
                                    onOpenZones()
                                }) {
                                    Text(stringResource(R.string.screen_crop_zones))
                                }
                                if (userRole == UserRole.FARMER || userRole == UserRole.ADMIN) {
                                    OutlinedButton(onClick = {
                                        shareTargetFarm = if (shareTargetFarm?.id == farm.id) null else farm
                                    }) {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share Expert")
                                    }
                                }
                            }
                            if (shareTargetFarm?.id == farm.id) {
                                if (experts.isEmpty()) {
                                    Text(
                                        text = "No verified Agricultural Experts registered in directory yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    experts.forEach { expert ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${expert.displayName} (${expert.district})")
                                            OutlinedButton(onClick = {
                                                onShareFarmWithExpert(farm, expert.userId)
                                                shareTargetFarm = null
                                            }) {
                                                Text(if (expert.userId in farm.sharedExpertIds) "Shared" else "Grant")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFarmScreen(
    language: AppLanguage,
    onCreateFarm: (
        name: String,
        locationName: String,
        lat: Double,
        lon: Double,
        areaAcres: Double,
        crop: CropType
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("10.7870") }
    var lonText by remember { mutableStateOf("79.1378") }
    var areaText by remember { mutableStateOf("2.5") }
    var selectedCrop by remember { mutableStateOf(CropType.PADDY) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    @SuppressLint("MissingPermission")
    fun fetchCoarseLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                latText = "%.4f".format(loc.latitude)
                lonText = "%.4f".format(loc.longitude)
                statusMessage = "Location updated from device GPS."
            } else {
                statusMessage = "No cached GPS fix available; enter coordinates manually."
            }
        }.addOnFailureListener {
            statusMessage = "Unable to read location; enter coordinates manually."
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchCoarseLocation()
        else statusMessage = "Location permission denied. Enter latitude/longitude manually."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("add_farm_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Farm Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_farm_name_input")
        )
        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Village / District") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_farm_location_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text("Longitude") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) fetchCoarseLocation()
                else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Use Current Approximate Location")
        }

        OutlinedTextField(
            value = areaText,
            onValueChange = { areaText = it },
            label = { Text("Field Area (Acres)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Primary Crop", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropType.entries.forEach { crop ->
                FilterChip(
                    selected = crop == selectedCrop,
                    onClick = { selectedCrop = crop },
                    label = { Text(cropLabel(crop, language)) }
                )
            }
        }

        if (statusMessage != null) {
            Text(
                text = statusMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                val lat = latText.toDoubleOrNull()
                val lon = lonText.toDoubleOrNull()
                val area = areaText.toDoubleOrNull()
                if (name.trim().length < 2 || locationName.trim().length < 2) {
                    statusMessage = "Enter valid farm name and district."
                } else if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    statusMessage = "Enter valid latitude (-90..90) and longitude (-180..180)."
                } else if (area == null || area <= 0.0) {
                    statusMessage = "Enter a valid positive field area."
                } else {
                    onCreateFarm(name.trim(), locationName.trim(), lat, lon, area, selectedCrop)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_farm_button")
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
fun CropSelectionScreen(
    language: AppLanguage,
    onCreateZoneWithCrop: (CropType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("crop_selection_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(CropType.entries) { crop ->
            val spec = CropGuideCatalog.specFor(crop)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${cropLabel(crop, language)} (${spec.scientificName})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (language == AppLanguage.TAMIL) spec.stageSummaryTa else spec.stageSummaryEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Default Moisture Band: ${spec.defaultMinMoisturePct.toInt()}%–${spec.defaultMaxMoisturePct.toInt()}% • Max Air Temp: ${spec.defaultMaxAirTempC.toInt()}°C",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Common Varieties: ${spec.typicalVarieties.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { onCreateZoneWithCrop(crop) }) {
                        Text("Configure ${cropLabel(crop, language)} Zone")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CropZonesScreen(
    selectedFarm: Farm?,
    zones: List<CropZone>,
    initialCrop: CropType?,
    language: AppLanguage,
    userRole: UserRole,
    onCreateZone: (
        farm: Farm,
        name: String,
        crop: CropType,
        variety: String?,
        plantingDateEpochMs: Long,
        stage: GrowthStage,
        areaAcres: Double,
        minMoisture: Double,
        maxMoisture: Double,
        maxAirTemp: Double
    ) -> Unit
) {
    var showForm by remember(initialCrop) { mutableStateOf(initialCrop != null) }
    var zoneName by remember { mutableStateOf("") }
    var cropType by remember(initialCrop) { mutableStateOf(initialCrop ?: selectedFarm?.resolvedCrop() ?: CropType.PADDY) }
    var variety by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(GrowthStage.VEGETATIVE) }
    var areaText by remember { mutableStateOf("1.0") }
    val defaultSpec = CropGuideCatalog.specFor(cropType)
    var minMoistText by remember(cropType) { mutableStateOf(defaultSpec.defaultMinMoisturePct.toInt().toString()) }
    var maxMoistText by remember(cropType) { mutableStateOf(defaultSpec.defaultMaxMoisturePct.toInt().toString()) }
    var maxTempText by remember(cropType) { mutableStateOf(defaultSpec.defaultMaxAirTempC.toInt().toString()) }
    var stageMenuExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val farmZones = selectedFarm?.let { f -> zones.filter { it.farmId == f.id } }.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("crop_zones_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedFarm == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Agriculture,
                    title = "No Farm Selected",
                    description = "Select or add a farm first to manage its crop zones."
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedFarm.name} Zones (${farmZones.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (userRole != UserRole.EXPERT) {
                        Button(onClick = { showForm = !showForm }) {
                            Text(if (showForm) stringResource(R.string.action_cancel) else "Add Zone")
                        }
                    }
                }
            }

            if (showForm) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = zoneName,
                                onValueChange = { zoneName = it },
                                label = { Text("Zone Name (e.g., North Plot A)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CropType.entries.forEach { c ->
                                    FilterChip(
                                        selected = c == cropType,
                                        onClick = { cropType = c },
                                        label = { Text(cropLabel(c, language)) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = variety,
                                onValueChange = { variety = it },
                                label = { Text("Variety (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            ExposedDropdownMenuBox(
                                expanded = stageMenuExpanded,
                                onExpandedChange = { stageMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = stageLabel(stage, language),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Growth Stage") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = stageMenuExpanded,
                                    onDismissRequest = { stageMenuExpanded = false }
                                ) {
                                    GrowthStage.entries.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(stageLabel(s, language)) },
                                            onClick = {
                                                stage = s
                                                stageMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = areaText,
                                    onValueChange = { areaText = it },
                                    label = { Text("Area (Acres)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = minMoistText,
                                    onValueChange = { minMoistText = it },
                                    label = { Text("Min Moist %") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = maxMoistText,
                                    onValueChange = { maxMoistText = it },
                                    label = { Text("Max Moist %") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = maxTempText,
                                onValueChange = { maxTempText = it },
                                label = { Text("Max Air Temp Alert (°C)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMsg != null) {
                                Text(errorMsg.orEmpty(), color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = {
                                    val area = areaText.toDoubleOrNull()
                                    val minM = minMoistText.toDoubleOrNull()
                                    val maxM = maxMoistText.toDoubleOrNull()
                                    val maxT = maxTempText.toDoubleOrNull()
                                    if (zoneName.trim().length < 2 || area == null || area <= 0.0 ||
                                        minM == null || maxM == null || minM > maxM || maxT == null
                                    ) {
                                        errorMsg = "Check zone name, area, and threshold numbers."
                                    } else {
                                        errorMsg = null
                                        onCreateZone(
                                            selectedFarm,
                                            zoneName.trim(),
                                            cropType,
                                            variety.trim().ifBlank { null },
                                            System.currentTimeMillis(),
                                            stage,
                                            area,
                                            minM,
                                            maxM,
                                            maxT
                                        )
                                        showForm = false
                                        zoneName = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Crop Zone")
                            }
                        }
                    }
                }
            }

            items(farmZones, key = { it.id }) { zone ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${zone.name} • ${cropLabel(zone.resolvedCrop(), language)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stage: ${stageLabel(zone.resolvedStage(), language)} • Variety: ${zone.variety ?: "Not specified"} • Area: ${zone.areaAcres} ac",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Thresholds: Moisture ${zone.minSoilMoisturePct.toInt()}%–${zone.maxSoilMoisturePct.toInt()}% • Max Temp ${zone.maxAirTempC.toInt()}°C • Planted: ${formatEpochShort(zone.plantingDateEpochMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
