package dev.voicejournal.ui.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voicejournal.ui.theme.AppTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun getCalendarTodayIcon(tintColor: Color): ImageVector {
    return ImageVector.Builder(
        name = "CalendarToday",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(tintColor)) {
            moveTo(20f, 3f)
            horizontalLineToRelative(-1f)
            verticalLineTo(1f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(2f)
            horizontalLineTo(7f)
            verticalLineTo(1f)
            horizontalLineTo(5f)
            verticalLineToRelative(2f)
            horizontalLineTo(4f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(16f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(16f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(5f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(20f, 21f)
            horizontalLineTo(4f)
            verticalLineTo(8f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(13f)
            close()
        }
    }.build()
}

@Composable
fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDatePickerClick: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val isNextDisabled = yearMonth == YearMonth.now()
    val formattedTitle = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    Surface(
        color = colors.background,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTitle,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier
                    .clickable { onTitleClick() }
                    .padding(vertical = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        tint = colors.textSecondary
                    )
                }

                IconButton(
                    onClick = onDatePickerClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = getCalendarTodayIcon(colors.primary),
                        contentDescription = "Jump to specific date",
                        tint = colors.primary
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = !isNextDisabled,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                        tint = if (isNextDisabled) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary
                    )
                }
            }
        }
    }
}
