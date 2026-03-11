package com.gps.locationtracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.os.Build
import com.gps.locationtracker.data.repository.PreferencesRepository
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            
            try {
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                val format = bundle.getString("format")
                
                val messages = pdus.map { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(it as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(it as ByteArray)
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = PreferencesRepository(context)
                    val triggerKey = prefs.getSMSTriggerKey().first()
                    
                    if (triggerKey.isEmpty()) {
                        Timber.w("SMS received but trigger key is not set")
                        return@launch
                    }

                    for (msg in messages) {
                        val body = msg.messageBody
                        if (body != null && body.contains(triggerKey)) {
                            Timber.d("Trigger key matched in SMS from ${msg.originatingAddress}")
                            triggerLocationCapture(context)
                            break 
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("Error processing SMS: ${e.message}")
            }
        }
    }

    private fun triggerLocationCapture(context: Context) {
        val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
            action = Constants.ACTION_CAPTURE_LOCATION_SMS
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
