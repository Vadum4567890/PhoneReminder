package com.vrma.blenotify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat


class BluetoothReceiver : BroadcastReceiver() {

    var isNotificationChannelCreated = false
    lateinit var mChannel: NotificationChannel
    val channelId = "all_notifications"
    lateinit var notificationManager: NotificationManager
    var actionState: String? = ""
    var deviceIsPaired = false

    var TAG = "bilson"
    override fun onReceive(context: Context, intent: Intent) {
        if (!isNotificationChannelCreated) {
            createNotificationChannel(context)
            isNotificationChannelCreated = true
        }
        val action = intent.action

        if (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) {
            actionState = intent.action
        }

//        if (action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
//            connectionChanged = !connectionChanged
//            Log.d("bilson", "con state: ${connectionChanged}}")
//        }

        Log.d("bilson", "onReceiveIntent: ${intent.action}")

        if (actionState == BluetoothDevice.ACTION_ACL_CONNECTED && action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            // You can perform any necessary actions here
            Log.d(TAG, "preparing connected notif: ")
            try {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                }
                selectedBluetoothDevice.device = device
                deviceIsPaired = true
                sendNotification(
                    context, "Device Connected", "${device?.name} device connected"
                )
            } catch (e: Exception) {
                Log.d("bilson", "Connected Error: ${e.message}}")
            }
        } else if (actionState == BluetoothDevice.ACTION_ACL_DISCONNECTED && deviceIsPaired) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            deviceIsPaired = false
            try {
                selectedBluetoothDevice.device = null
                sendNotification(
                    context, "Take your phone!", "${device?.name} device disconnected"
                )
            } catch (e: Exception) {
                Log.d("bilson", "Disconnected Error: ${e.message}}")
            }
        }
    }

    fun createNotificationChannel(mContext: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // You should create a String resource for this instead of storing in a variable
            mChannel = NotificationChannel(
                channelId, "General Notifications", NotificationManager.IMPORTANCE_DEFAULT
            )
            mChannel.description = "This is default channel used for all other notifications"
            mChannel.setSound(null, null)

            notificationManager =
                mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    fun sendNotification(context: Context, title: String, description: String) {
//        createNotificationChannel(context)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, Intent(), PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        );
        val builder =
            NotificationCompat.Builder(context, channelId) // Create notification with channel Id
                .setSmallIcon(R.mipmap.ic_launcher_round).setContentTitle(title)
                .setContentText(description).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSilent(true)
        builder.setContentIntent(pendingIntent).setAutoCancel(false)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            val mp: MediaPlayer = MediaPlayer.create(context, selectedRingtone.uri)
            mp.start()
        } catch (e: Exception) {
            ///error handling
        }
        actionState = " "
        with(mNotificationManager) {
            notify(123, builder.build())

        }
    }

}
