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

package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.DataView
import org.isoron.platform.gui.TextAlign
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.LocalDateFormatter
import org.isoron.uhabits.core.models.PaletteColor
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class AllHabitsChart(
    var dateFormatter: LocalDateFormatter,
    var firstWeekday: DayOfWeek,
    var theme: Theme,
    var today: LocalDate,
    var habitCompletions: Map<LocalDate, List<PaletteColor>> = emptyMap(),
    var onDateClickedListener: OnDateClickedListener = object : OnDateClickedListener {},
    var padding: Double = 0.0
) : DataView {

    var squareSpacing = 1.0
    override var dataOffset = 0

    private var squareSize = 0.0
    private var width = 0.0
    private var height = 0.0
    private var nColumns = 0
    private var topLeftOffset = 0
    private var topLeftDate = LocalDate(2020, 1, 1)
    private var lastPrintedMonth = ""
    private var lastPrintedYear = ""
    private var headerOverflow = 0.0

    override val dataColumnWidth: Double
        get() = squareSpacing + squareSize

    override fun onClick(x: Double, y: Double) {
        onDateClicked(x, y, false)
    }

    override fun onLongClick(x: Double, y: Double) {
        onDateClicked(x, y, true)
    }

    private fun onDateClicked(x: Double, y: Double, isLongClick: Boolean) {
        if (width <= 0.0) throw IllegalStateException("onClick must be called after draw(canvas)")
        val col = ((x - padding) / squareSize).toInt()
        val row = ((y - padding) / squareSize).toInt()
        val offset = col * 7 + (row - 1)
        if (x - padding < 0 || row == 0 || row > 7 || col < 0 || col >= nColumns) return
        val clickedDate = topLeftDate.plus(offset)
        if (clickedDate.isNewerThan(today)) return
        if (isLongClick) {
            onDateClickedListener.onDateLongPress(clickedDate)
        } else {
            onDateClickedListener.onDateShortPress(clickedDate)
        }
    }

    override fun draw(canvas: Canvas) {
        width = canvas.getWidth()
        height = canvas.getHeight()

        canvas.setColor(theme.cardBackgroundColor)
        canvas.fill()

        squareSize = round((height - 2 * padding) / 8.0)
        canvas.setFontSize(min(12.0, squareSize * 0.3))

        val weekdayColumnWidth = DayOfWeek.entries.map { weekday ->
            canvas.measureText(dateFormatter.shortWeekdayName(weekday)) + squareSize * 0.15
        }.maxOrNull() ?: 0.0

        nColumns = floor((width - 2 * padding - weekdayColumnWidth) / squareSize).toInt()
        val firstWeekdayOffset = (
            today.dayOfWeek.daysSinceSunday -
                firstWeekday.daysSinceSunday + 7
            ) % 7
        topLeftOffset = (nColumns - 1 + dataOffset) * 7 + firstWeekdayOffset
        topLeftDate = today.minus(topLeftOffset)

        lastPrintedYear = ""
        lastPrintedMonth = ""
        headerOverflow = 0.0

        // Draw main columns
        repeat(nColumns) { column ->
            val topOffset = topLeftOffset - 7 * column
            val topDate = topLeftDate.plus(7 * column)
            drawColumn(canvas, column, topDate, topOffset)
        }

        // Draw week day names
        canvas.setColor(theme.mediumContrastTextColor)
        repeat(7) { row ->
            val date = topLeftDate.plus(row)
            canvas.setTextAlign(TextAlign.LEFT)
            canvas.drawText(
                dateFormatter.shortWeekdayName(date),
                padding + nColumns * squareSize + squareSize * 0.15,
                padding + squareSize * (row + 1) + squareSize / 2
            )
        }
    }

    private fun drawColumn(
        canvas: Canvas,
        column: Int,
        topDate: LocalDate,
        topOffset: Int
    ) {
        drawHeader(canvas, column, topDate)
        repeat(7) { row ->
            val offset = topOffset - row
            val date = topDate.plus(row)
            if (offset < 0) return
            drawSquare(
                canvas,
                padding + column * squareSize,
                padding + (row + 1) * squareSize,
                squareSize - squareSpacing,
                squareSize - squareSpacing,
                date
            )
        }
    }

    private fun drawHeader(canvas: Canvas, column: Int, date: LocalDate) {
        canvas.setColor(theme.mediumContrastTextColor)
        val monthText = dateFormatter.shortMonthName(date)
        val yearText = date.year.toString()
        val headerText: String
        when {
            monthText != lastPrintedMonth -> {
                headerText = monthText
                lastPrintedMonth = monthText
            }
            yearText != lastPrintedYear -> {
                headerText = yearText
                lastPrintedYear = headerText
            }
            else -> {
                headerText = ""
            }
        }
        canvas.setTextAlign(TextAlign.LEFT)
        canvas.drawText(
            headerText,
            headerOverflow + padding + column * squareSize,
            padding + squareSize / 2
        )

        headerOverflow += canvas.measureText(headerText) + 0.1 * squareSize
        headerOverflow = max(0.0, headerOverflow - squareSize)
    }

    private fun drawSquare(
        canvas: Canvas,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        date: LocalDate
    ) {
        val colors = habitCompletions[date] ?: emptyList()

        // Square background
        canvas.setColor(theme.lowContrastTextColor)
        canvas.fillRoundRect(x, y, width, height, width * 0.15)

        // Day number
        canvas.setColor(theme.mediumContrastTextColor)
        canvas.setTextAlign(TextAlign.CENTER)
        canvas.setFontSize(min(14.0, height * 0.28))
        canvas.drawText(date.day.toString(), x + width / 2.0, y + height * 0.3)

        // Dots for completed habits
        if (colors.isNotEmpty()) {
            drawHabitDots(
                canvas = canvas,
                startX = x + width * 0.1,
                startY = y + height * 0.38,
                areaWidth = width * 0.8,
                areaHeight = height * 0.55,
                colors = colors
            )
        }
    }

    private fun drawHabitDots(
        canvas: Canvas,
        startX: Double,
        startY: Double,
        areaWidth: Double,
        areaHeight: Double,
        colors: List<PaletteColor>
    ) {
        val total = colors.size
        val cols = when {
            total <= 2 -> total
            total <= 6 -> 3
            total <= 12 -> 4
            else -> 5
        }
        val rows = (total + cols - 1) / cols

        val cellWidth = areaWidth / cols
        val cellHeight = areaHeight / rows

        val dotRadius = min(cellWidth, cellHeight) * 0.38
        val clampedRadius = max(1.2, min(dotRadius, areaWidth / 6.0))

        repeat(total) { index ->
            val col = index % cols
            val row = index / cols
            val cx = startX + cellWidth * col + cellWidth / 2.0
            val cy = startY + cellHeight * row + cellHeight / 2.0

            val color = theme.color(colors[index].paletteIndex)
            canvas.setColor(color)
            canvas.fillCircle(cx, cy, clampedRadius)
        }
    }
}
