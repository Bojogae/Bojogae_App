package com.bojogae.bojogae_app.utils

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPClient(private val ipAddress: String, private val port: Int) {

    fun sendMessage(message: String) {
        try {
            val socket = DatagramSocket()

            // 서버의 내부 아이피와 포트로 패킷 생성
            val serverAddress = InetAddress.getByName(ipAddress)
            val sendData = message.toByteArray()
            val packet = DatagramPacket(sendData, sendData.size, serverAddress, port)

            // 패킷 전송
            socket.send(packet)
            Log.d("test","메시지 전송: $message")

            // 소켓 닫기
            socket.close()
        } catch (e: Exception) {
            Log.d("test","메시지 실패")
            Log.d("test2",e.stackTraceToString())
            e.printStackTrace()
        }
    }
}

