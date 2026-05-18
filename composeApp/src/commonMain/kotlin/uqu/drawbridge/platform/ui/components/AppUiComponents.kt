package uqu.drawbridge.platform.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.MainTab
import uqu.drawbridge.platform.ui.theme.AppNavySurfaceHigh
import uqu.drawbridge.platform.ui.theme.AppMutedText
import uqu.drawbridge.platform.ui.theme.Primary500
import uqu.drawbridge.platform.ui.theme.SuccessColor
import uqu.drawbridge.platform.ui.theme.WarningColor

@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.075f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            content = content,
        )
    }
}

@Composable
internal fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Primary500,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.24f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (tint == Primary500) Color(0xFF8EF3C5) else tint,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun GlassIconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

@Composable
internal fun GlassIconLabelRow(
    icon: ImageVector,
    text: String,
    tint: Color = Primary500,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconTile(icon = icon, tint = tint, size = 28.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFF8FAFC),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun MainBottomBar(
    tabs: List<MainTab>,
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 1.dp, bottom = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(999.dp),
            color = AppNavySurfaceHigh.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    MainBottomBarItem(
                        tab = tab,
                        selected = tab == currentTab,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBarItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f)
    val itemColor by animateColorAsState(
        if (selected) Primary500.copy(alpha = 0.13f) else Color.Transparent,
    )
    val iconColor by animateColorAsState(if (selected) Primary500 else AppMutedText)
    val labelColor by animateColorAsState(if (selected) Color(0xFFF7FFFB) else AppMutedText)
    val indication = LocalIndication.current

    Column(
        modifier = modifier
            .height(36.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(999.dp))
            .background(itemColor)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = indication,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ScreenSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
internal fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            }
        ),
        border = BorderStroke(
            1.dp, 
            if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
        ),
        border = BorderStroke(
            1.dp, 
            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) 
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null,
    isPassword: Boolean = false,
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontWeight = FontWeight.Medium) },
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.4f else 0.6f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f)
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale),
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.3f else 0.8f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val color = when (tone) {
        StatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        StatusTone.Success -> SuccessColor
        StatusTone.Warning -> WarningColor
        StatusTone.Error -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal enum class StatusTone {
    Neutral,
    Success,
    Warning,
    Error,
}

@Composable
internal fun LoadingStateCard(
    title: String = "Loading",
    message: String = "Preparing the latest workspace data.",
) {
    AppCard {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun EmptyStateCard(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AppCard {
        StatusChip(text = "Empty", tone = StatusTone.Neutral)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionText != null && onAction != null) {
            SecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
internal fun ErrorStateCard(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AppCard {
        StatusChip(text = "Needs attention", tone = StatusTone.Error)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionText != null && onAction != null) {
            SecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
internal fun DeferredFeatureCard(
    destination: AppDestination,
    title: String,
    message: String,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            StatusChip(text = "Phase 2+", tone = StatusTone.Warning)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = destination.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}
