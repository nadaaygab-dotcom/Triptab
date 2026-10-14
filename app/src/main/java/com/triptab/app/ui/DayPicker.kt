package com.triptab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.triptab.app.data.Trip
import com.triptab.app.data.TripDays

/** Day selection shared by screens. [ALL_DAYS] means "sum all days". */
const val ALL_DAYS = 0

fun dayOrNull(day: Int): Int? = if (day == ALL_DAYS) null else day

@Composable
fun DayPicker(trip: Trip, day: Int, onDay: (Int) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = day == ALL_DAYS, onClick = { onDay(ALL_DAYS) },
                label = { Text("All days") },
                leadingIcon = { Icon(Icons.Default.Functions, null, Modifier.size(18.dp)) },
            )
        }
        items(TripDays.days(trip)) { d ->
            FilterChip(
                selected = day == d, onClick = { onDay(d) },
                label = { Text(TripDays.label(trip, d)) },
                leadingIcon = if (day == d) {
                    { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) }
                } else null,
            )
        }
    }
}
