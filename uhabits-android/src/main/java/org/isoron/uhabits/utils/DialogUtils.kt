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

package org.isoron.uhabits.utils

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

var currentDialog: WeakReference<Dialog> = WeakReference(null)
var currentDialogFragment: WeakReference<DialogFragment> = WeakReference(null)

fun dismissCurrentDialog() {
    try {
        currentDialog.get()?.dismiss()
    } catch (e: Exception) {
        // NOP
    }
    currentDialog = WeakReference(null)

    val fragment = currentDialogFragment.get()
    if (fragment != null && fragment.isAdded) {
        try {
            fragment.dismissAllowingStateLoss()
        } catch (e: Exception) {
            // NOP
        }
    }
    currentDialogFragment = WeakReference(null)
}

fun Dialog.dismissCurrentAndShow() {
    dismissCurrentDialog()
    currentDialog = WeakReference(this)
    show()
}

fun DialogFragment.dismissCurrentAndShow(fragmentManager: FragmentManager, tag: String) {
    dismissCurrentDialog()
    currentDialogFragment = WeakReference(this)
    show(fragmentManager, tag)
    fragmentManager.executePendingTransactions()
}
