package com.neurodrive.smartcar.ui.help

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.R.attr.text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurodrive.smartcar.R
import com.neurodrive.smartcar.ui.theme.*
import android.content.ActivityNotFoundException


import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext

// ════════════════════════════════════════════════════════════════
//  Help Screen Sections
// ════════════════════════════════════════════════════════════════

enum class HelpSection(
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    ELECTRODE_GUIDE(
        "Electrode Placement",
        Icons.Default.Sensors,
        Color(0xFF00E676)
    ),
    GETTING_STARTED(
        "Getting Started",
        Icons.Default.RocketLaunch,
        Color(0xFF448AFF)
    ),
    TROUBLESHOOTING(
        "Troubleshooting",
        Icons.Default.Build,
        Color(0xFFFF9800)
    ),
    FAQ(
        "FAQ",
        Icons.AutoMirrored.Filled.HelpCenter,
        Color(0xFFAB47BC)
    ),
    SAFETY(
        "Safety Guidelines",
        Icons.Default.HealthAndSafety,
        Color(0xFFEF5350)
    ),
    CONTACT(
        "Contact Support",
        Icons.Default.SupportAgent,
        Color(0xFF26C6DA)
    )
}

// ════════════════════════════════════════════════════════════════
//  Data Models
// ════════════════════════════════════════════════════════════════

data class ElectrodePlacementStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val precaution: String,
    val imageRes: Int? = null, // R.drawable.step_x
    val tips: List<String> = emptyList()
)

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String = "General"
)

data class TroubleshootItem(
    val problem: String,
    val solution: String,
    val icon: ImageVector = Icons.Default.Error
)

data class SafetyGuideline(
    val title: String,
    val description: String,
    val severity: SafetySeverity = SafetySeverity.INFO
)

enum class SafetySeverity(val color: Color, val icon: ImageVector) {
    INFO(Color(0xFF448AFF), Icons.Default.Info),
    WARNING(Color(0xFFFF9800), Icons.Default.Warning),
    CRITICAL(Color(0xFFEF5350), Icons.Default.Dangerous)
}

// ════════════════════════════════════════════════════════════════
//  Main Help Screen
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedSection by remember { mutableStateOf<HelpSection?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Top Bar ──
            item {
                HelpTopBar(onNavigateBack = onNavigateBack)
            }

            // ── Search Bar ──
            item {
                HelpSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // ── Quick Help Cards ──
            item {
                QuickHelpSection(
                    onSectionSelected = { selectedSection = it }
                )
            }

            // ── Selected Section Content ──
            when (selectedSection) {
                HelpSection.ELECTRODE_GUIDE -> {
                    item {
                        SectionHeader(
                            section = HelpSection.ELECTRODE_GUIDE,
                            onClose = { selectedSection = null }
                        )
                    }
                    item { ElectrodeOverviewCard() }
                    itemsIndexed(getElectrodePlacementSteps()) { index, step ->
                        ElectrodePlacementStepCard(
                            step = step,
                            isLast = index == getElectrodePlacementSteps().size - 1
                        )
                    }
                    item { ElectrodePrecautionsCard() }
                }

                HelpSection.GETTING_STARTED -> {
                    item {
                        SectionHeader(
                            section = HelpSection.GETTING_STARTED,
                            onClose = { selectedSection = null }
                        )
                    }
                    itemsIndexed(getGettingStartedSteps()) { index, step ->
                        GettingStartedStepCard(step = step)
                    }
                }

                HelpSection.TROUBLESHOOTING -> {
                    item {
                        SectionHeader(
                            section = HelpSection.TROUBLESHOOTING,
                            onClose = { selectedSection = null }
                        )
                    }
                    items(getTroubleshootItems()) { item ->
                        TroubleshootCard(item = item)
                    }
                }

                HelpSection.FAQ -> {
                    item {
                        SectionHeader(
                            section = HelpSection.FAQ,
                            onClose = { selectedSection = null }
                        )
                    }
                    items(
                        getFaqItems().filter {
                            searchQuery.isBlank() ||
                                    it.question.contains(searchQuery, ignoreCase = true) ||
                                    it.answer.contains(searchQuery, ignoreCase = true)
                        }
                    ) { faq ->
                        FaqCard(faq = faq)
                    }
                }

                HelpSection.SAFETY -> {
                    item {
                        SectionHeader(
                            section = HelpSection.SAFETY,
                            onClose = { selectedSection = null }
                        )
                    }
                    items(getSafetyGuidelines()) { guideline ->
                        SafetyGuidelineCard(guideline = guideline)
                    }
                }

                HelpSection.CONTACT -> {
                    item {
                        SectionHeader(
                            section = HelpSection.CONTACT,
                            onClose = { selectedSection = null }
                        )
                    }
                    item { ContactSupportSection() }
                }

                null -> {
                    // Show default content when no section selected
                    item { ElectrodeQuickGuide() }
                    item { CommonIssuesSection() }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Top Bar
// ════════════════════════════════════════════════════════════════

@Composable
fun HelpTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "Help & Support",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Guides, troubleshooting & electrode placement",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Search Bar
// ════════════════════════════════════════════════════════════════

@Composable
fun HelpSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search help articles...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = GreenPrimary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = GreenPrimary,
            focusedLeadingIconColor = GreenPrimary,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
        )
    )
}

// ════════════════════════════════════════════════════════════════
//  Quick Help Section (Horizontal Cards)
// ════════════════════════════════════════════════════════════════

@Composable
fun QuickHelpSection(
    onSectionSelected: (HelpSection) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "Help Topics",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(HelpSection.entries) { section ->
                HelpSectionCard(
                    section = section,
                    onClick = { onSectionSelected(section) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSectionCard(
    section: HelpSection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = section.color.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, section.color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = section.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Section Header
// ════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    section: HelpSection,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = section.color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    section.icon,
                    contentDescription = null,
                    tint = section.color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  ELECTRODE PLACEMENT GUIDE
// ════════════════════════════════════════════════════════════════

@Composable
fun ElectrodeOverviewCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── Header Image ──
            // ✅ Replace R.drawable.electrode_overview with your actual image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1a237e),
                                Color(0xFF0d47a1),
                                Color(0xFF006064)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // ✅ Uncomment when you have the image:
                // Image(
                //     painter = painterResource(id = R.drawable.electrode_overview),
                //     contentDescription = "Electrode Placement Overview",
                //     modifier = Modifier.fillMaxSize(),
                //     contentScale = ContentScale.Fit
                // )

                // Placeholder (remove when you add real image)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "10-20 EEG System",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        "International Electrode Placement",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Electrode Placement Guide",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Proper electrode placement is crucial for accurate brain signal " +
                        "detection. Follow these steps carefully for the best BCI experience. " +
                        "NeuroDrive uses the international 10-20 EEG electrode system.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatItem(
                    value = "8",
                    label = "Steps",
                    color = GreenPrimary
                )
                QuickStatItem(
                    value = "15",
                    label = "Minutes",
                    color = BlueAccent
                )
                QuickStatItem(
                    value = "4-8",
                    label = "Electrodes",
                    color = RedAlert
                )
            }
        }
    }
}

@Composable
fun QuickStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  Electrode Placement Step Card
// ════════════════════════════════════════════════════════════════

@Composable
fun ElectrodePlacementStepCard(
    step: ElectrodePlacementStep,
    isLast: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Timeline indicator ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Step number circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GreenPrimary, CircleShape)
                    .border(2.dp, GreenPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${step.stepNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Connecting line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (isExpanded) 350.dp else 120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GreenPrimary.copy(alpha = 0.5f),
                                    GreenPrimary.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Step content card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = { isExpanded = !isExpanded }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )

                // ── Expanded content ──
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Step Image ──
                        step.imageRes?.let { imageRes ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1a237e).copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = step.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // ── Placeholder image (when no imageRes) ──
                        if (step.imageRes == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF1a237e).copy(alpha = 0.3f),
                                                Color(0xFF006064).copy(alpha = 0.3f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Step ${step.stepNumber} illustration",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // ── Precaution Box ──
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = RedAlert.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                RedAlert.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = RedAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "⚠️ Precaution",
                                        color = RedAlert,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = step.precaution,
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // ── Tips ──
                        if (step.tips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = GreenPrimary.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        "💡 Tips",
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    step.tips.forEach { tip ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                "•",
                                                color = GreenPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = tip,
                                                color = Color.White.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 18.sp
                                            )
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

// ════════════════════════════════════════════════════════════════
//  Electrode Precautions Summary Card
// ════════════════════════════════════════════════════════════════

@Composable
fun ElectrodePrecautionsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RedAlert.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Important Precautions",
                    style = MaterialTheme.typography.titleMedium,
                    color = RedAlert,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val precautions = listOf(
                "🚫 Never use electrodes near open wounds or damaged skin",
                "🚫 Do not apply excessive pressure when placing electrodes",
                "🚫 Keep electrodes away from eyes and mucous membranes",
                "🚫 Do not use if you have a pacemaker without consulting your doctor",
                "🚫 Stop immediately if you feel pain, tingling, or discomfort",
                "🚫 Do not use during thunderstorms (if connected to external power)",
                "✅ Always clean the electrode sites before and after use",
                "✅ Replace electrodes if they show signs of wear or degradation",
                "✅ Store electrodes in a cool, dry place",
                "✅ Consult your physician if you have epilepsy or seizure disorders"
            )

            precautions.forEach { precaution ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = precaution,
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Electrode Quick Guide (Default Section)
// ════════════════════════════════════════════════════════════════

@Composable
fun ElectrodeQuickGuide() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sensors,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Quick Electrode Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Electrode positions diagram
            val positions = listOf(
                Triple("Fp1 / Fp2", "Frontal Pole", "Concentration & intention signals"),
                Triple("C3 / C4", "Central", "Motor imagery (left/right hand)"),
                Triple("P3 / P4", "Parietal", "Spatial awareness & navigation"),
                Triple("O1 / O2", "Occipital", "Visual processing & alpha waves")
            )

            positions.forEach { (label, region, function) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GreenPrimary.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, GreenPrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.take(2),
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$label — $region",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = function,
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Getting Started Step Card
// ════════════════════════════════════════════════════════════════

@Composable
fun GettingStartedStepCard(step: ElectrodePlacementStep) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(BlueAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${step.stepNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    step.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    step.description,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Common Issues Section (Default)
// ════════════════════════════════════════════════════════════════

@Composable
fun CommonIssuesSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Common Issues",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val issues = listOf(
                Pair("Weak signal?", "Check electrode contact and apply more gel"),
                Pair("No connection?", "Ensure Bluetooth is on and device is charged"),
                Pair("Noisy signal?", "Move away from electronic devices and power lines"),
                Pair("Electrode falling off?", "Clean skin with alcohol wipe before placement")
            )

            issues.forEach { (issue, fix) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        tint = RedAlert,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            issue,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            fix,
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  FAQ Card
// ════════════════════════════════════════════════════════════════

@Composable
fun FaqCard(faq: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.QuestionMark,
                        contentDescription = null,
                        tint = Color(0xFFAB47BC),
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color(0xFFAB47BC).copy(alpha = 0.15f),
                                CircleShape
                            )
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = faq.question,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Category: ${faq.category}",
                        color = Color(0xFFAB47BC).copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Troubleshoot Card
// ════════════════════════════════════════════════════════════════

@Composable
fun TroubleshootCard(item: TroubleshootItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = RedAlert,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        RedAlert.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.problem,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.solution,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Safety Guideline Card
// ════════════════════════════════════════════════════════════════

@Composable
fun SafetyGuidelineCard(guideline: SafetyGuideline) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = guideline.severity.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            1.dp,
            guideline.severity.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                guideline.severity.icon,
                contentDescription = null,
                tint = guideline.severity.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = guideline.title,
                    color = guideline.severity.color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = guideline.description,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Contact Support Section
// ════════════════════════════════════════════════════════════════

@Composable
fun ContactSupportSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),

    ) {
        val context = LocalContext.current
        // Email Support
        // Email Card
        ContactMethodCard(
            icon = Icons.Default.Email,
            title = "Email Us",
            subtitle = "neurodrive86@gmail.com",
            description = "We'll respond within 24 hours",
            color = Color(0xFF4CAF50),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:neurodrive.supportcenter@gmail.com") // Fixed: added @ symbol
                    putExtra(Intent.EXTRA_SUBJECT, "NeuroDrive Feedback")
                    putExtra(Intent.EXTRA_TEXT, "Hello NeuroDrive Team,\n\n")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Handle case where no email app is installed
                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                }
            }
        )

       /* // Live Chat
        ContactMethodCard(
            icon = Icons.Default.Chat,
            title = "Live Chat",
            subtitle = "Available 9 AM - 6 PM IST",
            description = "Chat with our support team in real-time",
            color = GreenPrimary
        )*/

        // Phone

        // Phone Card
        ContactMethodCard(
            icon = Icons.Default.Phone,
            title = "Call Us",
            subtitle = "04862 294 561",
            description = "Available Mon-Fri, 9AM-6PM",
            color = Color(0xFF2196F3),
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:04862 294 561")
                }
                context.startActivity(intent)
            }

                    /*
                    // Documentation
                    ContactMethodCard(
                        icon = Icons.Default.MenuBook,
                        title = "Documentation",
                        subtitle = "docs.neurodrive.com",
                        description = "Detailed technical documentation and API reference",
                        color = RedAlert
                    )

                    // Community
                    ContactMethodCard(
                        icon = Icons.Default.Forum,
                        title = "Community Forum",
                        subtitle = "community.neurodrive.com",
                        description = "Connect with other NeuroDrive users",
                        color = Color(0xFFAB47BC)
                    )

                    // Bug Report
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = RedAlert.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = RedAlert,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Report a Bug",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Help us improve NeuroDrive",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }*/
        )

        // App Version
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NeuroDrive v1.0.0 (Build 1)",
            color = Color.White.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "© 2025 NeuroDrive Technologies",
            color = Color.White.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ContactMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    color: Color,
    onClick: () -> Unit = {} // Added onClick parameter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }, // Added clickable modifier
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  DATA PROVIDERS
// ════════════════════════════════════════════════════════════════

fun getElectrodePlacementSteps(): List<ElectrodePlacementStep> {
    return listOf(
        ElectrodePlacementStep(
            stepNumber = 1,
            title = "Prepare Your Scalp",
            description = "Clean the electrode placement areas on your scalp using an alcohol " +
                    "swab. This removes oils and dead skin cells that can interfere with " +
                    "signal quality. Let the area dry completely before proceeding.",
            precaution = "Do NOT use alcohol wipes on broken, irritated, or sunburned skin. " +
                    "If you have any scalp conditions (psoriasis, eczema, open wounds), " +
                    "consult your doctor before using electrodes.",
            imageRes = null, // Replace with: R.drawable.step1_clean_scalp
            tips = listOf(
                "Use 70% isopropyl alcohol wipes for best results",
                "Part your hair at the electrode sites for better contact",
                "Wait 30 seconds after cleaning for the alcohol to evaporate",
                "Avoid using hair products (gel, spray, oil) on placement day"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 2,
            title = "Apply Conductive Gel",
            description = "Apply a small amount (pea-sized) of conductive electrode gel to " +
                    "each electrode. The gel creates a bridge between your scalp and the " +
                    "electrode, dramatically improving signal quality.",
            precaution = "Use only medical-grade conductive gel. Never use regular hair gel, " +
                    "lotion, or water as substitutes. Check the expiry date on the gel. " +
                    "Avoid getting gel in your eyes.",
            imageRes = null, // Replace with: R.drawable.step2_apply_gel
            tips = listOf(
                "Too much gel can cause signals to short-circuit between electrodes",
                "Too little gel will result in weak or noisy signals",
                "Store gel at room temperature — cold gel may be uncomfortable",
                "Saline-based gel is gentler on sensitive skin"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 3,
            title = "Locate Fp1 & Fp2 (Frontal)",
            description = "The frontal pole electrodes go on your forehead. Measure 10% of the " +
                    "distance from your nasion (bridge of nose) to inion (bump at back of " +
                    "head). Place Fp1 on the left side and Fp2 on the right side, each " +
                    "about 5% of the head circumference from the center.",
            precaution = "Ensure electrodes are symmetrical. Asymmetric placement can lead to " +
                    "incorrect signal interpretation. Do not place electrodes directly on " +
                    "your eyebrows or too close to the hairline.",
            imageRes = null, // Replace with: R.drawable.step3_frontal
            tips = listOf(
                "Use a measuring tape for accurate 10-20 system placement",
                "The midpoint between Fp1 and Fp2 should be directly above your nose",
                "These electrodes capture concentration and decision-making signals",
                "Mark positions with a washable skin marker before attaching"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 4,
            title = "Locate C3 & C4 (Central/Motor)",
            description = "The central electrodes capture motor imagery signals. Find the vertex " +
                    "(Cz) — the topmost point of your head, midway between nasion and inion. " +
                    "C3 is 20% to the left of Cz, and C4 is 20% to the right. These are " +
                    "the most important electrodes for vehicle control.",
            precaution = "These electrodes are CRITICAL for motor control signals. Poor placement " +
                    "of C3/C4 will significantly reduce your ability to control the vehicle. " +
                    "Double-check measurements before securing.",
            imageRes = null, // Replace with: R.drawable.step4_central
            tips = listOf(
                "C3 controls RIGHT-side movements (contralateral)",
                "C4 controls LEFT-side movements (contralateral)",
                "Hair at the crown may need extra parting for good contact",
                "These electrodes detect mu rhythm (8-13 Hz) for motor imagery"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 5,
            title = "Locate P3 & P4 (Parietal)",
            description = "Parietal electrodes are placed behind the central electrodes. They " +
                    "capture spatial awareness and P300 signals. Measure 20% of the " +
                    "nasion-to-inion distance behind Cz, then 20% left (P3) and right (P4) " +
                    "of the midline.",
            precaution = "Parietal placement is often confused with occipital placement. Make " +
                    "sure you're measuring from Cz BACKWARD, not from the inion forward. " +
                    "Incorrect placement will capture wrong signal types.",
            imageRes = null, // Replace with: R.drawable.step5_parietal
            tips = listOf(
                "P3/P4 electrodes help with spatial navigation commands",
                "These capture P300 event-related potentials",
                "Useful for attention-based BCI paradigms",
                "Cross-verify with ear-to-ear measurements for accuracy"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 6,
            title = "Place Reference & Ground Electrodes",
            description = "The reference electrode goes on the earlobe (A1 or A2) or the " +
                    "mastoid bone (behind the ear). The ground electrode is typically " +
                    "placed on the forehead (AFz) or the other earlobe. These are " +
                    "essential for noise cancellation.",
            precaution = "Reference and ground electrodes MUST have excellent contact. " +
                    "Poor reference contact will add noise to ALL channels. " +
                    "Clean earlobes thoroughly — they tend to have more oil buildup.",
            imageRes = null, // Replace with: R.drawable.step6_reference
            tips = listOf(
                "Clip-on ear electrodes work well for earlobe reference",
                "Linked earlobes (A1+A2) provide a more stable reference",
                "The ground electrode helps reject common-mode noise (50/60 Hz)",
                "If using mastoid reference, place directly on the bone, not on hair"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 7,
            title = "Secure All Electrodes",
            description = "Once all electrodes are positioned, secure them using the headband " +
                    "or cap. Ensure each electrode maintains firm but comfortable contact " +
                    "with the scalp. The headband should be snug but not tight enough to " +
                    "cause discomfort.",
            precaution = "Do not over-tighten the headband — this can cause headaches and " +
                    "reduce blood flow to the scalp, potentially affecting signals. " +
                    "If you feel any pressure points, adjust immediately.",
            imageRes = null, // Replace with: R.drawable.step7_secure
            tips = listOf(
                "Check that no electrode has shifted during cap placement",
                "Gently wiggle each electrode to ensure gel contact",
                "The cap should feel secure when you move your head",
                "Re-apply gel if any electrode feels loose after cap placement"
            )
        ),
        ElectrodePlacementStep(
            stepNumber = 8,
            title = "Verify Signal Quality",
            description = "Open the NeuroDrive app and go to Signal Monitor. Check that all " +
                    "channels show clean, low-noise signals. Green indicators mean good " +
                    "contact. Yellow means acceptable. Red means the electrode needs " +
                    "repositioning or more gel.",
            precaution = "Do NOT start driving until ALL electrodes show green or yellow " +
                    "status. Red electrodes indicate unreliable signal quality and may " +
                    "lead to incorrect control commands. This is a SAFETY requirement.",
            imageRes = null, // Replace with: R.drawable.step8_verify
            tips = listOf(
                "Impedance below 5 kΩ is ideal (green)",
                "Impedance 5-10 kΩ is acceptable (yellow)",
                "Impedance above 10 kΩ requires repositioning (red)",
                "Blink your eyes to verify Fp1/Fp2 are detecting signals",
                "Imagine moving your hands to verify C3/C4 motor signals"
            )
        )
    )
}

fun getGettingStartedSteps(): List<ElectrodePlacementStep> {
    return listOf(
        ElectrodePlacementStep(
            stepNumber = 1,
            title = "Create Your Account",
            description = "Sign up with your email and verify it using the OTP code. " +
                    "Choose your role (Driver, Researcher, Developer) to customize your experience.",
            precaution = "Use a valid email — you'll need it for account recovery."
        ),
        ElectrodePlacementStep(
            stepNumber = 2,
            title = "Connect Your BCI Device",
            description = "Turn on your EEG headset and enable Bluetooth on your phone. " +
                    "Go to Settings → Device Connection and pair your device.",
            precaution = "Ensure your device is fully charged before first use."
        ),
        ElectrodePlacementStep(
            stepNumber = 3,
            title = "Place Electrodes",
            description = "Follow the Electrode Placement Guide to properly position all " +
                    "electrodes on your scalp. This is the most important step for accuracy.",
            precaution = "Take your time — rushing electrode placement leads to poor signals."
        ),
        /*ElectrodePlacementStep(
            stepNumber = 4,
            title = "Run Calibration",
            description = "Go to Calibration in the app. Follow the on-screen instructions to " +
                    "calibrate the system to your unique brain signals. This takes about 10 minutes.",
            precaution = "Stay relaxed and focused during calibration. Avoid distractions."
        )*/
        ElectrodePlacementStep(
            stepNumber = 4,
            title = "Practice in Simulator",
            description = "Use the built-in simulator to practice basic commands: Forward, " +
                    "Stop, Left, Right. Get comfortable before connecting to a real vehicle.",
            precaution = "Practice until you achieve 90%+ accuracy before real driving."
        ),
        ElectrodePlacementStep(
            stepNumber = 5,
            title = "Start Driving!",
            description = "Connect to your vehicle, verify all signals are green, and begin " +
                    "your NeuroDrive experience. Start with slow speeds in a safe area.",
            precaution = "ALWAYS have a safety co-driver and emergency override system active."
        )
    )
}

fun getFaqItems(): List<FaqItem> {
    return listOf(
        FaqItem(
            question = "What is NeuroDrive?",
            answer = "NeuroDrive is a Brain-Computer Interface (BCI) application that allows " +
                    "you to control a vehicle using your brain signals (EEG). It uses " +
                    "advanced signal processing and machine learning to interpret your " +
                    "intentions and translate them into driving commands.",
            category = "General"
        ),
        FaqItem(
            question = "Is it safe to drive with brain signals?",
            answer = "NeuroDrive includes multiple safety features: emergency override, " +
                    "speed limiting, obstacle detection, and a mandatory co-driver requirement. " +
                    "The system also has fail-safe mechanisms that stop the vehicle if signal " +
                    "quality drops below safe thresholds.",
            category = "Safety"
        ),
        FaqItem(
            question = "What EEG devices are compatible?",
            answer = "NeuroDrive supports OpenBCI (Cyton, Ganglion), Emotiv EPOC+/FLEX, " +
                    "NeuroSky MindWave, Muse 2/S, and any device that supports LSL " +
                    "(Lab Streaming Layer) protocol. Check our compatibility list for details.",
            category = "Hardware"
        ),
        FaqItem(
            question = "How accurate is the brain signal detection?",
            answer = "With proper electrode placement and calibration, NeuroDrive achieves " +
                    "85-95% accuracy for basic commands (forward, stop, left, right). " +
                    "Accuracy improves with practice and consistent use as the ML model " +
                    "adapts to your unique brain patterns.",
            category = "Technical"
        ),
        FaqItem(
            question = "How long does calibration take?",
            answer = "Initial calibration takes about 10-15 minutes. You'll be asked to " +
                    "imagine specific movements and focus on visual cues. Recalibration " +
                    "(recommended weekly) takes about 5 minutes.",
            category = "Setup"
        ),
        FaqItem(
            question = "Can I use NeuroDrive if I have a medical condition?",
            answer = "If you have epilepsy, seizure disorders, or a pacemaker, consult your " +
                    "physician before using NeuroDrive. The flashing visual stimuli used in " +
                    "some BCI paradigms may trigger seizures in photosensitive individuals.",
            category = "Safety"
        ),
        FaqItem(
            question = "Why is my signal quality poor?",
            answer = "Common causes: 1) Dirty scalp (clean with alcohol), 2) Insufficient gel, " +
                    "3) Hair between electrode and scalp, 4) Loose electrode contact, " +
                    "5) Electronic interference nearby, 6) Dried-out gel (reapply fresh gel).",
            category = "Troubleshooting"
        ),
        FaqItem(
            question = "How do I clean the electrodes?",
            answer = "After each use: 1) Remove electrodes gently, 2) Rinse with warm water, " +
                    "3) Use a soft brush to remove dried gel, 4) Pat dry with a lint-free cloth, " +
                    "5) Store in provided case. Never use harsh chemicals or submerge electronics.",
            category = "Maintenance"
        ),
        FaqItem(
            question = "What is motor imagery?",
            answer = "Motor imagery is the mental process of imagining movement without actually " +
                    "moving. For example, imagining clenching your right fist activates the left " +
                    "motor cortex (C3). NeuroDrive detects these brain patterns to generate " +
                    "control commands.",
            category = "Technical"
        ),
        FaqItem(
            question = "Can multiple users share one device?",
            answer = "Yes, but each user must create their own profile and run their own " +
                    "calibration. Brain patterns are unique to each individual, so calibration " +
                    "data is not transferable between users.",
            category = "General"
        )
    )
}

fun getTroubleshootItems(): List<TroubleshootItem> {
    return listOf(
        TroubleshootItem(
            problem = "Bluetooth device not found",
            solution = "1. Ensure the EEG headset is powered on and in pairing mode\n" +
                    "2. Turn Bluetooth off and on again on your phone\n" +
                    "3. Move closer to the device (within 5 meters)\n" +
                    "4. Remove the device from paired list and re-pair\n" +
                    "5. Restart both the headset and your phone",
            icon = Icons.Default.BluetoothDisabled
        ),
        TroubleshootItem(
            problem = "Weak or noisy EEG signals",
            solution = "1. Recheck electrode placement using the placement guide\n" +
                    "2. Reapply conductive gel (old gel may have dried)\n" +
                    "3. Clean electrode contact points with alcohol\n" +
                    "4. Move away from power lines and electronic devices\n" +
                    "5. Ensure reference/ground electrodes have good contact\n" +
                    "6. Check impedance in the Signal Monitor screen",
            icon = Icons.Default.SignalCellularConnectedNoInternet4Bar
        ),
        TroubleshootItem(
            problem = "App crashes or freezes",
            solution = "1. Force stop the app and reopen it\n" +
                    "2. Clear the app cache (Settings → Apps → NeuroDrive → Clear Cache)\n" +
                    "3. Ensure your phone has at least 2GB free RAM\n" +
                    "4. Update the app to the latest version\n" +
                    "5. If persistent, uninstall and reinstall the app",
            icon = Icons.Default.ErrorOutline
        ),
        TroubleshootItem(
            problem = "Calibration fails or gives poor results",
            solution = "1. Ensure you're in a quiet, distraction-free environment\n" +
                    "2. Verify ALL electrodes show green signal quality\n" +
                    "3. Stay relaxed — muscle tension creates noise\n" +
                    "4. Focus clearly on the visual cues during calibration\n" +
                    "5. Try calibrating at a different time of day (alertness matters)",
            icon = Icons.Default.Tune
        ),
        TroubleshootItem(
            problem = "Commands are delayed or inaccurate",
            solution = "1. Check your Bluetooth connection stability\n" +
                    "2. Recalibrate the system (brain patterns change over time)\n" +
                    "3. Ensure electrode gel hasn't dried out\n" +
                    "4. Reduce the command threshold in Settings\n" +
                    "5. Practice motor imagery exercises for better signal clarity",
            icon = Icons.Default.Speed
        ),
        TroubleshootItem(
            problem = "Vehicle not responding to commands",
            solution = "1. Verify the vehicle is in BCI control mode\n" +
                    "2. Check the vehicle's CAN bus connection\n" +
                    "3. Ensure the NeuroDrive hardware module is powered\n" +
                    "4. Verify signal quality in the Control screen\n" +
                    "5. Check if emergency override is accidentally engaged",
            icon = Icons.Default.DirectionsCar
        )
    )
}

fun getSafetyGuidelines(): List<SafetyGuideline> {
    return listOf(
        SafetyGuideline(
            title = "Always Have a Safety Co-Driver",
            description = "Never operate a BCI-controlled vehicle alone. A trained co-driver " +
                    "must be present with access to manual controls and emergency override " +
                    "at all times.",
            severity = SafetySeverity.CRITICAL
        ),
        SafetyGuideline(
            title = "Emergency Override System",
            description = "The vehicle must have a functioning emergency override system that " +
                    "can take control instantly. Test this system before every driving session.",
            severity = SafetySeverity.CRITICAL
        ),
        SafetyGuideline(
            title = "Signal Quality Threshold",
            description = "Never start driving with any electrode showing RED signal quality. " +
                    "All channels must be GREEN or YELLOW before engaging vehicle control.",
            severity = SafetySeverity.CRITICAL
        ),
        SafetyGuideline(
            title = "Speed Limitations",
            description = "BCI-controlled driving is currently limited to 30 km/h in controlled " +
                    "environments. Do not attempt to override speed limits.",
            severity = SafetySeverity.WARNING
        ),
        SafetyGuideline(
            title = "Fatigue Monitoring",
            description = "The system monitors your alertness level. If drowsiness is detected, " +
                    "the vehicle will safely stop. Take breaks every 30 minutes.",
            severity = SafetySeverity.WARNING
        ),
        SafetyGuideline(
            title = "Medical Conditions",
            description = "Consult your physician before using NeuroDrive if you have epilepsy, " +
                    "seizure disorders, heart conditions, or a pacemaker.",
            severity = SafetySeverity.WARNING
        ),
        SafetyGuideline(
            title = "Environmental Conditions",
            description = "Avoid BCI driving in heavy rain, fog, or extreme temperatures. " +
                    "These conditions can affect both signal quality and driving safety.",
            severity = SafetySeverity.WARNING
        ),
        SafetyGuideline(
            title = "Electrode Care",
            description = "Replace electrodes if they show signs of degradation. Damaged " +
                    "electrodes can cause skin irritation and provide unreliable signals.",
            severity = SafetySeverity.INFO
        ),
        SafetyGuideline(
            title = "Data Privacy",
            description = "Your brain signal data is encrypted and stored securely. You can " +
                    "delete your data at any time from Settings → Privacy → Delete My Data.",
            severity = SafetySeverity.INFO
        ),
        SafetyGuideline(
            title = "Regular Maintenance",
            description = "Clean electrodes after each use, recalibrate weekly, and update the " +
                    "app regularly for the best and safest experience.",
            severity = SafetySeverity.INFO
        )
    )
}