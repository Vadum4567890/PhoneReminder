package com.vrma.blenotify

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID


object selectedRingtone {
    var uri: Uri? = null
    var name: String? = null
    var PREFS_NAME = "RINGTONE_STORAGE"
}

object selectedBluetoothDevice {
    var device: BluetoothDevice? = null
}

class MainActivity : AppCompatActivity() {
    val ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 4

    val TAG = "bilson"
    lateinit var listView: ListView
    lateinit var mBluetoothAdapter: BluetoothAdapter
    private val mDeviceList = ArrayList<String>()
    private val mBleDeviceList = ArrayList<BluetoothDevice>()

    private val REQUEST_ENABLE_BT = 5647

    var actionState: String? = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getRingtoneInitial()

        if (!isLocationEnabled(applicationContext)) {
            takeUserToLocationSettings()
        }

        listView = findViewById<View>(R.id.bleDeviceList) as ListView

        ///Scan Button
        findViewById<MaterialButton>(R.id.scan_button).setOnClickListener({
            if (!isLocationEnabled(applicationContext)) {
                takeUserToLocationSettings()
            } else if (mBluetoothAdapter != null || !mBluetoothAdapter.isEnabled()) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        ASK_MULTIPLE_PERMISSION_REQUEST_CODE
                    );
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                if (mBluetoothAdapter != null) {
                    Toast.makeText(applicationContext, "Scanning...", Toast.LENGTH_SHORT).show()
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    }
                    mBluetoothAdapter.startDiscovery()
                }
            }

        })

        ///go to settings activity
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener({
            startActivity(Intent(this, SettingsActivity::class.java))
        })

        val bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.getAdapter()


        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(mReceiver, filter)


        //list view item click
        listView.setOnItemClickListener { parent, view, position, id ->
            try {
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage("Do you want to connect to ${mBleDeviceList[position].name}")
                    // if the dialog is cancelable
                    .setCancelable(false)
                    .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                        Log.d(TAG, "Already Bonded ${mBleDeviceList[position].bondState}")

                        if (mBleDeviceList[position].bondState == BluetoothDevice.BOND_BONDED) {
//                            connectToDevice(mBleDeviceList[position])
                            takeUserToSettings(mBleDeviceList[position].name)
                            dialog.dismiss()
                        } else {
                            try {
                                mBleDeviceList[position].createBond()
                            } catch (e: Exception) {
                                Log.d(TAG, "CREATEBOND ${e.localizedMessage} and ${e.stackTrace}")
                            }
                            Toast.makeText(
                                applicationContext,
                                "Connecting to ${mBleDeviceList[position].name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()

                        }
                    })
                    .setNegativeButton("No", { dialog, id ->
                        dialog.dismiss()
                    })

                val alert = dialogBuilder.create()
                alert.setTitle("Bluetooth Connection")

                alert.show()
            } catch (e: Exception) {
                Log.d(TAG, "onCreate: ${e.message}")
            }

        }
        startService(Intent(this, BluetoothService::class.java))

        checkPermissions()

//        Log.d(TAG, "onCreate: BLEDEVICE NAME ${getConnectedBluetoothDevice(applicationContext)?.name}")
//        findViewById<TextView>(R.id.connectedBleDevice).text =
//            getConnectedBluetoothDevice(applicationContext)?.name

    }

    private fun takeUserToLocationSettings() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Please enable location from the settings")
            // if the dialog is cancelable
            .setCancelable(false)
            .setPositiveButton("Yes, take me there", DialogInterface.OnClickListener { dialog, id ->
                try {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    applicationContext.startActivity(intent)
                } catch (e: Exception) {
                    TODO("Not yet implemented")
                }

            })
            .setNegativeButton("No", { dialog, id ->
                dialog.dismiss()
            })

        val alert = dialogBuilder.create()
        alert.setTitle("Location Services")

        alert.show()
    }

    private fun takeUserToSettings(name: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Please connect or remove device from bluetooth settings")
            // if the dialog is cancelable
            .setCancelable(false)
            .setPositiveButton("Yes ,take me there", DialogInterface.OnClickListener { dialog, id ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
                dialog.dismiss()

            })
            .setNegativeButton("No", { dialog, id ->
                dialog.dismiss()
            })

        val alert = dialogBuilder.create()
        alert.setTitle("Device[${name}] Already Paired !!!")

        alert.show()
    }
    ///on create ends

//    fun getConnectedBluetoothDevice(context: Context): BluetoothDevice? {
//
//        val bluetoothManager =
//            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//        }
//        val bluetoothProfile = bluetoothManager.getConnectedDevices(BluetoothProfile.STATE_CONNECTED)
//        return if (bluetoothProfile.isNotEmpty()) {
//            bluetoothProfile[0]
//        } else {
//            null
//        }
//    }

    fun generateUUIDFromAddress(device: BluetoothDevice): UUID {
        val deviceAddress = device.address
        val bytes = deviceAddress.toByteArray(StandardCharsets.UTF_8)
        return UUID.nameUUIDFromBytes(bytes)
    }

    fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice: Inside")
        // Create a UUID for the connection
        val uuid: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

        // Create a BluetoothSocket for the connection

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {

        }
        Log.d(TAG, "connectToDevice: permission checked")

        var socket: BluetoothSocket? = null
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
        } catch (e: Exception) {
            Log.d(TAG, "connectToDevice: ${e.localizedMessage}")
        }

        // Check if the socket was created successfully
        if (socket == null) {
            Log.d(TAG, "connectToDevice: socket creation failed")
        }

        // Connect to the device
        try {
            Log.d(TAG, "connectToDevice: socket connected")

            socket?.connect()
            // Connection successful, do further operations with the socket
        } catch (e: IOException) {
            // Connection failed
        }
    }

    private fun checkPermissions() {
        val permissionsDenied = mutableListOf<String>()
        val permissions = arrayOf<String>(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
//            Manifest.permission.BLUETOOTH_SCAN,
//            Manifest.permission.BLUETOOTH_CONNECT,
//            Manifest.permission.POST_NOTIFICATIONS,
        )

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsDenied.add(permission)
            }
        }
        Log.d(TAG, "checkPermissions: ${permissionsDenied}")

        if (permissionsDenied.isNotEmpty()) {
            try {
                showPermissionDialog()
            } catch (e: Exception) {
                Log.d(TAG, "checkPermissions: ${e.message}")
            }
        }
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        return gpsEnabled || networkEnabled
    }

    private fun showPermissionDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Permission Request")
        dialogBuilder.setMessage("Please grant the necessary permissions.")
            // if the dialog is cancelable
            .setCancelable(false)
            .setPositiveButton("Allow", DialogInterface.OnClickListener { dialog, id ->

                requestPermissions(
                    arrayOf<String>(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ),
                    ASK_MULTIPLE_PERMISSION_REQUEST_CODE
                )
                dialog.dismiss()

            })

        val alert = dialogBuilder.create()
        alert.setTitle("Permission Request")
        alert.setCancelable(false)

        alert.show()
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) {
                actionState = intent.action
            }

            ///setting currently connected device
            try {
                if (actionState == BluetoothDevice.ACTION_ACL_CONNECTED && action == BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) {
                    val device = intent
                        .getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    findViewById<TextView>(R.id.connectedBleDevice).text =
                        "Currently Connected device : ${device?.name}"

                }
                if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    findViewById<TextView>(R.id.connectedBleDevice).text = ""


                }
            } catch (e: Exception) {
                Log.d(TAG, "onReceiveError: ${e.localizedMessage}")
            }

            Log.d(TAG, "onReceive: action ${action}")
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent
                    .getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        ASK_MULTIPLE_PERMISSION_REQUEST_CODE
                    )
                }
                if (device!!.name != null) {
                    if (!mDeviceList.contains(device!!.name)) {
                        mBleDeviceList.add(device!!)
                        mDeviceList.add(
                            device.name
                        )
                    } else {
                        Log.d(TAG, "onReceive: bleListSize before ${mBleDeviceList.size}")
                        Log.d(TAG, "onReceive: devicenameSize before ${mDeviceList.size}")
                        mBleDeviceList.remove(device)
                        mDeviceList.remove(device.name)
                        Log.d(TAG, "onReceive: bleListSize after ${mBleDeviceList.size}")
                        Log.d(TAG, "onReceive: devicenameSize after ${mDeviceList.size}")

                        mBleDeviceList.add(device)
                        mDeviceList.add(device.name)
                    }
                }
                mDeviceList.distinct()
                listView.adapter = ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_list_item_1, mDeviceList
                )
            }
        }
    }

    fun getRingtoneInitial() {

        var prefs = getSharedPreferences(selectedRingtone.PREFS_NAME, 0)
        var ringUriString = prefs.getString("ringtoneURI", "NOT_FOUND");
        var ringUri = Uri.parse(ringUriString)

        if (ringUriString != "NOT_FOUND") {
            selectedRingtone.uri = ringUri
        } else {
            val manager = RingtoneManager(this)
            manager.setType(RingtoneManager.TYPE_NOTIFICATION)
            val cursor = manager.cursor
            cursor.moveToPosition(1)

            val ringtoneURI = manager.getRingtoneUri(cursor.position)
            // Do something with the title and the URI of ringtone
            selectedRingtone.uri = ringtoneURI
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {

                if (mBluetoothAdapter != null) {
                    Toast.makeText(applicationContext, "Scanning...", Toast.LENGTH_SHORT).show()
//                    if (ActivityCompat.checkSelfPermission(
//                            this,
//                            Manifest.permission.BLUETOOTH_SCAN
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        Toast.makeText(
//                            applicationContext,
//                            "Need BLUETOOTH_SCAN Permission ",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        requestPermissions(
//                            arrayOf<String>(
//                                Manifest.permission.BLUETOOTH_SCAN,
//                            ),
//                            ASK_MULTIPLE_PERMISSION_REQUEST_CODE
//                        )
//
//                    }
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    }
                    mBluetoothAdapter.startDiscovery()
                }

            } else {
                // Bluetooth enabling was canceled or failed
                // Handle this case as needed
                // ...
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }


}