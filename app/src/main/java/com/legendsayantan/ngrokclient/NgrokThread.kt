package com.legendsayantan.ngrokclient

import android.content.Context
import androidx.core.content.ContextCompat
import okhttp3.internal.wait
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author legendsayantan
 */
class NgrokThread(var context: Context,var type :String,var port:Int) :Thread() {
    var apiClient = ApiClient()
    val logs = ArrayList<String>()
    var running = false
    var onTunnelCreate = {}
    lateinit var process : Process
    override fun run() {
        running = true
        val command = arrayOf("${context.filesDir}/ngrok", type, port.toString())
        val pb = ProcessBuilder(*command)
        pb.environment()["USER"] = 1.toString()
        pb.environment()["HOME"] = context.filesDir.absolutePath
        pb.redirectErrorStream(true)
        process = pb.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        ContextCompat.getMainExecutor(context).execute {
            onTunnelCreate()
        }
        try {
            while (reader.readLine().also { line = it } != null) {
                line?.let { logs.add(it) }
            }
            reader.close()
            process.waitFor()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
    override fun interrupt() {
        super.interrupt()
        running = false
    }
    fun lookForTunnels(onTunnelInfo: (String?) -> Unit) {
        Thread{
            val data = apiClient.getRequest("http://127.0.0.1:4040/api/tunnels/command_line")
            ContextCompat.getMainExecutor(context).execute {
                onTunnelInfo(data)
            }
        }.start()
    }
    fun stopTunnel() {
        Thread{
            apiClient.deleteRequest("http://127.0.0.1:4040/api/tunnels/command_line")
        }.start()
        process.destroyForcibly()
        this.interrupt()
    }
}