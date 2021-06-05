package com.example.firebase

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson

class HTTPQueryActivity : AppCompatActivity() {

    var url : String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_httpquery)

        val textView: TextView = findViewById(R.id.text)

        val queue: RequestQueue = Volley.newRequestQueue(this)
        url = "https://api.openuv.io/api/v1/protection?lat="
        //url = "https://api.openuv.io/api/v1/protection?lat=40.38940677262632&lng=-3.629207340704153"
        //val url = "https://api.openuv.io/api/v1/uv?lat=-23.310755&lng=131.548631"
        //val url = "https://www.google.es"

        /*
        val json = "{\n" +
                "    \"result\": {\n" +
                "        \"from_time\": \"2021-06-01T08:38:02.822Z\",\n" +
                "        \"from_uv\": 3.6753,\n" +
                "        \"to_time\": \"2021-06-01T15:58:02.822Z\",\n" +
                "        \"to_uv\": 3.3156\n" +
                "    }\n" +
                "}" // your json value here
        val topic = Gson().fromJson(json, Json4Kotlin_Base::class.java)

        Log.d("RESULTADO",topic.result.from_time)

        textView.text = topic.result.from_time + topic.result.from_uv + topic.result.to_time + topic.result.to_uv
        */
        /*
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                // Display the first 500 characters of the response string.
                textView.text = "Response is: ${response.substring(0, 500)}"
            },
            { textView.text = "That didn't work!" })
         */

        val stringRequest = object: StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                //textView.text = "Response is: ${response.substring(0, 500)}"
                //Log.d("A", "Response is: " + response.substring(0,500))
                val topic = Gson().fromJson(response, Json4Kotlin_Base::class.java)

                textView.text = topic.result.from_time +"\n" + topic.result.from_uv +"\n" + topic.result.to_time +"\n" + topic.result.to_uv
            },
            Response.ErrorListener {  })
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["x-access-token"] = "17ee934bcd028a2a82bd151018cd951e"
                return headers
            }
        }

        // Add the request to the RequestQueue.
        queue.add(stringRequest)

    }

    fun setURL(location : Location){
        url = url.plus(location.latitude.toString()).plus("&lng=").plus(location.longitude)
    }

}