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

package org.isoron.uhabits.activities.habits.allhabits

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.isoron.platform.gui.FontAwesome
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.ui.views.Theme
import org.isoron.uhabits.utils.InterfaceUtils
import org.isoron.uhabits.utils.sres
import java.util.Locale
import kotlin.math.min

class DayHabitsDialog : AppCompatDialogFragment() {

    private lateinit var date: LocalDate
    private lateinit var habitList: HabitList
    private lateinit var theme: Theme

    private lateinit var titleTextView: TextView
    private lateinit var subtitleTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DayHabitsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appComponent = (requireActivity().application as HabitsApplication).component
        habitList = appComponent.habitList
        val preferences = appComponent.preferences

        val themeSwitcher = AndroidThemeSwitcher(requireActivity(), preferences)
        themeSwitcher.apply()
        theme = themeSwitcher.currentTheme

        val unixTime = requireArguments().getLong(ARG_DATE_UNIX_TIME)
        date = LocalDate.fromUnixTime(unixTime)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_day_habits, null)

        titleTextView = dialogView.findViewById(R.id.dialogTitle)
        subtitleTextView = dialogView.findViewById(R.id.dialogSubtitle)
        recyclerView = dialogView.findViewById(R.id.dayHabitsRecyclerView)

        val formatter = JavaLocalDateFormatter(Locale.getDefault())
        titleTextView.text = "${formatter.longWeekdayName(date.dayOfWeek)}, ${formatter.longMonthName(date)} ${date.day}, ${date.year}"

        adapter = DayHabitsAdapter(emptyList(), theme)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        refreshHabitsList()

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            val metrics = resources.displayMetrics
            val dialogWidth = min((metrics.widthPixels * 0.88).toInt(), (420 * metrics.density).toInt())
            setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    private fun refreshHabitsList() {
        val activeHabits = habitList.filter { !it.isArchived }
        val items = activeHabits.map { habit ->
            val entry = habit.computedEntries.get(date)
            val completed = isCompleted(habit, entry)
            DayHabitItem(habit, entry, completed)
        }

        val completedCount = items.count { it.isCompleted }
        subtitleTextView.text = "$completedCount of ${items.size} habits completed"

        adapter.updateItems(items)
    }

    private fun isCompleted(habit: Habit, entry: Entry): Boolean {
        return if (habit.isNumerical) {
            entry.value != Entry.UNKNOWN && entry.value != Entry.SKIP && entry.value > 0
        } else {
            entry.value == Entry.YES_MANUAL || entry.value == Entry.YES_AUTO
        }
    }

    private data class DayHabitItem(
        val habit: Habit,
        val entry: Entry,
        val isCompleted: Boolean
    )

    private class DayHabitsAdapter(
        private var items: List<DayHabitItem>,
        private val theme: Theme
    ) : RecyclerView.Adapter<DayHabitsViewHolder>() {

        fun updateItems(newItems: List<DayHabitItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayHabitsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_day_habit, parent, false)
            return DayHabitsViewHolder(view, theme)
        }

        override fun onBindViewHolder(holder: DayHabitsViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class DayHabitsViewHolder(
        itemView: View,
        private val theme: Theme
    ) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val habitName: TextView = itemView.findViewById(R.id.habitName)
        private val habitValue: TextView = itemView.findViewById(R.id.habitValue)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)

        fun bind(item: DayHabitItem) {
            val habit = item.habit
            val entry = item.entry

            habitName.text = habit.name

            val color = theme.color(habit.color.paletteIndex).toInt()
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            colorIndicator.background = drawable

            statusText.typeface = InterfaceUtils.getFontAwesome(itemView.context)

            if (item.isCompleted) {
                statusText.text = InterfaceUtils.getFontAwesome(itemView.context).let {
                    // Checkmark FontAwesome icon
                    FontAwesome.CHECK
                }
                statusText.setTextColor(color)
            } else if (entry.value == Entry.SKIP) {
                statusText.text = "S"
                statusText.setTextColor(itemView.sres.getColor(R.attr.contrast60))
            } else {
                statusText.text = "—"
                statusText.setTextColor(itemView.sres.getColor(R.attr.contrast60))
            }

            if (habit.isNumerical && entry.value != Entry.UNKNOWN) {
                habitValue.visibility = View.VISIBLE
                val formattedVal = entry.value / 1000.0
                habitValue.text = "$formattedVal / ${habit.targetValue} ${habit.unit}".trim()
            } else {
                habitValue.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_DATE_UNIX_TIME = "arg_date_unix_time"

        fun newInstance(date: LocalDate): DayHabitsDialog {
            val dialog = DayHabitsDialog()
            dialog.arguments = Bundle().apply {
                putLong(ARG_DATE_UNIX_TIME, date.unixTime)
            }
            return dialog
        }
    }
}
