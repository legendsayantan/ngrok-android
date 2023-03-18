package com.legendsayantan.ngrokclient

/**
 * @author legendsayantan
 */
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class ApiClient {

    private val client = OkHttpClient()

    // GET request
    fun getRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // POST request
    fun postRequest(url: String, json: String): String? {
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // DELETE request
    fun deleteRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.body?.string()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}