package com.bojogae.bojogae_app.test.wifi_direct

import java.io.*
import java.net.Socket

class SocketClientThread(private val hostAddress: String, private val message: String) : Thread() {
    private val serverPort = 8888

    override fun run() {
        try {
            val socket = Socket(hostAddress, serverPort)
            val output = PrintWriter(socket.getOutputStream(), true)
            output.println(message)
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}