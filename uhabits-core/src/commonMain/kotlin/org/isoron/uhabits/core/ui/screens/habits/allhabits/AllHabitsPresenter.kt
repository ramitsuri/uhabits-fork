/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.core.ui.screens.habits.allhabits

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.views.Theme

data class HabitLegendItem(
    val id: Long,
    val name: String,
    val color: PaletteColor,
    val isVisible: Boolean
)

data class AllHabitsState(
    val today: LocalDate,
    val firstWeekday: DayOfWeek,
    val theme: Theme,
    val completions: Map<LocalDate, List<PaletteColor>>,
    val legendItems: List<HabitLegendItem>
)

class AllHabitsPresenter(
    private val habitList: HabitList
) {
    companion object {
        fun buildState(
            habitList: HabitList,
            preferences: Preferences,
            theme: Theme
        ): AllHabitsState {
            val today = getToday()
            val activeHabits = habitList.filter { !it.isArchived }

            val oldestDate = activeHabits.mapNotNull { habit ->
                habit.computedEntries.getKnown().lastOrNull()?.date
            }.minOrNull() ?: today

            val completionsMap = mutableMapOf<LocalDate, MutableList<PaletteColor>>()

            val visibleHabits = activeHabits.filter { habit ->
                habit.id != null && preferences.isHabitVisibleInAllHabits(habit.id!!)
            }

            for (habit in visibleHabits) {
                val entries = habit.computedEntries.getByInterval(oldestDate, today)
                for (entry in entries) {
                    if (isCompleted(habit, entry)) {
                        completionsMap.getOrPut(entry.date) { mutableListOf() }.add(habit.color)
                    }
                }
            }

            val legendItems = activeHabits.map { habit ->
                val habitId = habit.id ?: -1L
                HabitLegendItem(
                    id = habitId,
                    name = habit.name,
                    color = habit.color,
                    isVisible = habitId != -1L && preferences.isHabitVisibleInAllHabits(habitId)
                )
            }

            return AllHabitsState(
                today = today,
                firstWeekday = preferences.firstWeekday,
                theme = theme,
                completions = completionsMap,
                legendItems = legendItems
            )
        }

        private fun isCompleted(habit: Habit, entry: Entry): Boolean {
            return if (habit.isNumerical) {
                entry.value != Entry.UNKNOWN && entry.value != Entry.SKIP && entry.value > 0
            } else {
                entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO
            }
        }
    }
}
