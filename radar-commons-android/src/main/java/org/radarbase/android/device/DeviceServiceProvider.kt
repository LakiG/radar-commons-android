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

package org.radarbase.android.device

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import org.radarbase.android.MainActivity
import org.radarbase.android.RadarConfiguration
import org.radarbase.android.RadarService
import org.radarbase.android.auth.AppAuthState
import org.radarbase.android.auth.portal.SourceType
import org.radarbase.android.radarApp
import org.slf4j.LoggerFactory

/**
 * RADAR service provider, to bind and configure to a service. It is not thread-safe.
 * @param <T> state that the Service will provide.
</T> */
abstract class DeviceServiceProvider<T : BaseDeviceState> {

    /** Get the MainActivity associated to the current connection.  */
    /**
     * Associate a MainActivity with a new connection.
     * @throws NullPointerException if given context is null
     * @throws IllegalStateException if the connection has already been started.
     */
    var radarService: RadarService? = null
        set(radarService) {
            if (connectionBacking != null) {
                throw IllegalStateException(
                        "Cannot change the RadarService after a connection has been started.")
            }
            field = radarService
        }

    private var connectionBacking: DeviceServiceConnection<T>? = null

    val connection: DeviceServiceConnection<T>
        get() {
            if (connectionBacking == null) {
                val radarService = radarService
                        ?: throw IllegalStateException("#setRadarService(RadarService) needs to be set before #getConnection() is called.")

                connectionBacking = DeviceServiceConnection(radarService, serviceClass.name)
            }
            return connectionBacking!!
        }

    /**
     * Whether [.bind] has been called and [.unbind] has not been called since then.
     * @return true if bound, false otherwise
     */
    var isBound: Boolean = false
        private set

    /**
     * Class of the service.
     * @return DeviceService
     */
    abstract val serviceClass: Class<out DeviceService<*>>

    /** Display name of the service.  */
    abstract val displayName: String

    /**
     * Image to display when onboarding for this service.
     * @return resource number or -1 if none is available.
     */
    open val descriptionImage: Int
        get() = -1

    /**
     * Description of the service. This should tell what the service does and why certain
     * permissions are needed.
     * @return description or `null` if no description is needed.
     */
    open val description: String?
        get() = null

    /** Get the RadarConfiguration currently set for the service provider.  */
    val config: RadarConfiguration
        get() = radarService!!.radarApp.configuration

    abstract val sourceProducer: String

    abstract val sourceModel: String

    abstract val version: String

    /**
     * Whether the service has a UI detail view that can be invoked. If not,
     * [.showDetailView] will throw an UnsupportedOperationException.
     */
    open val hasDetailView: Boolean
        get() = false

    /**
     * Show a detail view from the MainActivity.
     * @throws UnsupportedOperationException if [.hasDetailView] is false.
     */
    fun showDetailView() {
        throw UnsupportedOperationException()
    }

    /**
     * Bind the service to the MainActivity. Call this when the [MainActivity.onStart] is
     * called.
     * @throws IllegalStateException if [.setRadarService] has not been called or
     * if the service is already bound.
     */
    fun bind() {
        if (this.radarService == null) {
            throw IllegalStateException(
                    "#setRadarService(RadarService) needs to be set before #bind() is called.")
        }
        if (isBound) {
            throw IllegalStateException("Service is already bound")
        }
        try {
            logger.debug("Binding {}", this)
            val extras = Bundle()
            configure(extras)

            val intent = Intent(this.radarService, serviceClass).apply {
                putExtras(extras)
            }

            this.radarService!!.startService(intent)
            this.radarService!!.bindService(intent, connection, Context.BIND_ABOVE_CLIENT)

            isBound = true
        } catch (ex: IllegalStateException) {
            logger.warn("App is in background. Cannot bind to any further providers.")
        }

    }

    /**
     * Unbind the service from the MainActivity. Call this when the [MainActivity.onStop] is
     * called.
     */
    fun unbind() {
        if (this.radarService == null) {
            throw IllegalStateException("#setRadarService(RadarService) needs to be set before #unbind() is called.")
        }
        if (!isBound) {
            throw IllegalStateException("Service is not bound")
        }
        logger.debug("Unbinding {}", this)
        isBound = false
        this.radarService!!.unbindService(connection)
        connection.onServiceDisconnected(null)
    }

    /**
     * Update the configuration of the service based on the given RadarConfiguration.
     * @throws IllegalStateException if [.getConnection] has not been called
     * yet.
     */
    fun updateConfiguration() {
        if (connection.hasService()) {
            val bundle = Bundle()
            configure(bundle)
            connection.updateConfiguration(bundle)
        }
    }

    /**
     * Configure the service from the set RadarConfiguration.
     */
    @CallSuper
    protected fun configure(bundle: Bundle) {
        // Add the default configuration parameters given to the service intents
        radarService!!.radarApp.configureProvider(bundle)
        val permissions = permissionsNeeded
        bundle.putBoolean(NEEDS_BLUETOOTH_KEY,
                BLUETOOTH in permissions || BLUETOOTH_ADMIN in permissions)
    }

    /** Whether [.getConnection] has already been called.  */
    val isConnected: Boolean
        get() = connectionBacking != null

    override fun toString(): String = "${javaClass.simpleName}<${serviceClass.simpleName}>"

    open val mayBeConnectedInBackground: Boolean = true

    /**
     * Android permissions that the underlying service needs to function correctly.
     */
    abstract val permissionsNeeded: List<String>

    /**
     * Android features (Under PackageManager.FEATURE_) that the provider requires. If the feature
     * is not available, the provider will not be enabled.
     */
    open val featuresNeeded: List<String> = emptyList()

    /** Whether the current service can meaningfully be displayed.  */
    open val isDisplayable: Boolean = true

    /**
     * Whether the device name should be checked with given filters before a connection is allowed
     */
    open val isFilterable: Boolean = false

    /**
     * Match device type.
     *
     * @param sourceType source type
     * @param checkVersion whether to do a strict version check
     */
    private fun matches(sourceType: SourceType?, checkVersion: Boolean): Boolean {
        return (sourceType != null
                && sourceType.producer.equals(sourceProducer, ignoreCase = true)
                && sourceType.model.equals(sourceModel, ignoreCase = true)
                && (!checkVersion || sourceType.catalogVersion.equals(version, ignoreCase = true)))
    }

    /**
     * Whether given provider matches any registered source or source type. If no registration is
     * required, this always returns true. A source with matching type is always considered a match.
     * If no source is registered but registration is dynamic, a match is also made.
     */
    fun isAuthorizedFor(state: AppAuthState, checkVersion: Boolean): Boolean {
        return !state.needsRegisteredSources
                || (state.sourceMetadata.any { matches(it.type, checkVersion) }
                && state.sourceTypes.any { it.hasDynamicRegistration && matches(it, checkVersion) })
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other.javaClass == javaClass
    }

    companion object {
        const val NEEDS_BLUETOOTH_KEY = "org.radarcns.android.device.DeviceServiceProvider.needsBluetooth"
        private val logger = LoggerFactory.getLogger(DeviceServiceProvider::class.java)
    }
}
