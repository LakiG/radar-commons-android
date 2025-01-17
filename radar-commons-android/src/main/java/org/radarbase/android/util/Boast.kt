/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.radarbase.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * [Toast] decorator allowing for easy cancellation of notifications. Use this class if you
 * want subsequent Toast notifications to overwrite current ones.
 *
 *
 * By default, a current [Boast] notification will be cancelled by a subsequent notification.
 * This default behaviour can be changed by calling certain methods like [.show].
 */
@SuppressLint("ShowToast")
class Boast private constructor(
    /**
     * Internal reference to the [Toast] object that will be displayed.
     */
    private val toast: Toast,
) {

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet. You do not normally
     * have to call this. Normally view will disappear on its own after the appropriate duration.
     */
    fun cancel() {
        toast.cancel()
    }
    /**
     * Show the view for the specified duration. This method can be used to cancel the current
     * notification, or to queue up notifications.
     *
     * @param cancelCurrent `true` to cancel any current notification and replace it with this new
     * one
     * @see .show
     */
    /**
     * Show the view for the specified duration. By default, this method cancels any current
     * notification to immediately display the new one. For conventional [Toast.show]
     * queueing behaviour, use method [.show].
     *
     * @see .show
     */
    @JvmOverloads
    fun show(cancelCurrent: Boolean = true) {
        // cancel current
        if (cancelCurrent) {
            globalBoast?.cancel()
        }

        // save an instance of this current notification
        globalBoast = this
        toast.show()
    }

    companion object {
        /**
         * Keeps track of certain Boast notifications that may need to be cancelled. This functionality
         * is only offered by some of the methods in this class.
         */
        @Volatile
        private var globalBoast: Boast? = null

        /**
         * Make a standard [Boast] that just contains a text view.
         *
         * @param context  The context to use. Usually your [android.app.Application] or
         * [android.app.Activity] object.
         * @param text     The text to show. Can be formatted text.
         * @param duration How long to display the message. Either [Toast.LENGTH_SHORT] or
         * [Toast.LENGTH_LONG]
         */
        fun makeText(context: Context, text: CharSequence, duration: Int): Boast {
            return Boast(Toast.makeText(context, text, duration))
        }

        /**
         * Make a standard [Boast] that just contains a text view with the text from a resource.
         *
         * @param context  The context to use. Usually your [android.app.Application] or
         * [android.app.Activity] object.
         * @param resId    The resource id of the string resource to use. Can be formatted text.
         * @param duration How long to display the message. Either [Toast.LENGTH_SHORT] or
         * [Toast.LENGTH_LONG]
         * @throws Resources.NotFoundException if the resource can't be found.
         */
        @Throws(Resources.NotFoundException::class)
        fun makeText(context: Context, @StringRes resId: Int, duration: Int): Boast {
            return Boast(Toast.makeText(context, resId, duration))
        }

        /**
         * Make a standard [Boast] that just contains a text view with the text from a resource.
         * Duration defaults to [Toast.LENGTH_SHORT].
         *
         * @param context The context to use. Usually your [android.app.Application] or
         * [android.app.Activity] object.
         * @param resId   The resource id of the string resource to use. Can be formatted text.
         * @throws Resources.NotFoundException if the resource can't be found.
         */
        @Throws(Resources.NotFoundException::class)
        fun makeText(context: Context, @StringRes resId: Int): Boast {
            return Boast(Toast.makeText(context, resId, Toast.LENGTH_SHORT))
        }
    }
}
