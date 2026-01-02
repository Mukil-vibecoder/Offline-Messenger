package com.example.emergencymessenger

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ChatActivity : AppCompatActivity() {

    private var chatService: BluetoothService? = null
    private var outStringBuffer: StringBuffer? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectedDeviceName: String? = null
    private lateinit var conversationAdapter: ArrayAdapter<String>
    private lateinit var messageListView: ListView
    private lateinit var outEditText: EditText
    private lateinit var btnSend: Button
    private lateinit var btnSOS: Button
    
    // Forwarding
    private var messageToForward: String? = null

    companion object {
        const val REQUEST_CONNECT_DEVICE_SECURE = 1
        const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        const val REQUEST_ENABLE_BT = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup UI
        messageListView = findViewById(R.id.in)
        outEditText = findViewById(R.id.edit_text_out)
        btnSend = findViewById(R.id.button_send)
        btnSOS = findViewById(R.id.btnSOS)

        conversationAdapter = ArrayAdapter(this, R.layout.message_item)
        messageListView.adapter = conversationAdapter
        
        // Manual Forwarding Logic
        messageListView.setOnItemLongClickListener { _, _, position, _ ->
            val message = conversationAdapter.getItem(position)
            if (message != null) {
                showForwardDialog(message)
            }
            true
        }

        btnSend.setOnClickListener {
            val view = findViewById<android.view.View>(R.id.edit_text_out)
            if (view is android.widget.TextView) {
                val message = view.text.toString()
                sendMessage(message)
            }
        }
        
        btnSOS.setOnClickListener {
            sendMessage("!!! SOS - EMERGENCY !!!")
        }

        // Check if we started with a device to connect to (from DeviceListActivity)
        val address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        if (address != null) {
             // We will connect in onResume or setupChat
        }
    }

    override fun onStart() {
        super.onStart()
        if (bluetoothAdapter?.isEnabled == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // Permission check skipped for brevity (handled in Main)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else if (chatService == null) {
            setupChat()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatService?.stop()
    }

    override fun onResume() {
        super.onResume()
        if (chatService != null) {
            if (chatService!!.getState() == BluetoothService.STATE_NONE) {
                chatService!!.start()
            }
        }
        
        // Handle auto-connect if passed via intent (initial launch)
        val address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        if (address != null && chatService?.getState() != BluetoothService.STATE_CONNECTED && chatService?.getState() != BluetoothService.STATE_CONNECTING) {
             connectDevice(address, true)
             // Clear intent extra so we don't reconnect on rotation
             intent.removeExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        }
    }

    private fun setupChat() {
        chatService = BluetoothService(this, handler)
        outStringBuffer = StringBuffer("")
    }
    
    private fun showForwardDialog(message: String) {
        val cleanMessage = if (message.startsWith("Me: ")) message.substring(4) else if (message.startsWith("Remote: ")) message.substring(8) else message
        
        AlertDialog.Builder(this)
            .setTitle("Forward Message")
            .setMessage("Do you want to disconnect current session and forward this message to another device?\n\n\"$cleanMessage\"")
            .setPositiveButton("Yes") { _, _ ->
                messageToForward = cleanMessage
                val serverIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun sendMessage(message: String) {
        if (chatService?.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.title_not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isNotEmpty()) {
            val send = message.toByteArray()
            chatService?.write(send)
            outStringBuffer?.setLength(0)
            outEditText.setText(outStringBuffer)
        }
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> {
                    when (msg.arg1) {
                        BluetoothService.STATE_CONNECTED -> {
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName))
                            conversationAdapter.clear()
                            
                            // Check if we have a message to forward
                            if (messageToForward != null) {
                                sendMessage("FWD: $messageToForward")
                                messageToForward = null // Reset
                            }
                        }
                        BluetoothService.STATE_CONNECTING -> setStatus(R.string.title_connecting)
                        BluetoothService.STATE_LISTEN, BluetoothService.STATE_NONE -> setStatus(R.string.title_not_connected)
                    }
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    conversationAdapter.add("Me: $writeMessage")
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    conversationAdapter.add("Remote: $readMessage")
                    
                    // ALERT SOUND for SOS
                    if (readMessage.contains("SOS")) {
                         // Simple beep or vibration could be added here
                         Toast.makeText(applicationContext, "SOS RECEIVED!", Toast.LENGTH_LONG).show()
                    }
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    connectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    Toast.makeText(applicationContext, "Connected to $connectedDeviceName", Toast.LENGTH_SHORT).show()
                }
                Constants.MESSAGE_TOAST -> {
                    if (Build.VERSION.SDK_INT < 30) // Avoid background toast crashes on newer android if background
                        Toast.makeText(applicationContext, msg.data.getString(Constants.TOAST), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setStatus(resId: Int) {
        val subTitle = resources.getString(resId)
        supportActionBar?.subtitle = subTitle
    }

    private fun setStatus(subTitle: CharSequence) {
        supportActionBar?.subtitle = subTitle
    }
    
    private fun connectDevice(address: String, secure: Boolean) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        device?.let { chatService?.connect(it, secure) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val address = data.extras?.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
                    address?.let { connectDevice(it, true) }
                }
            }
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    setupChat()
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Make Discoverable")
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
             ensureDiscoverable()
             return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun ensureDiscoverable() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
             // Basic check
        }
        if (bluetoothAdapter?.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }
}
