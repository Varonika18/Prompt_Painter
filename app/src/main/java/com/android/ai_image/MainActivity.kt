package com.android.ai_image
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val BEARER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6IjZkYmNiOWQ1ZTBhNjRlYjFkNTkwYjVkMzU5YWY5OWJiIiwiY3JlYXRlZF9hdCI6IjIwMjQtMDQtMTVUMDg6NDc6MDEuMzQ5Mjc2In0.B2ReoyMqymMS3k7USL7YdqnPdElOth4J4vfYMFtKpG4"

    private lateinit var textView: TextView
    private lateinit var imageView: ImageView
    private lateinit var fetchImageButton: Button
    private lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        imageView = findViewById(R.id.imageView)
        fetchImageButton = findViewById(R.id.fetchImageButton)
        client = OkHttpClient()

        fetchImageButton.setOnClickListener {
            // Start the process to generate the image
            GenerateImageTask().execute()
        }
    }

    inner class GenerateImageTask : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg voids: Void): String? {
            try {
                val mediaType = "application/json".toMediaTypeOrNull()
                val body = RequestBody.create(mediaType, "{\"safe_filter\":true}")
                val request = Request.Builder()
                    .url("https://api.monsterapi.ai/v1/generate/sdxl-base")
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer $BEARER_TOKEN")
                    .build()

                val response = client.newCall(request).execute()
                return response.body?.string()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(responseBody: String?) {
            if (responseBody != null) {
                try {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.getString("status") == "COMPLETED") {
                        val result = jsonResponse.getJSONObject("result")
                        val outputArray = result.getJSONArray("output")
                        if (outputArray.length() > 0) {
                            val imageUrl = outputArray.getString(0) // Assuming we only need the first image URL
                            // Display the image
                            FetchImageTask().execute(imageUrl)
                        } else {
                            // Log the response for debugging
                            Log.e("ResponseError", "No image URLs found in 'output' array")
                            textView.text = "Failed to fetch image URL."
                        }
                    } else {
                        // Log the response for debugging
                        Log.e("ResponseError", "Process is not completed: $responseBody")
                        textView.text = "Image generation is not completed."
                    }
                } catch (e: Exception) {
                    textView.text = "Failed to fetch image URL."
                    e.printStackTrace()
                }
            } else {
                textView.text = "Image generation failed."
            }
        }


    }

    inner class FetchImageTask : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg imageUrls: String): Bitmap? {
            try {
                val imageUrl = imageUrls[0]
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val inputStream = connection.inputStream
                return BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(bitmap: Bitmap?) {
            if (bitmap != null) {
                // Display the image
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
                textView.visibility = View.GONE
            } else {
                textView.text = "Failed to fetch image."
            }
        }
    }
}
