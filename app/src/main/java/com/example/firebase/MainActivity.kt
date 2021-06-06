package com.example.firebase

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val TAG = "MainActivity"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var foregroundOnlyLocationButton: ImageButton

    //private lateinit var outputTextView: TextView
    private var url : String = ""
    private lateinit var queue: RequestQueue
    private lateinit var queryButton: ImageButton
    private lateinit var queryTextView: TextView
    private lateinit var lastUpdateTextView: TextView
    private lateinit var mapsButton : Button
    private lateinit var location : Location
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer : Accelerometer

    private lateinit var d :Date
    private lateinit var sm : SimpleDateFormat
    private lateinit var strDate : String

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        /*HTTP query variables declaration*/
        url = "https://api.openuv.io/api/v1/protection?lat="

        queue= Volley.newRequestQueue(this)
        queryButton = findViewById(R.id.launch_query_button)
        queryTextView = findViewById(R.id.query_text_view)
        lastUpdateTextView = findViewById(R.id.last_update_TV)
        mapsButton = findViewById(R.id.maps_button)

        queryButton.setOnClickListener{
            executeQuery()
        }

        mapsButton.setOnClickListener{
            activityMaps()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = Accelerometer(sensorManager)

        d = Date()
        sm = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

        // Reset button listener
        /*
        butSaveCurrent = findViewById<View>(R.id.buttonSaveCurrent) as Button
        butSaveCurrent.setOnClickListener(View.OnClickListener {
            val database: FirebaseDatabase = FirebaseDatabase.getInstance()
            val myUsersRef = database.reference
            val d = Date()
            val sm = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            val strDate = sm.format(d)
            myUsersRef.child("botonazos").push().setValue(strDate)
        })
        */

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        foregroundOnlyLocationButton = findViewById(R.id.foreground_only_location_button)
        foregroundOnlyLocationButton.setImageResource(R.drawable.outline_location_off_24)
        //foregroundOnlyLocationButton.setBackgroundColor(Color.RED)

        //outputTextView = findViewById(R.id.output_text_view)

        foregroundOnlyLocationButton.setOnClickListener {
            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)

            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
                if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
                } else {
                    requestForegroundPermissions()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
            )
        }
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                findViewById(R.id.activity_main),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                else -> {
                    // Permission denied.
                    updateButtonState(false)

                    Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            //foregroundOnlyLocationButton.setBackgroundColor(Color.GREEN)
            foregroundOnlyLocationButton.setImageResource(R.drawable.outline_place_24)
            //foregroundOnlyLocationButton.text = getString(R.string.stop_location_updates_button_text)
        } else {
            //foregroundOnlyLocationButton.setBackgroundColor(Color.RED)
            foregroundOnlyLocationButton.setImageResource(R.drawable.outline_location_off_24)
            //foregroundOnlyLocationButton.text = getString(R.string.start_location_updates_button_text)
        }
    }

    /*
    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs = "$output\n${outputTextView.text}"
        outputTextView.text = outputWithPreviousLogs
    }
    */

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
             location = intent.getParcelableExtra(
                 ForegroundOnlyLocationService.EXTRA_LOCATION
             )!!

            if (::location.isInitialized) {
                //logResultsToScreen("Foreground location: ${location.toText()}")
                url = "https://api.openuv.io/api/v1/protection?lat="
                setURL(location)
                persistData(location)
            }
        }
    }

    fun setURL(location : Location){
        url = url.plus(location.latitude.toString()).plus("&lng=").plus(location.longitude.toString())
        Log.d("MIURL", url)
        //executeQuery()
    }

    private fun executeQuery(){
        val stringRequest = @SuppressLint("SetTextI18n")
        object: StringRequest(
            Method.GET, url,
            Response.Listener { response ->
                //textView.text = "Response is: ${response.substring(0, 500)}"
                Log.d("MYQUERY", "Query ejecutada")
                val topic = Gson().fromJson(response, Json4Kotlin_Base::class.java)
                d = Date()
                strDate = sm.format(d)
                lastUpdateTextView.text = "Last updated at $strDate"
                queryTextView.text =  topic.result.from_time +"\n" + topic.result.from_uv +"\n" + topic.result.to_time +"\n" + topic.result.to_uv
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

    private fun activityMaps(){
        val lat : Double
        val lon : Double
        val isInitialized: Boolean

        if (!::location.isInitialized) {

            lat = 40.3894234
            lon = -3.6278847
            isInitialized = false
            //Log.d("MYLOCATION", "lat "+location.latitude+" lon "+location.longitude)

        } else{
            lat = location.latitude
            lon = location.longitude
            isInitialized = true
        }

        val intent = Intent(this, MapsActivity::class.java).apply {
            Log.d("MYLOCATION", "lat $lat lon $lon")
            putExtra("lat", lat)
            putExtra("lon", lon)
            putExtra("init", isInitialized)
        }
        startActivity(intent)
    }

    private fun persistData(location: Location) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val myUsersRef = database.reference
        d = Date()
        strDate = sm.format(d)
        //myUsersRef.child("botonazos").push().setValue(strDate)
        myUsersRef.child("Location").push().setValue("$strDate - Latitude ${location.latitude}, longitude ${location.longitude}")
    }
}