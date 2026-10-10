package com.example.starborn.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.ui.arcade.*
import com.example.starborn.feature.arcade.domain.ArcadeIds
import java.awt.event.KeyEvent

internal data class CabinetInfo(
    val id: String,
    val cabinetIndex: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val accentColor: Color,
    val marqueeColor: Color,
    val bannerIcon: String,
    val description: String,
    val gameplayRules: List<String>,
    val controlsHint: String,
    val requiredPart: String,
    val bronzeScore: Int,
    val silverScore: Int,
    val goldScore: Int
)

private val AllCabinets = listOf(
    CabinetInfo(
        id = ArcadeIds.DEEP_MINE,
        cabinetIndex = "01",
        title = "Deep Mine: Asteroid Drill",
        subtitle = "Sub-Surface Core Extraction",
        genre = "MINING / DIGGER",
        accentColor = Color(0xFFFFB300),
        marqueeColor = Color(0xFF2A1C00),
        bannerIcon = "⛏",
        description = "Drill through dense asteroid crusts to unearth raw hyperion ore and crystalline geodes. Manage your drill temperature and evade shifting bedrock fissures.",
        gameplayRules = listOf(
            "Navigate drill head through rock strata and collect valuable mineral deposits.",
            "Dodge unstable bedrock boulders and tectonic magma vents.",
            "Monitor overheat gauges — cooling jets vent after high-speed chain cuts."
        ),
        controlsHint = "WASD / Arrow Keys to steer drill · Space to turbo drill",
        requiredPart = "Hyperion Logic Board",
        bronzeScore = 5_000,
        silverScore = 15_000,
        goldScore = 30_000
    ),
    CabinetInfo(
        id = ArcadeIds.CANOPY_HOPPER,
        cabinetIndex = "02",
        title = "Canopy Hopper",
        subtitle = "Bioluminescent Swamp Crosser",
        genre = "RETRO HOPPER",
        accentColor = Color(0xFF00FF9D),
        marqueeColor = Color(0xFF051C12),
        bannerIcon = "🐸",
        description = "Guide the bog hopper through hazardous marshlands. Leap over predator thickets, balance on drifting river logs, avoid volt eels, and populate all 5 canopy nests.",
        gameplayRules = listOf(
            "Hop across predator lanes and fast-flowing swamp river logs.",
            "Collect golden spores to ramp up score multipliers.",
            "Fill all 5 canopy nests before toxic miasma fills the swamp."
        ),
        controlsHint = "WASD / Arrow Keys to hop · Space for leap boost",
        requiredPart = "Sector-9 Optic Board",
        bronzeScore = 4_000,
        silverScore = 12_000,
        goldScore = 25_000
    ),
    CabinetInfo(
        id = ArcadeIds.SPIRE_INFILTRATOR,
        cabinetIndex = "03",
        title = "Spire Infiltrator",
        subtitle = "Tactical Stealth Cyber-Heist",
        genre = "CYBER STEALTH",
        accentColor = Color(0xFFE040FB),
        marqueeColor = Color(0xFF1D042B),
        bannerIcon = "🕵",
        description = "Ascend through corporate surveillance spires. Avoid patrol drones, hack terminal relays, extract encrypted data packets, and reach the extraction pad.",
        gameplayRules = listOf(
            "Stay out of detection cones of roving security sentinels.",
            "Access floor terminal nodes to unlock encrypted elevator shafts.",
            "Upload ghost payloads before mainframe alert levels peak."
        ),
        controlsHint = "WASD / Arrow Keys to sneak · Space to hack terminal",
        requiredPart = "Hyperion Mainframe Chip",
        bronzeScore = 6_000,
        silverScore = 18_000,
        goldScore = 35_000
    ),
    CabinetInfo(
        id = ArcadeIds.SLAG_CATCHER,
        cabinetIndex = "04",
        title = "Slag Catcher",
        subtitle = "Foundry Smelter Overdrive",
        genre = "REACTION CATCHER",
        accentColor = Color(0xFFFF6D00),
        marqueeColor = Color(0xFF280B00),
        bannerIcon = "🔥",
        description = "Operate the volcanic magma intake crucible. Catch high-yield molten slag cores while deflecting volatile thermite sparks to prevent blast furnace collapse.",
        gameplayRules = listOf(
            "Position ladle along foundry rails to capture falling molten cores.",
            "Deflect volatile red cinder sparks with quick reverse slides.",
            "Chain consecutive catches to activate hyper-cooling purge cycles."
        ),
        controlsHint = "A / D or Left / Right to move ladle · Space to vent slag",
        requiredPart = "Thermite Logic Gate",
        bronzeScore = 5_000,
        silverScore = 15_000,
        goldScore = 30_000
    ),
    CabinetInfo(
        id = ArcadeIds.ORBITAL_DEFENSE,
        cabinetIndex = "05",
        title = "Orbital Defense",
        subtitle = "Astra Station Planetary Interceptor",
        genre = "SPACE SHMUP",
        accentColor = Color(0xFF00E5FF),
        marqueeColor = Color(0xFF031A26),
        bannerIcon = "🚀",
        description = "Defend Astra Station from incoming asteroid barrages and rogue raider drones. Align orbital pulse cannons, lead interceptor volleys, and deploy shield bursts.",
        gameplayRules = listOf(
            "Rotate orbital battery to track incoming ballistic trajectories.",
            "Time railgun detonations to catch cluster warheads in chain explosions.",
            "Conserve thermal energy to prevent emergency turret lockdown."
        ),
        controlsHint = "A / D or Left / Right to rotate cannon · Space to fire",
        requiredPart = "Zenith Telemetry Array",
        bronzeScore = 7_500,
        silverScore = 20_000,
        goldScore = 40_000
    ),
    CabinetInfo(
        id = ArcadeIds.HARMONIC_PULSE,
        cabinetIndex = "06",
        title = "Harmonic Pulse",
        subtitle = "Prismatic Resonance Waveform",
        genre = "RHYTHM RESONANCE",
        accentColor = Color(0xFF7C4DFF),
        marqueeColor = Color(0xFF13082E),
        bannerIcon = "⚡",
        description = "Tune into ancient precursor quantum frequencies. Harmonize pulse waves across chromatic tracks to decrypt celestial telemetry signals before interference dissolves the link.",
        gameplayRules = listOf(
            "Match oscillating frequency pulses across chromatic resonance lanes.",
            "Execute synchronized beats to maintain harmonic coherence multipliers.",
            "Recover phased drift waves before dissonance destabilizes the receiver."
        ),
        controlsHint = "1 - 4 or Arrow Keys for channel · Space to resonate",
        requiredPart = "Prismatic Tuning Fork",
        bronzeScore = 10_000,
        silverScore = 25_000,
        goldScore = 50_000
    )
)

@Composable
fun DesktopArcadeScreen(services: DesktopAppServices, onClose: () -> Unit) {
    val session by services.sessionStore.state.collectAsState()
    val settings by services.userSettingsStore.settings.collectAsState(initial = com.example.starborn.data.local.UserSettings())
    val inventory by services.inventoryService.state.collectAsState()

    var playing by remember {
        mutableStateOf(services.activeArcadeCabinet?.takeIf { services.arcadeService.progress(it).repaired })
    }
    var selectedIndex by remember { mutableStateOf(0) }

    val back = { playing = null }
    val cue: (String) -> Unit = { cueName ->
        services.audioDriver.executeAll(services.audioRouter.commandsForUi(cueName))
    }

    // Keyboard navigation when in Arcade selector
    if (playing == null) {
        DesktopArcadeKeys { keyCode, pressed ->
            if (pressed) {
                when (keyCode) {
                    KeyEvent.VK_UP, KeyEvent.VK_W -> {
                        selectedIndex = (selectedIndex - 1 + AllCabinets.size) % AllCabinets.size
                        cue("action_inspect")
                    }
                    KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                        selectedIndex = (selectedIndex + 1) % AllCabinets.size
                        cue("action_inspect")
                    }
                    KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                        val selected = AllCabinets.getOrNull(selectedIndex)
                        if (selected != null && session.arcadeProgress[selected.id]?.repaired == true) {
                            playing = selected.id
                            cue("confirm")
                        }
                    }
                    KeyEvent.VK_ESCAPE -> {
                        onClose()
                    }
                }
            }
        }
    }

    when (playing) {
        ArcadeIds.DEEP_MINE -> DeepMineArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.CANOPY_HOPPER -> CanopyHopperArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.SPIRE_INFILTRATOR -> SpireInfiltratorArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.SLAG_CATCHER -> SlagCatcherArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.ORBITAL_DEFENSE -> OrbitalDefenseArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        ArcadeIds.HARMONIC_PULSE -> HarmonicPulseArcadeScreen(services.arcadeService, back, settings.largeTouchTargets, settings.disableFlashes, cue)
        else -> {
            val selectedCabinet = AllCabinets.getOrElse(selectedIndex) { AllCabinets.first() }
            val selectedProgress = session.arcadeProgress[selectedCabinet.id]
            val isSelectedRepaired = selectedProgress?.repaired == true

            val onlineCount = AllCabinets.count { session.arcadeProgress[it.id]?.repaired == true }
            val tokenCount = inventory.firstOrNull { it.item.id == ArcadeIds.TOKEN_ITEM_ID }?.quantity ?: 0
            val totalScore = AllCabinets.sumOf { session.arcadeProgress[it.id]?.highScore ?: 0 }

            val arcadeBackdrop = rememberDesktopAssetPainter(
                "images/rooms/astra/common_room_arcade_v1.webp",
                services.assetProvider
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050B14))
            ) {
                // Background artwork with vignette overlay
                Image(
                    painter = arcadeBackdrop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.28f
                )

                // Scanline and phosphor atmospheric raster
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val scanlineSpacing = 4.dp.toPx()
                            var y = 0f
                            while (y < size.height) {
                                drawLine(
                                    color = Color(0x1200E5FF),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                                y += scanlineSpacing
                            }
                        }
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xE0050B14),
                                    Color(0xF007111D),
                                    Color(0xFA040810)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    // Top Header Bar
                    ArcadeHeaderBar(
                        onlineCount = onlineCount,
                        totalCabinets = AllCabinets.size,
                        tokenCount = tokenCount,
                        totalScore = totalScore,
                        onClose = onClose
                    )

                    Spacer(Modifier.height(16.dp))

                    // Main Content: 2-Column Desktop Parlor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Column: Cabinet Selection List
                        CabinetSelectorColumn(
                            cabinets = AllCabinets,
                            sessionProgress = session.arcadeProgress,
                            selectedIndex = selectedIndex,
                            onSelectIndex = { index ->
                                selectedIndex = index
                                cue("action_inspect")
                            },
                            onLaunchCabinet = { id ->
                                playing = id
                                cue("confirm")
                            },
                            modifier = Modifier
                                .width(380.dp)
                                .fillMaxHeight()
                        )

                        // Right Column: Showcase & CRT Launch Deck
                        CabinetShowcaseDeck(
                            cabinet = selectedCabinet,
                            progress = selectedProgress,
                            isRepaired = isSelectedRepaired,
                            onLaunch = {
                                playing = selectedCabinet.id
                                cue("confirm")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Bottom Hotkey Strip
                    ArcadeBottomKeyLegend(
                        isSelectedRepaired = isSelectedRepaired,
                        onPlayClick = {
                            if (isSelectedRepaired) {
                                playing = selectedCabinet.id
                                cue("confirm")
                            }
                        },
                        onCloseClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcadeHeaderBar(
    onlineCount: Int,
    totalCabinets: Int,
    tokenCount: Int,
    totalScore: Int,
    onClose: () -> Unit
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xDD091522),
        border = BorderStroke(1.5.dp, Color(0xFF1E384D)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Lore badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10283B),
                    border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "ARCADE",
                        color = Color(0xFF63E6FF),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Column {
                    Text(
                        text = "ASTRA RECREATION LOUNGE",
                        color = Color.White,
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Common Room Subsystem · Vintage Sector-9 Restoration Matrix",
                        color = Color(0xFF8FB0C4),
                        fontSize = 12.sp
                    )
                }
            }

            // Status Badges & Back Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Online count badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (onlineCount > 0) Color(0x2200FF9D) else Color(0x22FFB300),
                    border = BorderStroke(1.dp, if (onlineCount > 0) Color(0xFF00FF9D).copy(alpha = 0.6f) else Color(0xFFFFB300).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(if (onlineCount > 0) Color(0xFF00FF9D) else Color(0xFFFFB300))
                        )
                        Text(
                            text = "$onlineCount / $totalCabinets ONLINE",
                            color = if (onlineCount > 0) Color(0xFF00FF9D) else Color(0xFFFFB300),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Tokens badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x22FFD54F),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🪙 $tokenCount TOKENS",
                            color = Color(0xFFFFD54F),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Combined score
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x2263E6FF),
                    border = BorderStroke(1.dp, Color(0xFF63E6FF).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "★ %,d PTS".format(totalScore),
                            color = Color(0xFF63E6FF),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Close Button
                DesktopKeyBadge(
                    keyGlyph = "Esc",
                    label = "Back to Lounge",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun CabinetSelectorColumn(
    cabinets: List<CabinetInfo>,
    sessionProgress: Map<String, com.example.starborn.domain.session.ArcadeCabinetProgress>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onLaunchCabinet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xDD081421),
        border = BorderStroke(1.5.dp, Color(0xFF1B3245)),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CABINET DIRECTORY",
                    color = Color(0xFF8FB0C4),
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SELECT [W / S]",
                    color = Color(0xFF53768E),
                    fontFamily = fonts.orbitron,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(cabinets, key = { _, c -> c.id }) { index, cabinet ->
                    val progress = sessionProgress[cabinet.id]
                    val isRepaired = progress?.repaired == true
                    val isSelected = index == selectedIndex
                    val highScore = progress?.highScore ?: 0

                    CabinetDirectoryCard(
                        cabinet = cabinet,
                        isRepaired = isRepaired,
                        isSelected = isSelected,
                        highScore = highScore,
                        onClick = { onSelectIndex(index) },
                        onDoubleClick = {
                            if (isRepaired) onLaunchCabinet(cabinet.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CabinetDirectoryCard(
    cabinet: CabinetInfo,
    isRepaired: Boolean,
    isSelected: Boolean,
    highScore: Int,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val fonts = LocalStarbornFonts.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        when {
            isSelected -> cabinet.accentColor
            isHovered -> cabinet.accentColor.copy(alpha = 0.6f)
            else -> Color(0xFF1E3244)
        }
    )

    val cardBg = when {
        isSelected -> cabinet.accentColor.copy(alpha = 0.12f)
        isHovered -> Color(0xFF0F2233)
        else -> Color(0xFF0A1826)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = cardBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .desktopPointerHover()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("cabinet-card-${cabinet.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabinet Index & Icon
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) cabinet.accentColor.copy(alpha = 0.25f) else Color(0xFF112536),
                border = BorderStroke(1.dp, if (isSelected) cabinet.accentColor else Color(0xFF264660)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = cabinet.bannerIcon,
                        fontSize = 16.sp
                    )
                }
            }

            // Cabinet Title & Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cabinet.cabinetIndex,
                        color = if (isSelected) cabinet.accentColor else Color(0xFF6F92A8),
                        fontFamily = fonts.orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = cabinet.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cabinet.genre,
                        color = Color(0xFF7E9FB4),
                        fontSize = 10.sp,
                        fontFamily = fonts.orbitron
                    )
                    Text(
                        text = "·",
                        color = Color(0xFF456377),
                        fontSize = 10.sp
                    )
                    if (isRepaired) {
                        Text(
                            text = if (highScore > 0) "BEST: %,d".format(highScore) else "UNPLAYED",
                            color = if (highScore > 0) Color(0xFFFFD54F) else Color(0xFF8FB0C4),
                            fontSize = 10.sp,
                            fontFamily = fonts.orbitron
                        )
                    } else {
                        Text(
                            text = "NEEDS REPAIR",
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status Lamp
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isRepaired) Color(0x2200FF9D) else Color(0x22FF9800),
                border = BorderStroke(1.dp, if (isRepaired) Color(0xFF00FF9D).copy(alpha = 0.5f) else Color(0xFFFF9800).copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (isRepaired) "READY" else "OFFLINE",
                    color = if (isRepaired) Color(0xFF00FF9D) else Color(0xFFFF9800),
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun CabinetShowcaseDeck(
    cabinet: CabinetInfo,
    progress: com.example.starborn.domain.session.ArcadeCabinetProgress?,
    isRepaired: Boolean,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = LocalStarbornFonts.current
    val highScore = progress?.highScore ?: 0
    val playCount = progress?.playCount ?: 0

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xEE081422),
        border = BorderStroke(2.dp, cabinet.accentColor.copy(alpha = 0.6f)),
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabinet Marquee Header
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cabinet.marqueeColor,
                border = BorderStroke(1.5.dp, cabinet.accentColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "CABINET #${cabinet.cabinetIndex}",
                                color = cabinet.accentColor,
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = cabinet.accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, cabinet.accentColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = cabinet.genre,
                                    color = cabinet.accentColor,
                                    fontFamily = fonts.orbitron,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = cabinet.title,
                            color = Color.White,
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        Text(
                            text = cabinet.subtitle,
                            color = Color(0xFFC7DEEC),
                            fontSize = 12.sp
                        )
                    }

                    // High Score Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF08121B),
                        border = BorderStroke(1.5.dp, if (highScore > 0) Color(0xFFFFD54F) else Color(0xFF233B4D)),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "HIGH SCORE",
                                color = if (highScore > 0) Color(0xFFFFD54F) else Color(0xFF6F92A8),
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (highScore > 0) "%,d".format(highScore) else "—",
                                color = if (highScore > 0) Color.White else Color(0xFF6F92A8),
                                fontFamily = fonts.pressStart,
                                fontSize = 14.sp
                            )
                            if (playCount > 0) {
                                Text(
                                    text = "$playCount RUNS RECORDED",
                                    color = Color(0xFF789BB0),
                                    fontSize = 9.sp,
                                    fontFamily = fonts.orbitron
                                )
                            }
                        }
                    }
                }
            }

            // CRT Screen Viewport Preview & Game Synopsis
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF040A10),
                border = BorderStroke(1.5.dp, Color(0xFF1B3245)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Synopsis section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MISSION BRIEFING",
                            color = cabinet.accentColor,
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = cabinet.description,
                            color = Color(0xFFD6E6F2),
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFF142938))

                    // Rules & Tactical Objectives
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TACTICAL OBJECTIVES",
                            color = Color(0xFF8FB0C4),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        cabinet.gameplayRules.forEach { rule ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "▸",
                                    color = cabinet.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = rule,
                                    color = Color(0xFFB0CDDF),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF142938))

                    // Reward Tiers Breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SCORE TARGETS & TOKEN MILESTONES",
                            color = Color(0xFF8FB0C4),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RewardTierChip(
                                tierName = "BRONZE",
                                threshold = cabinet.bronzeScore,
                                claimed = highScore >= cabinet.bronzeScore,
                                accent = Color(0xFFCD7F32),
                                modifier = Modifier.weight(1f)
                            )
                            RewardTierChip(
                                tierName = "SILVER",
                                threshold = cabinet.silverScore,
                                claimed = highScore >= cabinet.silverScore,
                                accent = Color(0xFFC0C0C0),
                                modifier = Modifier.weight(1f)
                            )
                            RewardTierChip(
                                tierName = "GOLD",
                                threshold = cabinet.goldScore,
                                claimed = highScore >= cabinet.goldScore,
                                accent = Color(0xFFFFD700),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF142938))

                    // Desktop Controls Reference
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CABINET CONTROLS",
                            color = Color(0xFF8FB0C4),
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF09141F),
                            border = BorderStroke(1.dp, Color(0xFF1E394E))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⌨",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = cabinet.controlsHint,
                                    color = Color(0xFF9FC6DD),
                                    fontFamily = fonts.orbitron,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Deck: Launch vs Repair
            if (isRepaired) {
                Button(
                    onClick = onLaunch,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cabinet.accentColor,
                        contentColor = Color(0xFF040A10)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .desktopPointerHover()
                        .testTag("launch-cabinet-button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "▶",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "INSERT TOKEN & LAUNCH RUN [Enter]",
                            fontFamily = fonts.orbitron,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1A1308),
                    border = BorderStroke(1.5.dp, Color(0xFFFF9800)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚠",
                            fontSize = 22.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "CABINET OFFLINE — REPAIR REQUIRED",
                                color = Color(0xFFFF9800),
                                fontFamily = fonts.orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Requires: ${cabinet.requiredPart}. Repair the logic board at the Tinkering Workbench to restore terminal power.",
                                color = Color(0xFFFFE0B2),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardTierChip(
    tierName: String,
    threshold: Int,
    claimed: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val fonts = LocalStarbornFonts.current

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (claimed) accent.copy(alpha = 0.15f) else Color(0xFF09141F),
        border = BorderStroke(1.dp, if (claimed) accent else Color(0xFF1E3547)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (claimed) "✓" else "★",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tierName,
                    color = accent,
                    fontFamily = fonts.orbitron,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "%,d".format(threshold),
                color = if (claimed) Color.White else Color(0xFF7897AC),
                fontFamily = fonts.pressStart,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun ArcadeBottomKeyLegend(
    isSelectedRepaired: Boolean,
    onPlayClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC07111B),
        border = BorderStroke(1.dp, Color(0xFF182D3D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DesktopKeyBadge(
                    keyGlyph = "W / S or ↑ / ↓",
                    label = "Select Cabinet"
                )
                DesktopKeyBadge(
                    keyGlyph = "Enter / Space",
                    label = if (isSelectedRepaired) "Play Cabinet" else "Cabinet Locked",
                    highlighted = isSelectedRepaired,
                    enabled = isSelectedRepaired,
                    onClick = if (isSelectedRepaired) onPlayClick else null
                )
            }

            DesktopKeyBadge(
                keyGlyph = "Esc",
                label = "Exit Arcade",
                onClick = onCloseClick
            )
        }
    }
}
