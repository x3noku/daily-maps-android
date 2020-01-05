package com.x3noku.dailymaps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView


class AddTask(private val previousSelectedItemId: Int) : DialogFragment() {

    val TAG = "AddTask"
    private val MY_PERMISSIONS_REQUEST_LOCATION = 89

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)

        val dialogFragment = dialog
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialogFragment?.window?.setLayout(width, height)

        checkLocationPermission()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_add_task, container, false)

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            googleMap.isMyLocationEnabled = true
            try {
                val locationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                location?.let {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    val camPos = CameraPosition.Builder()
                        .target(userLocation)
                        .zoom(12.2f)
                        .build()
                    val camUpdate = CameraUpdateFactory.newCameraPosition(camPos)
                    googleMap.moveCamera(camUpdate)
                }

            }
            catch (e: SecurityException) {
                Log.w(TAG, "onMapReady: UserInfo Rejected Location Request");
            }

            var marker: Marker? = null
            googleMap.setOnMapClickListener { latLng ->
                marker?.remove()
                marker = googleMap.addMarker(MarkerOptions().position(latLng))
            }
        }

        return rootView
    }

    override fun onDetach() {
        super.onDetach()

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        val fragmentTransaction: FragmentTransaction = activity!!.supportFragmentManager.beginTransaction()
        fragmentTransaction.remove(mapFragment)
        fragmentTransaction.commit()

        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.selectedItemId = previousSelectedItemId
    }

    private fun checkLocationPermission() {
        while(context
            != null
            &&
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_LOCATION)
        }
    }

}