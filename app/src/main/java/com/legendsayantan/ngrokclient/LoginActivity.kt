package com.legendsayantan.ngrokclient

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import java.util.*

class LoginActivity : AppCompatActivity() {
    val ngrokAuthTokenUrl = "https://dashboard.ngrok.com/get-started/your-authtoken"
    lateinit var webView: WebView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        webView = findViewById<WebView>(R.id.webview)
        webView.settings.javaScriptEnabled = true
        if(intent.getBooleanExtra("clear",false)){
            CookieManager.getInstance().removeAllCookies(null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.isForceDarkAllowed = true
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient(){
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if(url?.contains("google")==true){
                    webView.stopLoading()
                    webView.loadUrl(ngrokAuthTokenUrl)
                    val intent = Intent(Intent.ACTION_VIEW, ngrokAuthTokenUrl.replace("get-started/your-authtoken","login/google").toUri())
                    startActivity(intent)
                    Toast.makeText(applicationContext, "Please login to ngrok and copy the authtoken.", Toast.LENGTH_LONG).show()
                }else super.onPageStarted(view, url, favicon)
            }
        }
        webView.loadUrl(ngrokAuthTokenUrl)
        waitForToken()
        findViewById<ImageView>(R.id.login).setOnClickListener {
            val tokenText = findViewById<EditText>(R.id.token).text.toString()
            if(tokenText.isEmpty()) {
                Toast.makeText(applicationContext, "Please enter a valid token.", Toast.LENGTH_LONG)
                    .show()
            }else{
                MainActivity.loginUsingToken(applicationContext,tokenText)
                finish()
            }
        }
    }
    private fun waitForToken(){
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        if(webView.url?.contains("setup") == true){
                            webView.loadUrl(ngrokAuthTokenUrl)
                        }
                        if(webView.url.equals(ngrokAuthTokenUrl)){
                            webView.evaluateJavascript("document.querySelector(\"input[class='ant-input']\").value") {
                                println(it)
                                if(it.equals("null")) return@evaluateJavascript
                                MainActivity.loginUsingToken(applicationContext,it)
                                cancel()
                                finish()
                            }
                        }
                    }
                }
            }, 0, 1000
        )
    }
}