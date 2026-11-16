package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.model.DriverProfile
import com.example.ui.model.GpsStopPoint
import com.example.ui.model.PreviewRole
import com.example.ui.model.VisitItem
import com.example.ui.theme.BubblesBackground
import com.example.ui.theme.BubblesCyan
import com.example.ui.theme.BubblesCyanBorder
import com.example.ui.theme.BubblesCyanBright
import com.example.ui.theme.BubblesCyanDark
import com.example.ui.theme.BubblesCyanSoft
import com.example.ui.theme.BubblesGray
import com.example.ui.theme.BubblesSlateDark
import com.example.ui.theme.BubblesWhite
import com.example.ui.theme.GlassCardFrosted
import com.example.ui.theme.GlassCardTranslucent
import com.example.ui.theme.GlassCardWhite
import com.example.ui.theme.GlassCyanTint
import com.example.ui.theme.GlassPillFloating
import com.example.ui.theme.GlassSpecHighlight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusIndigo
import com.example.ui.theme.StatusRed

@Composable
fun BubblesAtmosphericBackdrop(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "backdropFloat")
    val drift by infiniteTransition.animateFloat(
        initialValue = -22f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val bubbleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubbleDrift"
    )

    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(18.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF8FB),
                        BubblesBackground,
                        Color(0xFFF6FCFD)
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_bubbles_backdrop_1794774063466),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(blurMod)
                .alpha(0.68f)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Luminous cyan & indigo ambient orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BubblesCyanBright.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w * 0.84f, h * 0.14f + drift),
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = Offset(w * 0.84f, h * 0.14f + drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BubblesCyan.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.14f, h * 0.65f - drift),
                    radius = w * 0.56f
                ),
                radius = w * 0.56f,
                center = Offset(w * 0.14f, h * 0.65f - drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(StatusIndigo.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(w * 0.75f, h * 0.84f + bubbleDrift),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.75f, h * 0.84f + bubbleDrift)
            )

            // Crisp translucent soap-bubble glass rings for signature Bubbles brand depth
            val orbs = listOf(
                Triple(Offset(w * 0.18f, h * 0.22f + bubbleDrift), w * 0.11f, 0.24f),
                Triple(Offset(w * 0.86f, h * 0.46f - bubbleDrift), w * 0.08f, 0.22f),
                Triple(Offset(w * 0.52f, h * 0.11f - bubbleDrift * 0.5f), w * 0.055f, 0.26f),
                Triple(Offset(w * 0.26f, h * 0.86f + bubbleDrift * 0.7f), w * 0.09f, 0.20f)
            )
            orbs.forEach { (center, r, alpha) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = alpha * 0.65f), BubblesCyan.copy(alpha = alpha * 0.35f), Color.Transparent),
                        center = Offset(center.x - r * 0.28f, center.y - r * 0.28f),
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha + 0.16f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 2.2f)
                )
            }
        }
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = GlassCardWhite,
    borderBrush: Brush = Brush.linearGradient(
        listOf(GlassSpecHighlight, BubblesCyanBorder, Color.White.copy(alpha = 0.52f))
    ),
    shadowElevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val innerGlassBrush = Brush.verticalGradient(
        listOf(
            containerColor,
            containerColor.copy(alpha = (containerColor.alpha - 0.11f).coerceAtLeast(0.45f))
        )
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.25.dp, borderBrush),
            shadowElevation = shadowElevation
        ) {
            Column(
                modifier = Modifier.background(innerGlassBrush),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.25.dp, borderBrush),
            shadowElevation = shadowElevation
        ) {
            Column(
                modifier = Modifier.background(innerGlassBrush),
                content = content
            )
        }
    }
}

@Composable
fun GradientIconBadge(
    icon: ImageVector,
    accentColor: Color,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.22f),
                        accentColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun BubblesLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.linearGradient(listOf(BubblesCyanBright, BubblesCyan, BubblesCyanDark)),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.04f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.52f), Color.Transparent),
                center = Offset(w * 0.34f, h * 0.24f),
                radius = w * 0.36f
            ),
            radius = w * 0.36f,
            center = Offset(w * 0.34f, h * 0.24f)
        )
        drawCircle(color = Color.White, radius = w * 0.20f, center = Offset(w * 0.46f, h * 0.59f))
        drawCircle(color = Color.White.copy(alpha = 0.92f), radius = w * 0.11f, center = Offset(w * 0.66f, h * 0.34f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = w * 0.065f, center = Offset(w * 0.46f, h * 0.24f))
    }
}

@Composable
fun PulsingLiveDot(color: Color = StatusEmerald, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing)),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing)),
        label = "pulseAlpha"
    )

    Box(modifier = modifier.size(16.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun BubblesAppHeader(
    isAdminAccount: Boolean,
    role: PreviewRole,
    onRoleChange: (PreviewRole) -> Unit,
    onToggleLanguage: () -> Unit,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onSyncNow: () -> Unit,
    isCloudMode: Boolean = false
) {
    val s = LocalAppStrings.current
    Surface(
        color = GlassPillFloating,
        border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(GlassSpecHighlight, BubblesCyanBorder))),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BubblesLogoMark(modifier = Modifier.size(38.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Bubbles Laundry",
                                style = MaterialTheme.typography.titleMedium,
                                color = BubblesSlateDark,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAdminAccount) BubblesCyan.copy(alpha = 0.16f) else StatusEmerald.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (isAdminAccount) (if (s.isArabic) "إدارة" else "ADMIN") else (if (s.isArabic) "مندوب" else "DRIVER"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAdminAccount) BubblesCyanDark else StatusEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PulsingLiveDot(if (isOnline || !isCloudMode) StatusEmerald else StatusAmber)
                            Text(
                                text = when {
                                    isCloudMode && !isOnline -> if (s.isArabic) "غير متصل" else "Offline"
                                    isCloudMode -> if (s.isArabic) "متصل بالسحابة" else "Cloud Synced"
                                    else -> s.previewBanner
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline || !isCloudMode) BubblesCyanDark else StatusAmber
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(16.dp),
                        color = BubblesCyanSoft,
                        border = BorderStroke(1.dp, BubblesCyanBorder),
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = "Language",
                                tint = BubblesCyanDark,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (s.isArabic) "EN" else "AR",
                                style = MaterialTheme.typography.labelSmall,
                                color = BubblesCyanDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = GlassCyanTint) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.size(40.dp).testTag("notifications_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge(containerColor = StatusRed) { Text(unreadNotifications.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = s.tabNotifications,
                                    tint = BubblesSlateDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    if (isAdminAccount) {
                        Surface(shape = CircleShape, color = GlassCyanTint) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(40.dp).testTag("admin_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = s.settingsTitle,
                                    tint = BubblesSlateDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Surface(shape = CircleShape, color = StatusRed.copy(alpha = 0.10f)) {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.size(40.dp).testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = s.signOut,
                                tint = StatusRed,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            if (isCloudMode && (!isOnline || pendingSyncCount > 0)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (!isOnline) "${s.offlineBanner} ($pendingSyncCount)" else (if (s.isArabic) "بانتظار مزامنة $pendingSyncCount عنصر" else "$pendingSyncCount pending sync"),
                                style = MaterialTheme.typography.labelSmall,
                                color = BubblesSlateDark
                            )
                        }
                        if (isOnline && pendingSyncCount > 0) {
                            TextButton(onClick = onSyncNow) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.syncNow, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (isAdminAccount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE1F1F5).copy(alpha = 0.78f))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RolePill(
                        label = s.adminView,
                        icon = Icons.Rounded.AdminPanelSettings,
                        selected = role == PreviewRole.ADMIN,
                        onClick = { onRoleChange(PreviewRole.ADMIN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_admin_role")
                    )
                    RolePill(
                        label = s.driverView,
                        icon = Icons.Rounded.LocalShipping,
                        selected = role == PreviewRole.DRIVER,
                        onClick = { onRoleChange(PreviewRole.DRIVER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("switch_driver_role")
                    )
                }
            }
        }
    }
}

@Composable
private fun RolePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (selected) {
        Brush.horizontalGradient(listOf(BubblesCyan, BubblesCyanDark))
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val fg by animateColorAsState(if (selected) BubblesWhite else BubblesGray, label = "roleFg")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class GlassNavItem(
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun FloatingGlassBottomBar(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = GlassPillFloating,
            border = BorderStroke(
                1.3.dp,
                Brush.linearGradient(listOf(GlassSpecHighlight, BubblesCyanBorder, Color.White.copy(alpha = 0.65f)))
            ),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, item ->
                    val selected = idx == selectedIndex
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 1f,
                        animationSpec = spring(dampingRatio = 0.65f),
                        label = "navScale"
                    )
                    Surface(
                        onClick = { onSelect(idx) },
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .testTag(item.tag)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(
                                            listOf(BubblesCyan.copy(alpha = 0.22f), BubblesCyanDark.copy(alpha = 0.12f))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .then(
                                    if (selected) Modifier.border(1.dp, BubblesCyan.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
                                    else Modifier
                                )
                                .padding(vertical = 7.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) BubblesCyanDark else BubblesGray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) BubblesCyanDark else BubblesGray,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    sparklinePoints: List<Float> = listOf(0.35f, 0.55f, 0.45f, 0.72f, 0.65f, 0.92f),
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        cornerRadius = 24.dp,
        containerColor = GlassCardWhite,
        shadowElevation = 5.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIconBadge(
                    icon = icon,
                    accentColor = accentColor,
                    contentDescription = title,
                    size = 36.dp,
                    iconSize = 18.dp
                )
                Canvas(
                    modifier = Modifier
                        .width(62.dp)
                        .height(26.dp)
                ) {
                    if (sparklinePoints.size >= 2) {
                        val stepX = size.width / (sparklinePoints.size - 1)
                        val pts = sparklinePoints.mapIndexed { i, v ->
                            Offset(i * stepX, size.height * (1f - v.coerceIn(0.12f, 0.94f)))
                        }
                        val linePath = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 1 until pts.size) {
                                val prev = pts[i - 1]
                                val cur = pts[i]
                                val midX = (prev.x + cur.x) / 2f
                                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(pts.last().x, size.height)
                            lineTo(pts.first().x, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.28f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = accentColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                        drawCircle(color = BubblesWhite, radius = 5f, center = pts.last())
                        drawCircle(color = accentColor, radius = 3.2f, center = pts.last())
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = BubblesGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = BubblesSlateDark,
                fontWeight = FontWeight.Bold
            )
            if (badgeText != null) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompletionDonutChartCard(
    completed: Int,
    inProgress: Int,
    late: Int,
    unexecuted: Int,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val totalRaw = completed + inProgress + late + unexecuted
    val total = totalRaw.coerceAtLeast(1)
    val completionPct = if (totalRaw == 0) 0 else ((completed * 100f) / total).toInt()
    val animatedSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "donutSweep"
    )

    GlassSurface(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp, containerColor = GlassCardWhite) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(modifier = Modifier.size(102.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 15.dp.toPx()
                    drawCircle(
                        color = Color(0xFFDCEEF3),
                        style = Stroke(width = stroke)
                    )
                    if (totalRaw > 0) {
                        val segments = listOf(
                            completed to StatusEmerald,
                            inProgress to BubblesCyan,
                            late to StatusAmber,
                            unexecuted to StatusRed
                        )
                        var startAngle = -90f
                        segments.forEach { (count, col) ->
                            val sweep = (count.toFloat() / total) * animatedSweep
                            if (sweep > 0f) {
                                drawArc(
                                    color = col,
                                    startAngle = startAngle,
                                    sweepAngle = (sweep - 5f).coerceAtLeast(2f),
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                startAngle += sweep
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$completionPct%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BubblesSlateDark)
                    Text(if (s.isArabic) "إنجاز" else "Done", style = MaterialTheme.typography.labelSmall, color = BubblesGray)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = if (s.isArabic) "توزيع أداء العمليات اليوم" else "Today's Operations Split",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BubblesSlateDark
                )
                LegendRow(s.completedCount, completed, StatusEmerald)
                LegendRow(if (s.isArabic) "قيد التنفيذ" else "Active", inProgress, BubblesCyan)
                LegendRow(s.lateCount, late, StatusAmber)
                LegendRow(s.unexecutedCount, unexecuted, StatusRed)
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = BubblesGray)
        }
        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LiveFleetMapCanvas(
    drivers: List<DriverProfile>,
    visits: List<VisitItem>,
    selectedDriverId: String?,
    geofenceRadiusMeters: Long = 150L,
    onSelectDriver: (DriverProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 68f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "radarRadius"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "radarAlpha"
    )

    fun zoomedCoord(raw: Float): Float = (0.5f + (raw - 0.5f) * zoomFactor).coerceIn(0.06f, 0.90f)

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        containerColor = GlassCardTranslucent,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(255.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawRect(
                    Brush.linearGradient(
                        listOf(Color(0xFFEAF5F8), Color(0xFFF3FAFC))
                    )
                )
                drawRoundRect(Color(0xFFD5F2E4), Offset(w * 0.08f, h * 0.12f), Size(w * 0.20f, h * 0.24f), CornerRadius(20f, 20f))
                drawRoundRect(Color(0xFFD5F2E4), Offset(w * 0.70f, h * 0.56f), Size(w * 0.22f, h * 0.28f), CornerRadius(20f, 20f))

                val blockColor = Color(0xFFDCEBF0)
                drawRoundRect(blockColor, Offset(w * 0.34f, h * 0.10f), Size(w * 0.14f, h * 0.18f), CornerRadius(12f, 12f))
                drawRoundRect(blockColor, Offset(w * 0.58f, h * 0.16f), Size(w * 0.16f, h * 0.15f), CornerRadius(12f, 12f))
                drawRoundRect(blockColor, Offset(w * 0.14f, h * 0.56f), Size(w * 0.18f, h * 0.20f), CornerRadius(12f, 12f))

                val minorRoad = Color(0xFFFFFFFF).copy(alpha = 0.80f)
                for (i in 1..4) {
                    val y = h * i / 5f
                    drawLine(minorRoad, Offset(0f, y), Offset(w, y), strokeWidth = 5f)
                }
                for (i in 1..5) {
                    val x = w * i / 6f
                    drawLine(minorRoad, Offset(x, 0f), Offset(x, h), strokeWidth = 5f)
                }
                drawLine(Color.White, Offset(0f, h * 0.44f), Offset(w, h * 0.32f), strokeWidth = 18f)
                drawLine(BubblesCyan.copy(alpha = 0.28f), Offset(0f, h * 0.44f), Offset(w, h * 0.32f), strokeWidth = 3.5f)
                drawLine(Color.White, Offset(w * 0.46f, 0f), Offset(w * 0.54f, h), strokeWidth = 14f)

                val primaryVisit = visits.firstOrNull()
                if (primaryVisit != null) {
                    val center = Offset(w * zoomedCoord(primaryVisit.mapX), h * zoomedCoord(primaryVisit.mapY))
                    val geoPx = (48f * zoomFactor).coerceIn(30f, 82f)
                    drawCircle(color = BubblesCyan.copy(alpha = 0.16f), radius = geoPx, center = center)
                    drawCircle(color = BubblesCyan.copy(alpha = radarAlpha), radius = radarRadius, center = center)
                    drawCircle(
                        color = BubblesCyan,
                        radius = geoPx,
                        center = center,
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                    )
                }

                drivers.filter { it.gpsActive }.forEach { driver ->
                    val dc = Offset(w * zoomedCoord(driver.mapX), h * zoomedCoord(driver.mapY))
                    drawCircle(color = BubblesCyan.copy(alpha = radarAlpha), radius = radarRadius * 0.85f, center = dc)
                }

                visits.forEach { visit ->
                    val pt = Offset(w * zoomedCoord(visit.mapX), h * zoomedCoord(visit.mapY))
                    drawCircle(color = Color.White, radius = 13f, center = pt)
                    drawCircle(color = visit.status.color, radius = 8.5f, center = pt)
                }
            }

            drivers.filter { it.gpsActive || it.lastLat != null }.forEach { driver ->
                val isSelected = driver.id == selectedDriverId
                Surface(
                    onClick = { onSelectDriver(driver) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) BubblesCyanDark else if (driver.gpsActive) BubblesCyan else BubblesGray,
                    border = BorderStroke(1.5.dp, Color.White),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = (zoomedCoord(driver.mapX) * 235).dp,
                            top = (zoomedCoord(driver.mapY) * 165).dp
                        )
                        .testTag("map_driver_pin_${driver.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalShipping,
                            contentDescription = driver.nameAr,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (s.isArabic) driver.nameAr.substringBefore(" ") else driver.nameEn.substringBefore(" "),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (drivers.none { it.gpsActive || it.lastLat != null } && visits.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GlassPillFloating,
                    border = BorderStroke(1.dp, GlassSpecHighlight),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = if (s.isArabic) "أضف مناديب وزيارات لعرض الخريطة الحية" else "Add drivers & visits to view live map",
                        style = MaterialTheme.typography.bodySmall,
                        color = BubblesGray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GlassPillFloating,
                border = BorderStroke(1.dp, GlassSpecHighlight),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { zoomFactor = (zoomFactor + 0.25f).coerceAtMost(2f) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Zoom in", tint = BubblesSlateDark, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { zoomFactor = (zoomFactor - 0.25f).coerceAtLeast(0.75f) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Zoom out", tint = BubblesGray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { zoomFactor = 1f }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Center", tint = BubblesCyanDark, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GlassPillFloating,
                border = BorderStroke(1.dp, GlassSpecHighlight),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BubblesCyan))
                        Text(if (s.isArabic) "مندوب" else "Driver", style = MaterialTheme.typography.labelSmall, color = BubblesSlateDark)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusEmerald))
                        Text(if (s.isArabic) "زيارة" else "Visit", style = MaterialTheme.typography.labelSmall, color = BubblesSlateDark)
                    }
                    Text("Geofence ${geofenceRadiusMeters}m", style = MaterialTheme.typography.labelSmall, color = BubblesCyanDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RoutePlaybackMapCanvas(
    points: List<GpsStopPoint>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        containerColor = GlassCardTranslucent,
        shadowElevation = 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFFEEF6F8))
                for (i in 1..4) {
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(0f, h * i / 5f), Offset(w, h * i / 5f), 4f)
                }
                for (i in 1..5) {
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(w * i / 6f, 0f), Offset(w * i / 6f, h), 4f)
                }
                if (points.size >= 2) {
                    val path = Path()
                    points.forEachIndexed { index, pt ->
                        val offset = Offset(w * pt.x, h * pt.y)
                        if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                    }
                    drawPath(
                        path = path,
                        color = BubblesCyan.copy(alpha = 0.28f),
                        style = Stroke(width = 8f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
                    )

                    val segFloat = progress.coerceIn(0f, 1f) * (points.size - 1)
                    val segIndex = segFloat.toInt().coerceIn(0, points.size - 2)
                    val frac = segFloat - segIndex
                    val activePath = Path()
                    activePath.moveTo(w * points[0].x, h * points[0].y)
                    for (i in 1..segIndex) {
                        activePath.lineTo(w * points[i].x, h * points[i].y)
                    }
                    val pA = points[segIndex]
                    val pB = points[segIndex + 1]
                    val curX = w * (pA.x + (pB.x - pA.x) * frac)
                    val curY = h * (pA.y + (pB.y - pA.y) * frac)
                    activePath.lineTo(curX, curY)
                    drawPath(activePath, BubblesCyan, style = Stroke(width = 10f, cap = StrokeCap.Round))

                    points.forEach { pt ->
                        val c = Offset(w * pt.x, h * pt.y)
                        val isStop = pt.stopMinutes > 0
                        drawCircle(Color.White, radius = if (isStop) 12f else 9f, center = c)
                        drawCircle(if (isStop) StatusAmber else BubblesCyanDark, radius = if (isStop) 8f else 5.5f, center = c)
                    }
                    drawCircle(BubblesCyan.copy(alpha = 0.26f), radius = 26f, center = Offset(curX, curY))
                    drawCircle(BubblesCyanDark, radius = 14f, center = Offset(curX, curY))
                    drawCircle(Color.White, radius = 6f, center = Offset(curX, curY))
                } else if (points.size == 1) {
                    val c = Offset(w * points[0].x, h * points[0].y)
                    drawCircle(BubblesCyan.copy(alpha = 0.25f), radius = 26f, center = c)
                    drawCircle(BubblesCyanDark, radius = 13f, center = c)
                }
            }

            if (points.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GlassPillFloating,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = if (s.isArabic) "لا توجد نقاط GPS مسجلة بعد في هذا النطاق" else "No GPS trail recorded yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = BubblesGray,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
