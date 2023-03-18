package com.legendsayantan.ngrokclient

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import me.ibrahimsn.lib.SmoothBottomBar
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity() {
    var ngrokThread: NgrokThread? = null
    lateinit var webView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<ImageView>(R.id.logout).setOnClickListener {
            logout(applicationContext)
            startActivity(
                Intent(applicationContext, LoginActivity::class.java).putExtra(
                    "clear",
                    true
                )
            )
        }
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object  : WebViewClient(){
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            webView.reload()
                        }
                    }
                }, 1000)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return if(url.contains("ngrok")){
                    //copy to clipboard
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ngrok url", url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    true
                }else false
            }
        }
        webView.webChromeClient = WebChromeClient()
        findViewById<MaterialCardView>(R.id.startButton).setOnClickListener {
            if(ngrokThread==null || !ngrokThread!!.running){
                if (findViewById<EditText>(R.id.port).text.toString().isNotEmpty()) {
                    startTunnel(
                        if (findViewById<RadioButton>(R.id.http).isChecked) "http" else "tcp",
                        findViewById<EditText>(R.id.port).text.toString().toInt()
                    )
                }else{
                    findViewById<EditText>(R.id.port).error = "Port cannot be empty"
                }
            }else{
                ngrokThread!!.stopTunnel()
                workWithBottomBar(2)
                initialiseUi()
            }
        }
        findViewById<SmoothBottomBar>(R.id.bottomBar).onItemSelected = {
            if(ngrokThread!=null && ngrokThread!!.running){
                workWithBottomBar(it)
            }
        }
        val protocol = intent.getStringExtra("protocol")
        val port = intent.getIntExtra("port",-1)
        if(authenticated(applicationContext)){
            if(protocol!=null && port!=-1){
                if(ngrokThread!=null && ngrokThread!!.running){
                    ngrokThread!!.stopTunnel()
                }
                startTunnel(protocol,port)
            }
        }else if(protocol!=null && port!=-1) {
            Toast.makeText(applicationContext,"Please login first to start tunnel",Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        var file = File(filesDir, "ngrok")
        if (!file.exists()) {
            copyNgrokBinary()
            file.setExecutable(true)
            Runtime.getRuntime().exec("chmod +x ${filesDir}/ngrok")
        }
        if (!authenticated(applicationContext)) {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            return
        }
    }
    fun workWithBottomBar(position: Int){
        when (position) {
            0 -> {
                findViewById<ListView>(R.id.listView).visibility = View.GONE
                findViewById<MaterialCardView>(R.id.webviewCard).visibility = View.VISIBLE
                webView.loadUrl("http://127.0.0.1:4040/status")
            }
            1 -> {
                findViewById<ListView>(R.id.listView).visibility = View.GONE
                findViewById<MaterialCardView>(R.id.webviewCard).visibility = View.VISIBLE
                webView.loadUrl("http://127.0.0.1:4040/inspect/http")
            }
            2 -> {
                findViewById<ListView>(R.id.listView).visibility = View.VISIBLE
                findViewById<MaterialCardView>(R.id.webviewCard).visibility = View.GONE
            }
        }
    }
    @Throws(IOException::class)
    private fun copyNgrokBinary() {
        val inputStream: InputStream = assets.open("ngrok")
        val file = File(filesDir, "ngrok")
        file.parentFile?.mkdirs()
        file.createNewFile()
        val outputStream: OutputStream = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    private fun startTunnel(type: String, port: Int) {
        ngrokThread = NgrokThread(applicationContext, type, port)
        ngrokThread!!.onTunnelCreate = {
            initialiseUi()
            workWithBottomBar(findViewById<SmoothBottomBar>(R.id.bottomBar).itemActiveIndex)
        }
        ngrokThread?.start()
        findViewById<ListView>(R.id.listView).adapter = ArrayAdapter(applicationContext,R.layout.list_item,ngrokThread!!.logs)
    }
    fun initialiseUi(){
        TransitionManager.beginDelayedTransition(findViewById(R.id.root))
        if(ngrokThread==null || !ngrokThread!!.running){
            findViewById<LinearLayout>(R.id.startControls).visibility = LinearLayout.VISIBLE
            findViewById<ImageView>(R.id.btnImage).setImageResource(R.drawable.baseline_play_arrow_24)
            findViewById<TextView>(R.id.btnText).text = "START"
        }else{
            findViewById<LinearLayout>(R.id.startControls).visibility = LinearLayout.GONE
            findViewById<ImageView>(R.id.btnImage).setImageResource(R.drawable.baseline_close_24)
            findViewById<TextView>(R.id.btnText).text = "STOP"
        }
    }


    companion object {
        fun loginUsingToken(context: Context, token: String) {
            val configFile = File("${context.filesDir}/.ngrok2", "ngrok.yml")
            configFile.parentFile?.mkdirs()
            configFile.createNewFile()
            configFile.writeText("authtoken: ${token.replace("\"", "")}")
        }

        fun logout(context: Context) {
            val configFile = File("${context.filesDir}/.ngrok2", "ngrok.yml")
            configFile.delete()
        }

        fun authenticated(context: Context): Boolean {
            val configFile = File("${context.filesDir}/.ngrok2", "ngrok.yml")
            return configFile.exists()
        }
    }

}