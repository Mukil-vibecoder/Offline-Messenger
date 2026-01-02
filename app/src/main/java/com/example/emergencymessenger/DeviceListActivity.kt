package com.example.emergencymessenger

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class DeviceListActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var newDevicesArrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request window feature for custom layout if needed, but we use AppCompat which has ActionBar
        setContentView(R.layout.activity_device_list)

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED)

        val scanButton = findViewById<Button>(R.id.button_scan)
        scanButton.setOnClickListener {
            doDiscovery()
            it.visibility = View.GONE
        }

        val pairedDevicesArrayAdapter = ArrayAdapter<String>(this, R.layout.device_name)
        newDevicesArrayAdapter = ArrayAdapter<String>(this, R.layout.device_name)

        val pairedListView = findViewById<ListView>(R.id.paired_devices)
        pairedListView.adapter = pairedDevicesArrayAdapter
        pairedListView.onItemClickListener = deviceClickListener

        val newDevicesListView = findViewById<ListView>(R.id.new_devices)
        newDevicesListView.adapter = newDevicesArrayAdapter
        newDevicesListView.onItemClickListener = deviceClickListener

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Get a set of currently paired devices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Permission not granted
        }
        
        val pairedDevices = bluetoothAdapter?.bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (!pairedDevices.isNullOrEmpty()) {
            findViewById<View>(R.id.title_paired_devices).visibility = View.VISIBLE
            for (device in pairedDevices) {
                pairedDevicesArrayAdapter.add("${device.name}\n${device.address}")
            }
        } else {
            val noDevices = resources.getText(R.string.none_paired).toString()
            pairedDevicesArrayAdapter.add(noDevices)
        }

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
        unregisterReceiver(receiver)
    }

    private fun doDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
             return
        }
        
        // If we're already discovering, stop it
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter?.startDiscovery()
    }

    private val deviceClickListener = AdapterView.OnItemClickListener { _, v, _, _ ->
        bluetoothAdapter?.cancelDiscovery()

        // Get the device MAC address, which is the last 17 chars in the View
        val info = (v as TextView).text.toString()
        if (info.length >= 17) {
            val address = info.substring(info.length - 17)

            // Create the result Intent and include the MAC address
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
            startActivity(intent)
            finish()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                         if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                             // skip name if perm missing (shouldn't happen here)
                         }
                        if (device.bondState != BluetoothDevice.BOND_BONDED) {
                            val name = device.name ?: "Unknown Device" // Handle null names
                            newDevicesArrayAdapter.add("$name\n${device.address}")
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (newDevicesArrayAdapter.count == 0) {
                        val noDevices = resources.getText(R.string.none_found).toString()
                         // Avoid adding multiple times
                         if (newDevicesArrayAdapter.getPosition(noDevices) == -1) {
                             newDevicesArrayAdapter.add(noDevices)
                         }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }
}
