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
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.allhabits.AllHabitsPresenter
import org.isoron.uhabits.core.ui.screens.habits.allhabits.HabitLegendItem
import org.isoron.uhabits.core.ui.views.Theme
import kotlin.math.min

class HabitsLegendDialog : AppCompatDialogFragment() {

    var onVisibilityChanged: () -> Unit = {}

    private lateinit var preferences: Preferences
    private lateinit var theme: Theme
    private lateinit var adapter: LegendAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appComponent = (requireActivity().application as HabitsApplication).component
        val habitList = appComponent.habitList
        preferences = appComponent.preferences

        val themeSwitcher = AndroidThemeSwitcher(requireActivity(), preferences)
        themeSwitcher.apply()
        theme = themeSwitcher.currentTheme

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_habits_legend, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.legendRecyclerView)

        val state = AllHabitsPresenter.buildState(habitList, preferences, theme)

        adapter = LegendAdapter(state.legendItems, theme) { item, isChecked ->
            preferences.setHabitVisibleInAllHabits(item.id, isChecked)
            val updatedState = AllHabitsPresenter.buildState(habitList, preferences, theme)
            adapter.updateItems(updatedState.legendItems)
            onVisibilityChanged()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

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

    private class LegendAdapter(
        private var items: List<HabitLegendItem>,
        private val theme: Theme,
        private val onItemCheckedChange: (HabitLegendItem, Boolean) -> Unit
    ) : RecyclerView.Adapter<LegendViewHolder>() {

        fun updateItems(newItems: List<HabitLegendItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_habit_legend, parent, false)
            return LegendViewHolder(view, theme, onItemCheckedChange)
        }

        override fun onBindViewHolder(holder: LegendViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class LegendViewHolder(
        itemView: View,
        private val theme: Theme,
        private val onItemCheckedChange: (HabitLegendItem, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        private val habitName: TextView = itemView.findViewById(R.id.habitName)
        private val checkBox: CheckBox = itemView.findViewById(R.id.showInCalendarCheckBox)

        fun bind(item: HabitLegendItem) {
            habitName.text = item.name

            val color = theme.color(item.color.paletteIndex).toInt()
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            colorIndicator.background = drawable

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = item.isVisible

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onItemCheckedChange(item, isChecked)
            }
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }
}
