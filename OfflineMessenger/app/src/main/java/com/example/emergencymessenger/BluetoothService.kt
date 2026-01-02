package com.example.emergencymessenger

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class BluetoothService(context: Context, private val handler: Handler) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var secureAcceptThread: AcceptThread? = null
    private var insecureAcceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var state: Int = STATE_NONE
    private var newState: Int = STATE_NONE

    companion object {
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_LISTEN = 1     // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3  // now connected to a remote device
        private const val TAG = "BluetoothService"
    }

    @Synchronized
    fun getState(): Int {
        return state
    }

    @Synchronized
    fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        connectThread?.cancel()
        connectThread = null

        // Cancel any thread currently running a connection
        connectedThread?.cancel()
        connectedThread = null

        // Start the thread to listen on a BluetoothServerSocket
        if (secureAcceptThread == null) {
            secureAcceptThread = AcceptThread(true)
            secureAcceptThread?.start()
        }
        if (insecureAcceptThread == null) {
            insecureAcceptThread = AcceptThread(false)
            insecureAcceptThread?.start()
        }
    }

    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            connectThread?.cancel()
            connectThread = null
        }

        // Cancel any thread currently running a connection
        connectedThread?.cancel()
        connectedThread = null

        // Start the thread to connect with the given device
        connectThread = ConnectThread(device, secure)
        connectThread?.start()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type:$socketType")

        // Cancel the thread that completed the connection
        connectThread?.cancel()
        connectThread = null

        // Cancel any thread currently running a connection
        connectedThread?.cancel()
        connectedThread = null

        // Cancel the accept thread because we only want to connect to one device
        secureAcceptThread?.cancel()
        secureAcceptThread = null
        insecureAcceptThread?.cancel()
        insecureAcceptThread = null

        // Start the thread to manage the connection and perform transmissions
        connectedThread = ConnectedThread(socket, socketType)
        connectedThread?.start()

        // Send the name of the connected device back to the UI Activity
        val msg = handler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.name)
        msg.data = bundle
        handler.sendMessage(msg)

        state = STATE_CONNECTED
    }

    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        connectThread?.cancel()
        connectThread = null
        connectedThread?.cancel()
        connectedThread = null
        secureAcceptThread?.cancel()
        secureAcceptThread = null
        insecureAcceptThread?.cancel()
        insecureAcceptThread = null
        state = STATE_NONE
    }

    fun write(out: ByteArray) {
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != STATE_CONNECTED) return
            r = connectedThread
        }
        // Perform the write unsynchronized
        r?.write(out)
    }
    
    // Server Thread
    @SuppressLint("MissingPermission")
    private inner class AcceptThread(secure: Boolean) : Thread() {
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String = if (secure) "Secure" else "Insecure"

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                if (secure) {
                    tmp = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(Constants.NAME_SECURE, Constants.MY_UUID_SECURE)
                } else {
                    tmp = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(Constants.NAME_INSECURE, Constants.MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: $mSocketType listen() failed", e)
            }
            mmServerSocket = tmp
            state = STATE_LISTEN
        }

        override fun run() {
            Log.d(TAG, "Socket Type: $mSocketType BEGIN mAcceptThread $this")
            name = "AcceptThread$mSocketType"

            var socket: BluetoothSocket?

            while (state != STATE_CONNECTED) {
                socket = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: $mSocketType accept() failed", e)
                    break
                }

                socket?.let {
                    synchronized(this@BluetoothService) {
                        when (state) {
                            STATE_LISTEN, STATE_CONNECTING -> {
                                // Situation normal. Start the connected thread.
                                connected(it, it.remoteDevice, mSocketType)
                            }
                            STATE_NONE, STATE_CONNECTED -> {
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    it.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: $mSocketType")
        }

        fun cancel() {
            Log.d(TAG, "Socket Type $mSocketType cancel $this")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type $mSocketType close() of server failed", e)
            }
        }
    }

    // Client Thread
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String = if (secure) "Secure" else "Insecure"

        init {
            var tmp: BluetoothSocket? = null
            try {
                if (secure) {
                    tmp = mmDevice.createRfcommSocketToServiceRecord(Constants.MY_UUID_SECURE)
                } else {
                    tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(Constants.MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: $mSocketType create() failed", e)
            }
            mmSocket = tmp
            state = STATE_CONNECTING
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter?.cancelDiscovery()

            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() $mSocketType socket during connection failure", e2)
                }
                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothService) {
                connectThread = null
            }

            // Start the connected thread
            connected(mmSocket!!, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }
        }
    }

    // Connected Thread (Exchange Data)
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, socketType: String) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (state == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer)
                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream.write(buffer)
                // Share the sent message back to the UI Activity
                handler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    private fun connectionFailed() {
        val msg = handler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg.data = bundle
        handler.sendMessage(msg)

        state = STATE_NONE
        // Start the service over to restart listening mode
        this@BluetoothService.start()
    }

    private fun connectionLost() {
        val msg = handler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg.data = bundle
        handler.sendMessage(msg)

        state = STATE_NONE
        // Start the service over to restart listening mode
        this@BluetoothService.start()
    }
}
