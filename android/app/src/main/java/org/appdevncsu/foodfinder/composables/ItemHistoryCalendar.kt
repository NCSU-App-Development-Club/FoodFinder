package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import org.appdevncsu.foodfinder.viewmodel.ItemHistoryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HistoryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

private val HistoryMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

private const val DaysPerRow = 7
private val DayCellSize = 36.dp
private val MonthChevronRotationDegrees = 180f

@Composable
fun ItemHistoryCalendar(
    locationId: Int,
    itemName: String,
    date: String,
    modifier: Modifier = Modifier,
    viewModel: ItemHistoryViewModel = hiltViewModel(),
) {
    LaunchedEffect(locationId, itemName, date) {
        viewModel.load(locationId, itemName, date)
    }
    val state by viewModel.uiState.collectAsState()

    ItemHistoryCalendarContent(
        state = state,
        onRetry = { viewModel.retry(locationId, itemName, date) },
        modifier = modifier,
    )
}

@Composable
internal fun ItemHistoryCalendarContent(
    state: ItemHistoryViewModel.UiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
    val history = state.history
    when {
        state.error != null && history == null -> {
            ErrorState(
                message = state.error,
                onRetry = onRetry,
                modifier = modifier.fillMaxSize(),
            )
        }

        history == null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> HistoryContent(history = history, today = today, modifier = modifier)
    }
}

@Composable
private fun HistoryContent(
    history: ItemHistoryViewModel.History,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HistoryStats(history = history, firstSeen = history.firstSeen)
        ItemSeenCalendar(
            seenDates = history.seenDates,
            firstSeen = history.firstSeen,
            today = today,
        )
    }
}

@Composable
private fun HistoryStats(
    history: ItemHistoryViewModel.History,
    firstSeen: LocalDate?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Stat(
            label = stringResource(R.string.item_history_first_seen),
            value = firstSeen?.format(HistoryDateFormatter)
                ?: stringResource(R.string.item_history_never),
            modifier = Modifier.weight(1f),
        )
        Stat(
            label = stringResource(R.string.item_history_frequency),
            value = formatItemFrequency(history.frequencyPerWeek),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ItemSeenCalendar(
    seenDates: Set<LocalDate>,
    firstSeen: LocalDate?,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val todayMonth = remember(today) { YearMonth.from(today) }
    val earliestMonth = remember(seenDates, todayMonth) {
        seenDates.minOrNull()?.let(YearMonth::from) ?: todayMonth
    }
    var displayedMonthKey by rememberSaveable { mutableStateOf(todayMonth.toString()) }
    val displayedMonth = remember(displayedMonthKey) { YearMonth.parse(displayedMonthKey) }

    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(
            month = displayedMonth,
            canGoPrevious = displayedMonth > earliestMonth,
            canGoNext = displayedMonth < todayMonth,
            onPrevious = { displayedMonthKey = displayedMonth.minusMonths(1).toString() },
            onNext = { displayedMonthKey = displayedMonth.plusMonths(1).toString() },
        )
        WeekdayHeader()
        MonthGrid(
            month = displayedMonth,
            seenDates = seenDates,
            firstSeen = firstSeen,
            today = today,
        )
        Text(
            text = if (seenDates.isEmpty()) {
                stringResource(R.string.item_history_seen_none)
            } else {
                pluralStringResource(R.plurals.item_history_seen_summary, seenDates.size, seenDates.size)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right_24px),
                contentDescription = stringResource(R.string.item_history_previous_month),
                modifier = Modifier.rotate(MonthChevronRotationDegrees),
            )
        }
        Text(text = month.format(HistoryMonthFormatter), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                painter = painterResource(R.drawable.keyboard_arrow_right_24px),
                contentDescription = stringResource(R.string.item_history_next_month),
            )
        }
    }
}

@Composable
private fun WeekdayHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(DaysPerRow) { index ->
            val label = java.time.DayOfWeek.of(if (index == 0) 7 else index)
                .getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    seenDates: Set<LocalDate>,
    firstSeen: LocalDate?,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value % DaysPerRow
    val daysInMonth = month.lengthOfMonth()
    val rowCount = (leadingBlanks + daysInMonth + DaysPerRow - 1) / DaysPerRow

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rowCount) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DaysPerRow) { column ->
                    val dayOfMonth = row * DaysPerRow + column - leadingBlanks + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = month.atDay(dayOfMonth)
                        DayCell(
                            date = date,
                            isSeen = date in seenDates,
                            isFirstSeen = date == firstSeen,
                            isToday = date == today,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSeen: Boolean,
    isFirstSeen: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val background = if (isSeen) MaterialTheme.colorScheme.primary else Color.Transparent
        val border = when {
            isFirstSeen -> BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
            isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            else -> null
        }
        Box(
            modifier = Modifier
                .size(DayCellSize)
                .clip(CircleShape)
                .background(background)
                .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSeen) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private val SampleHistory = ItemHistoryViewModel.History(
    firstSeen = LocalDate.now().minusDays(45),
    frequencyPerWeek = 2.4,
    seenDates = listOf(
        LocalDate.now().minusDays(40),
        LocalDate.now().minusDays(38),
        LocalDate.now().minusDays(35),
        LocalDate.now().minusDays(10),
        LocalDate.now().minusDays(7),
        LocalDate.now().minusDays(3),
        LocalDate.now(),
    ).toSet(),
)

@Composable
@Preview(showBackground = true)
private fun ItemHistoryCalendarPreview() {
    FoodFinderTheme {
        ItemHistoryCalendarContent(
            state = ItemHistoryViewModel.UiState(loading = false, history = SampleHistory),
        )
    }
}
