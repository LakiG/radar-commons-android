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

package org.radarbase.android.config

import org.radarbase.android.RadarConfiguration
import java.util.*
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE

class SingleRadarConfiguration(val status: RadarConfiguration.RemoteConfigStatus, val config: Map<String, String>) {
    val keys: Set<String> = config.keys

    /**
     * Get a string indexed by key.
     * @throws IllegalArgumentException if the key does not have a value
     */
    fun getString(key: String): String {
        return requireNotNull(optString(key)) { "Key $key does not have a value" }
    }

    /**
     * Get a configured long value.
     * @param key key of the value
     * @return long value
     * @throws NumberFormatException if the configured value is not a Long
     * @throws IllegalArgumentException if the key does not have an associated value
     */
    fun getLong(key: String): Long = getString(key).toLong()

    /**
     * Get a configured int value.
     * @param key key of the value
     * @return int value
     * @throws NumberFormatException if the configured value is not an Integer
     * @throws IllegalArgumentException if the key does not have an associated value
     */
    fun getInt(key: String): Int = getString(key).toInt()

    /**
     * Get a configured float value.
     * @param key key of the value
     * @return float value
     * @throws NumberFormatException if the configured value is not an Float
     * @throws IllegalArgumentException if the key does not have an associated value
     */
    fun getFloat(key: String): Float = getString(key).toFloat()

    /**
     * Get a string indexed by key, or a default value if it does not exist.
     * @throws IllegalArgumentException if the key does not have a value
     */
    fun getString(key: String, defaultValue: String): String = optString(key) ?: defaultValue

    /**
     * Get a string indexed by key, or null if it does not exist.
     */
    fun optString(key: String): String? = config[key]

    /**
     * Get a configured long value. If the configured value is not present or not a valid long,
     * return a default value.
     * @param key key of the value
     * @param defaultValue default value
     * @return configured long value, or defaultValue if no suitable value was found.
     */
    fun getLong(key: String, defaultValue: Long): Long {
        return optString(key)?.toLongOrNull()
                ?: defaultValue
    }

    /**
     * Get a configured int value. If the configured value is not present or not a valid int,
     * return a default value.
     * @param key key of the value
     * @param defaultValue default value
     * @return configured int value, or defaultValue if no suitable value was found.
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return optString(key)?.toIntOrNull()
                ?: defaultValue
    }


    /**
     * Get a configured float value. If the configured value is not present or not a valid float,
     * return a default value.
     * @param key key of the value
     * @param defaultValue default value
     * @return configured float value, or defaultValue if no suitable value was found.
     */
    fun getFloat(key: String, defaultValue: Float): Float {
        return optString(key)?.toFloatOrNull()
                ?: defaultValue
    }

    fun getBoolean(key: String): Boolean {
        val str = getString(key)
        return when {
            IS_TRUE.matcher(str).find() -> true
            IS_FALSE.matcher(str).find() -> false
            else -> throw NumberFormatException("String '$str' of property $key is not a boolean")
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val str = optString(key) ?: return defaultValue
        return when {
            IS_TRUE.matcher(str).find() -> true
            IS_FALSE.matcher(str).find() -> false
            else -> defaultValue
        }
    }

    /** There is a non-empty configuration for given key. */
    operator fun contains(key: String): Boolean = key in config

    override fun toString(): String {
        return StringBuilder(config.size * 40 + 20).apply {
            append("RadarConfiguration (")
            append(status)
            append("):\n")
            for ((key, value) in config.toSortedMap()) {
                append("  ")
                append(key)
                append(": ")
                append(value)
                append('\n')
            }
        }.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleRadarConfiguration

        return status == other.status && config == other.config
    }

    override fun hashCode(): Int = Objects.hash(status, config)

    companion object {
        private val IS_TRUE = Pattern.compile(
                "^(1|true|t|yes|y|on)$", CASE_INSENSITIVE)
        private val IS_FALSE = Pattern.compile(
                "^(0|false|f|no|n|off|)$", CASE_INSENSITIVE)
    }
}
