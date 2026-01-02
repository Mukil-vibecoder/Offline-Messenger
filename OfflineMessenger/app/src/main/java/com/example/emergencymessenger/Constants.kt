package com.example.emergencymessenger

import java.util.UUID

object Constants {
    // Message types sent from the BluetoothChatService Handler
    const val MESSAGE_STATE_CHANGE = 1
    const val MESSAGE_READ = 2
    const val MESSAGE_WRITE = 3
    const val MESSAGE_DEVICE_NAME = 4
    const val MESSAGE_TOAST = 5

    // Key names received from the BluetoothChatService Handler
    const val DEVICE_NAME = "device_name"
    const val TOAST = "toast"

    // UUID for this application
    val MY_UUID_SECURE: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    val MY_UUID_INSECURE: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    const val NAME_SECURE = "BluetoothChatSecure"
    const val NAME_INSECURE = "BluetoothChatInsecure"
}
