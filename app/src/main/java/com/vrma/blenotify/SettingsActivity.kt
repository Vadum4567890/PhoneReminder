package com.vrma.blenotify

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnTouchListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class SettingsActivity : AppCompatActivity() {

    val ringtoneMap: MutableMap<String, Uri?> = HashMap()

    lateinit var ringtone: Ringtone
    var isRingtoneSpinnerTouched: Boolean = false
    var isBleSpinnerTouched: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        getRingtonesList()

        val ble_spinner = findViewById<Spinner>(R.id.spinner_ble_settings)
        val ble_items = arrayOf("What do you want to do?", "Disconnect")
        val ble_adapter = ArrayAdapter(
            this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, ble_items
        )
        ble_spinner.adapter = ble_adapter
        ble_spinner.setOnTouchListener(OnTouchListener { v, event ->
            isBleSpinnerTouched = true
            false
        })
        ble_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                try {
                    if (isBleSpinnerTouched) {
                        Toast.makeText(
                            applicationContext,
                            "Another Device is using this device cannot interfere",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "${e.localizedMessage}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }
        ble_spinner.setSelection(0)

        val ring_spinner = findViewById<Spinner>(R.id.spinner_ringtone)
        val ring_items = ringtoneMap.keys.toList()
        val ring_adapter = ArrayAdapter(
            this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, ring_items
        )
        ring_spinner.adapter = ring_adapter



        ring_spinner.setOnTouchListener(OnTouchListener { v, event ->
            isRingtoneSpinnerTouched = true
            false
        })
        ring_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (isRingtoneSpinnerTouched) {
                    try {
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                        }
                    } catch (e: Exception) {
                        print(e.message)
                    }
                    val alert = ringtoneMap[parent?.getItemAtPosition(position)]
                    ringtone = RingtoneManager.getRingtone(applicationContext, alert)
                    ringtone.streamType = AudioManager.STREAM_RING
                    ringtone.play()
                    var title = ""
                    ringtoneMap.keys.forEach({
                        if (it.equals(parent?.getItemAtPosition(position).toString())) {
                            title = parent?.getItemAtPosition(position).toString();
                        }
                    })
                    Toast.makeText(applicationContext, "${title} selected.", Toast.LENGTH_SHORT)
                        .show()
                    findViewById<TextView>(R.id.ringtone_tv).text = title
                    selectedRingtone.name = title
                    selectedRingtone.uri = ringtoneMap[parent?.getItemAtPosition(position)]
                    ///store ringtone

                    try {
                        var prefs = getSharedPreferences(selectedRingtone.PREFS_NAME, 0).edit()
                        prefs.putString(
                            "ringtoneURI",
                            ringtoneMap[parent?.getItemAtPosition(position)].toString()
                        )
                        prefs.commit()
                    } catch (e: Exception) {
                        TODO("Not yet implemented")
                    }

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
    }

    fun getRingtonesList() {
        val manager = RingtoneManager(this)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val ringtoneURI = manager.getRingtoneUri(cursor.position)
            // Do something with the title and the URI of ringtone
            ringtoneMap[title] = ringtoneURI;
        }
    }
}