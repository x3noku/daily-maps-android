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
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
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
import com.shashank.sony.fancytoastlib.FancyToast
import com.x3noku.dailymaps.classes.TimeLackException
import com.x3noku.dailymaps.utils.*
import com.google.android.gms.maps.model.LatLng as MapsLatLng
import com.google.maps.model.LatLng as DirectionsLatLng

class RouteActivity : AppCompatActivity() {

    companion object {
        const val TAG = "RouteActivity"

    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var user: FirebaseUser
    private lateinit var templateId: String
    private lateinit var googleMap: GoogleMap

    private var mLocationPermissionGranted = false
    private lateinit var mGeoApiContext: GeoApiContext

    private val changeList = mutableListOf<Pair<MarkedTask?, String?>>()
    private lateinit var departureTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        findViewById<Toolbar>(R.id.route_toolbar).setNavigationOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.route_toolbar_action_image_button).setOnClickListener {
            showChangeListDialog()
        }

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

    private fun buildRoute(userLocation: DirectionsLatLng) {
        val taskList: MutableList<Task> = mutableListOf()
        var snapshotsReceived = 0

        getTemplate(templateId)
            .addOnSuccessListener { templateSnapshot ->
                val template = Template(templateSnapshot)
                for( taskId in template.taskIds ) {
                    getTask(taskId)
                        .addOnSuccessListener { taskSnapshot ->
                            val task = Task(taskSnapshot)
                            task.coords?.let { _ ->
                                taskList.add(task)
                            }

                            if(++snapshotsReceived == template.taskIds.size)
                                calculateRoute( userLocation, taskList.sortedBy { it.startTime } )
                        }
                }
            }
    }

    private fun calculateRoute(userLocation: DirectionsLatLng, taskList: List<Task>) {
         val markedTaskList: MutableList<MarkedTask?> = MutableList(taskList.size) {null}

        for( (index, task) in taskList.withIndex() ) {
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
                        result?.let { dResult ->
                            markedTaskList[index]  = MarkedTask(task, dResult.countRouteTime() )
                            drawPolylines(dResult)

                            if( !markedTaskList.contains(null) ) {
                                optimizeRoute(markedTaskList.toList() )
                            }
                            else {
                                Log.w(TAG, "markedTaskList, $markedTaskList")
                            }
                        }
                    }

                    override fun onFailure(e: Throwable?) {
                        Log.e(TAG, "155: $e", e)
                    }
                })
        }
    }

    private fun showChangeListDialog() {
        var message = "Отправление в $departureTime."

        for( changesPair in changeList ) {
            if( changesPair.first != null && changesPair.second != null ) {
                message += "\n\nЗадание \"${changesPair.first!!.text}\" было " +
                            "перенесено с ${changesPair.first!!.startTime.toDigitalView()} " +
                        "на ${changesPair.second}."
            }
        }

        AlertDialog
            .Builder(this)
            .setTitle("Информация о маршруте")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Ок", ({   _, _ ->  }))
            .create()
            .show()

    }

    private fun optimizeRoute(markedTaskList: List<MarkedTask?>) {
        val markedTaskFragmentList = markedTaskList.splitToFragments()

        doAsync(
            handler = {
                try {
                    markedTaskFragmentList.forEach { if(it.isNotEmpty()) it.optimizeFragment() }
                }
                catch (e: TimeLackException) {
                    /*
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setMessage("Для нормального функцианирования приложению нужен доступ к GPS. Хотите включить его?")
                        .setCancelable(false)
                        .setPositiveButton("Да", ({   _, _ -> finish()   }))
                    val alert: AlertDialog = builder.create()
                    alert.show()
                     */
                    Log.e(TAG, "${e.message}")
                }
            },
            postAction = {
                doAsync(
                    handler = {
                        for(markedTaskFragment in markedTaskFragmentList) {
                            markedTaskFragment.taskList.forEach { it.drawPoint(googleMap) }
                            markedTaskFragment.limiterLeft?.drawPoint(googleMap)
                            markedTaskFragment.limiterRight?.drawPoint(googleMap)
                        }
                    },
                    postAction = {
                        val firstTask =
                            if( markedTaskFragmentList.first().isNotEmpty() )
                                markedTaskFragmentList.first().taskList.first()
                            else
                                markedTaskFragmentList.first().limiterRight!!

                        departureTime =
                            (firstTask.startTime - firstTask.routeTime).toDigitalView()

                        var i = 0
                        markedTaskFragmentList.forEach { fragmentTaskList ->

                            fragmentTaskList.limiterLeft?.let {
                                changeList.add(Pair(null, null))
                                i++
                            }
                            fragmentTaskList.taskList.forEach { markedTask ->
                                if( markedTask.startTime != markedTaskList[i]?.startTime ) {
                                    val changes =
                                        Pair(
                                            markedTaskList[i],
                                            markedTask.startTime.toDigitalView()
                                        )
                                    changeList.add(changes)
                                }
                                else {
                                    changeList.add(Pair(null, null))
                                }
                                i++
                            }
                        }

                        FancyToast
                            .makeText(this,
                                "Маршрут успешно оптимизирован! Выйдете в $departureTime, " +
                                        "чтобы успеть выполнить все задания!",
                                FancyToast.LENGTH_LONG,
                                FancyToast.SUCCESS,
                                false
                            ).show()

                        findViewById<ImageButton>(R.id.route_toolbar_action_image_button)
                            .visibility = View.VISIBLE

                        if( changeList.count{ it.first != null && it.second != null } != markedTaskList.size)
                            showChangeListDialog()
                    }
                )
            }
        )

    }

    private fun DirectionsResult.countRouteTime(): Int {
        val route = this.routes[0]
        var routeTime = 0

        for(leg in route.legs) {
            routeTime += (leg.duration.inSeconds / 60).toInt()
        }

        return routeTime
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
                            .zoom(ZOOM_LEVEL)
                            .build()
                        val camUpdate = CameraUpdateFactory.newCameraPosition(camPos)
                        googleMap.moveCamera(camUpdate)

                        buildRoute( userLocation.toDirectionsLatLng() )
                    } ?: run {
                        FancyToast.makeText(
                            this,
                            "Не удалось определить местоположение!",
                            FancyToast.LENGTH_LONG,
                            FancyToast.ERROR,
                            false
                        ).show()
                    }
                }
                .addOnFailureListener {
                    buildAlertMessageNoGps()
                }
        }
    }

    private fun checkMapServices(): Boolean {
        if(isServicesOK()) {
            if(isMapsEnabled()) {
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

}


