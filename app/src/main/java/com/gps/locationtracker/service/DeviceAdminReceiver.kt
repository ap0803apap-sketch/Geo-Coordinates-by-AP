package com.gps.locationtracker.service

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import timber.log.Timber

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Timber.d("Device Admin: Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Timber.d("Device Admin: Disabled")
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, com.gps.locationtracker.service.DeviceAdminReceiver::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val admin = getComponentName(context)
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(admin)
        }

        fun removeAdmin(context: Context) {
            try {
                val admin = getComponentName(context)
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (dpm.isAdminActive(admin)) {
                    dpm.removeActiveAdmin(admin)
                    Timber.d("Device Admin removal requested")
                }
            } catch (e: Exception) {
                Timber.e("Error removing device admin: ${e.message}")
            }
        }

        fun activateAdmin(context: Context) {
            try {
                val admin = getComponentName(context)
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "This permission is required to prevent unauthorized uninstallation of the tracking app.")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Timber.d("Device Admin activation screen launched")
            } catch (e: Exception) {
                Timber.e("Error activating device admin: ${e.message}")
            }
        }
    }
}
