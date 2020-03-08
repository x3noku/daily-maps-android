package com.x3noku.dailymaps

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import com.x3noku.dailymaps.utils.*
import com.google.android.gms.maps.model.LatLng as MapsLatLng
import com.google.maps.model.LatLng as DirectionsLatLng

// ToDo: RENAME FUNCTIONS AND REORGANIZE IT HIERARCHY

class RouteActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val TAG = "RouteActivity"
        
        private lateinit var firestore: FirebaseFirestore
        private lateinit var user: FirebaseUser
        private lateinit var templateId: String
        private lateinit var googleMap: GoogleMap

        private var mLocationPermissionGranted = false
        private lateinit var mGeoApiContext: GeoApiContext
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        firestore = FirebaseFirestore.getInstance()
        FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->
            user = firebaseUser
        } ?: run {
            startActivity( Intent(this, LoginActivity::class.java) )
        }
        intent.getStringExtra("templateId")?.let { templateIdExtra ->
            templateId = templateIdExtra
            initMap()
        } ?: run {
            startActivity( Intent(this, MainActivity::class.java) )
        }

    }

    private fun drawPolylines(result: DirectionsResult) {
        Handler(Looper.getMainLooper()).post {
            for (route: DirectionsRoute in result.routes) {
                val decodedPath: List<DirectionsLatLng> =
                    PolylineEncoding.decode(route.overviewPolyline.encodedPath)
                val newDecodedPath: MutableList<MapsLatLng> = mutableListOf()
                decodedPath.forEach {
                    newDecodedPath.add(it.toMapsLatLng())
                }

                googleMap
                    .addPolyline(
                        PolylineOptions().addAll(newDecodedPath)
                    )
                    .color = ContextCompat.getColor(this, R.color.BLUE)
            }
        }
    }

    private fun Task.drawPoint() {
        Handler(Looper.getMainLooper()).post {
            googleMap.addMarker(
                MarkerOptions()
                    .position(this.coords!!)
                    .title(this.text)
            )
        }
    }

    private fun buildRoute(userLocation: DirectionsLatLng) {
        Log.d(TAG, "buildRoute: function was called")

        fun calculateRoutes(taskList: List<Task>?) {
            Log.d(TAG, "calculateTask: function was called")
            Log.d(TAG, "TaskList: $taskList")

            taskList?.let { _ ->
                Log.e(TAG, "${taskList.split()}")

                for( (index, task) in taskList.withIndex() ) {
                    Log.e(TAG, "${task.text} building a route")
                    DirectionsApiRequest(mGeoApiContext)
                        .alternatives(false)
                        .mode(TravelMode.TRANSIT)
                        .origin(
                            if(index>0)
                                taskList[index-1].coords?.toDirectionsLatLng()
                            else
                                userLocation
                        )
                        .destination( task.coords?.toDirectionsLatLng() )
                        .setCallback(object : PendingResult.Callback<DirectionsResult> {
                            override fun onResult(result: DirectionsResult?) {
                                result?.let {
                                    drawPolylines(result)
                                }
                            }
                            override fun onFailure(e: Throwable?) {
                                Log.e(TAG, "calculateTask: onFailure: ${e.toString()}")
                            }
                        })
                    task.drawPoint()
                }
            }

        }

        fun getTaskList(attempt: Int = 0) {
            Log.d(TAG, "getTaskList: function was called")
            if(attempt < 3) {
                val taskList: MutableList<Task> = mutableListOf()

                getTemplate(templateId)
                    .addOnSuccessListener { templateSnapshot ->
                        val template = Template(templateSnapshot)

                        for( (index, taskId) in template.taskIds.withIndex() ) {
                            getTask(taskId)
                                .addOnSuccessListener { taskSnapshot ->
                                    val task = Task(taskSnapshot)
                                    task.coords?.let { _ ->
                                        taskList.add(task)

                                        Log.e(TAG, "\"${task.text}\" cooridnates " +
                                                "is ${task.coords}" +
                                                ", so let's add it: ${taskList} \n")


                                    }
                                }
                                .addOnCompleteListener {
                                    if(index == template.taskIds.size-1) {
                                        calculateRoutes( taskList.sortedBy { it.startTime } )
                                    }
                                }
                        }
                    }
                    .addOnFailureListener {
                        getTaskList(attempt+1)
                    }
            }
            else {
                Toast
                    .makeText(
                        this,
                        "Не удалось получить шаблон, попробуйте вернуться попозже.",
                        Toast.LENGTH_LONG
                    )
                    .show()
            }
        }

        getTaskList()
    }

    private fun getTemplate(templateId: String) =
        firestore
            .collection(getString(R.string.firestore_templates_collection))
            .document(templateId)
            .get()

    private fun getTask(taskId: String) =
        firestore
            .collection(getString(R.string.firestore_tasks_collection))
            .document(taskId)
            .get()

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { gMap ->
            googleMap = gMap
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            checkForUserLocation()
        }
        mGeoApiContext = GeoApiContext
            .Builder()
            .apiKey(getString(R.string.GOOGLE_API_KEY))
            .build()
    }

    private fun checkForUserLocation() {
        if( checkMapServices() ) {
            googleMap.isMyLocationEnabled = true
            LocationServices
                .getFusedLocationProviderClient(this)
                .lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let { nonNullLocation ->
                        val userLocation =
                            MapsLatLng(nonNullLocation.latitude, nonNullLocation.longitude)
                        val camPos = CameraPosition.Builder()
                            .target(userLocation)
                            .zoom(12.2f)
                            .build()
                        val camUpdate = CameraUpdateFactory.newCameraPosition(camPos)
                        googleMap.moveCamera(camUpdate)

                        buildRoute( userLocation.toDirectionsLatLng() )
                    }
                }
                .addOnFailureListener {
                    buildAlertMessageNoGps()
                }
            Toast.makeText(this, "CHECK FOR USER LOCATION", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true
            }
        }
        return false
    }

    private fun isServicesOK(): Boolean {
        Log.d(TAG, "isServicesOK: checking google services version")
        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        when {
            available == ConnectionResult.SUCCESS -> {
                Log.d(TAG, "isServicesOK: Google Play Services is working")
                return true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                Log.d(TAG, "isServicesOK: an error occured but we can fix it")
                val dialog: Dialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, available, ERROR_DIALOG_REQUEST)
                dialog.show()
            }
            else ->
                Toast
                    .makeText(this, "You can't make map requests", Toast.LENGTH_SHORT)
                    .show()
        }
        return false
    }

    private fun isMapsEnabled(): Boolean {
        val manager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Для нормального функцианирования приложению нужен доступ к GPS. Хотите включить его?")
            .setCancelable(false)
            .setPositiveButton("Да", ({ _, _ ->
                val enableGpsIntent =
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
            }))
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called.")
        when (requestCode) {
            PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (mLocationPermissionGranted) {
                    checkForUserLocation()
                } else {
                    getLocationPermission()
                }
            }
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
            checkForUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    mLocationPermissionGranted = true
                    checkForUserLocation()
                }
            }
        }
    }

    override fun onMapReady(p0: GoogleMap?) {
        val success =
            googleMap.setMapStyle(
                MapStyleOptions(getString(R.string.map_style_json))
            )
    }

}


