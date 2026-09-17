package org.appdevncsu.foodfinder.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.appdevncsu.foodfinder.R
import org.appdevncsu.foodfinder.data.Event
import org.appdevncsu.foodfinder.data.endInstant
import org.appdevncsu.foodfinder.data.localStartDate
import org.appdevncsu.foodfinder.data.ncsuZone
import org.appdevncsu.foodfinder.data.startInstant
import org.appdevncsu.foodfinder.ui.theme.FoodFinderTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EventCardWidth = 300.dp
private val EventCardSpacing = 12.dp
private val EventCardPadding = 16.dp
private val EventCardHorizontalPadding = 16.dp
private val EventCardTitleSpacing = 4.dp
private val EventCardLocationSpacing = 2.dp
private val EventSheetHorizontalPadding = 24.dp
private val EventSheetBottomPadding = 32.dp
private val EventSheetSpacing = 8.dp
private val EventSheetDescriptionSpacing = 16.dp

private val EventTimeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val EventDateOutputFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/**
 * A horizontally scrolling row of upcoming event cards, shown at the top of the location feed.
 * A single event fills the width; multiple events scroll horizontally so the feed stays visible.
 */
@Composable
internal fun EventCarousel(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) return
    val single = events.size == 1
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = EventCardHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(EventCardSpacing),
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event) },
                modifier = if (single) Modifier.fillParentMaxWidth() else Modifier.width(EventCardWidth),
            )
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(EventCardPadding)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(EventCardTitleSpacing))
            Text(
                text = formatEventWhen(event),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            event.location?.let { location ->
                Spacer(modifier = Modifier.height(EventCardLocationSpacing))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Bottom sheet shown when an event card is tapped, revealing the full description. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventDetailSheet(
    event: Event,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        EventDetails(event)
    }
}

/** The contents of the event detail sheet: title, when, location, and the description. */
@Composable
internal fun EventDetails(
    event: Event,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = EventSheetHorizontalPadding,
                end = EventSheetHorizontalPadding,
                bottom = EventSheetBottomPadding,
            ),
    ) {
        Text(text = event.title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(EventSheetSpacing))
        Text(
            text = formatEventWhen(event),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        event.location?.let { location ->
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        event.description?.takeIf { it.isNotBlank() }?.let { html ->
            Spacer(modifier = Modifier.height(EventSheetDescriptionSpacing))
            Text(text = AnnotatedString.fromHtml(html), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Formats an event's date and time, e.g. "Today · 10:30 AM – 4:00 PM" or "Tomorrow · All day". */
@Composable
internal fun formatEventWhen(event: Event): String {
    val start = event.startInstant() ?: return event.start
    val zoned = start.atZone(ncsuZone)
    val today = LocalDate.now(ncsuZone)
    val dateLabel = when (zoned.toLocalDate()) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> zoned.format(EventDateOutputFormat)
    }
    val timeText = if (event.allDay) {
        stringResource(R.string.event_all_day)
    } else {
        val startText = zoned.format(EventTimeFormat)
        val endText = event.endInstant()?.atZone(ncsuZone)?.format(EventTimeFormat)
        if (endText == null) startText else stringResource(R.string.event_time_range, startText, endText)
    }
    return stringResource(R.string.event_when, dateLabel, timeText)
}

private val SampleEvent = Event(
    id = "sample@google.com#1790001000000",
    title = "Oktoberfest",
    description = "<p>Willkommen to Clark Dining Hall! Join us for an authentic Oktoberfest celebration.</p>",
    location = "Clark Dining Hall, Pullen Rd, Raleigh, NC 27606, USA",
    start = "2026-09-21T14:30:00Z",
    end = "2026-09-21T17:30:00Z",
)

private val SampleAllDayEvent = Event(
    id = "sample2@google.com#1800853200000",
    title = "Spring Meal Plans Active",
    start = "2027-01-05T05:00:00Z",
    end = "2027-01-06T05:00:00Z",
    allDay = true,
)

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun EventCarouselPreview() {
    FoodFinderTheme {
        EventCarousel(
            events = listOf(SampleEvent, SampleAllDayEvent),
            onEventClick = {},
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}
