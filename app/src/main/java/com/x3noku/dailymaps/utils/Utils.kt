package com.x3noku.dailymaps.utils

import com.google.android.gms.maps.model.LatLng as MapsLatLng
import com.google.maps.model.LatLng as DirectionsLatLng

fun MapsLatLng.toDirectionsLatLng(): DirectionsLatLng =
    DirectionsLatLng(this.latitude, this.longitude)

fun DirectionsLatLng.toMapsLatLng(): MapsLatLng =
    MapsLatLng(this.lat, this.lng)