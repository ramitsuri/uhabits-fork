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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.isoron.platform.gui.AndroidDataView
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.allhabits.AllHabitsPresenter
import org.isoron.uhabits.core.ui.screens.habits.allhabits.AllHabitsState
import org.isoron.uhabits.core.ui.views.AllHabitsChart
import org.isoron.uhabits.core.ui.views.OnDateClickedListener
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dismissCurrentAndShow
import java.util.Locale

class AllHabitsActivity : AppCompatActivity() {

    private lateinit var chart: AllHabitsChart
    private lateinit var chartView: AndroidDataView
    private lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = (applicationContext as HabitsApplication).component
        val habitList = appComponent.habitList
        preferences = appComponent.preferences

        val themeSwitcher = AndroidThemeSwitcher(this, preferences)
        themeSwitcher.apply()

        setContentView(R.layout.activity_all_habits)
        val container = findViewById<View>(R.id.container)
        container.applyRootViewInsets()
        container.applyBottomInset()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.applyToolbarInsets()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.all_habits)

        fun refreshState(): AllHabitsState {
            return AllHabitsPresenter.buildState(
                habitList = habitList,
                preferences = preferences,
                theme = themeSwitcher.currentTheme
            )
        }

        val initialState = refreshState()

        val showDayHabitsDialog = { date: LocalDate ->
            val dialog = DayHabitsDialog.newInstance(date)
            dialog.dismissCurrentAndShow(supportFragmentManager, "dayHabitsDialog")
        }

        chartView = findViewById(R.id.allHabitsChart)
        chart = AllHabitsChart(
            dateFormatter = JavaLocalDateFormatter(Locale.getDefault()),
            firstWeekday = initialState.firstWeekday,
            theme = initialState.theme,
            today = initialState.today,
            habitCompletions = initialState.completions,
            onDateClickedListener = object : OnDateClickedListener {
                override fun onDateShortPress(date: LocalDate) {
                    showDayHabitsDialog(date)
                }
            },
            padding = 10.0
        )
        chartView.view = chart
        chartView.postInvalidate()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.all_habits, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.actionFilterHabits -> {
                val dialog = HabitsLegendDialog()
                dialog.onVisibilityChanged = {
                    val appComponent = (applicationContext as HabitsApplication).component
                    val themeSwitcher = AndroidThemeSwitcher(this, preferences)
                    val updatedState = AllHabitsPresenter.buildState(
                        appComponent.habitList,
                        preferences,
                        themeSwitcher.currentTheme
                    )
                    chart.habitCompletions = updatedState.completions
                    chartView.postInvalidate()
                }
                dialog.dismissCurrentAndShow(supportFragmentManager, "habitsLegendDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
