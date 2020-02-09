package com.x3noku.dailymaps

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import xyz.aprildown.hmspickerview.HmsPickerView

class AddTaskDialogFragment() : DialogFragment() {

    private val TAG = "AddTask"
    private val My_Permissions_Request_Location = 89

    private var previousSelectedItemId: Int? = null
    private var idOfEditableFile: String? = null

    constructor(previousSelectedItemId: Int) : this() {
        this.previousSelectedItemId = previousSelectedItemId
    }

    constructor(idOfEditableFile: String) : this() {
        this.idOfEditableFile = idOfEditableFile
    }

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
        initLayout(rootView)
        return rootView
    }

    override fun onDetach() {
        super.onDetach()

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        val fragmentTransaction: FragmentTransaction = activity!!.supportFragmentManager.beginTransaction()
        fragmentTransaction.remove(mapFragment)
        fragmentTransaction.commit()

        previousSelectedItemId?.let {
            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigationView?.selectedItemId = it
        }
    }

    private fun initLayout(rootView: View) {
        val firestore = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        var task = Task()

        val toolbar = rootView.findViewById<Toolbar>(R.id.add_task_toolbar)
        val startTimeInputTextView = rootView.findViewById<TextView>(R.id.add_task_start_time_input)
        val durationInputTextView = rootView.findViewById<TextView>(R.id.add_task_duration_input)
        val priorityInputTextView = rootView.findViewById<TextView>(R.id.add_task_priority_input)
        val textInputEditText = rootView.findViewById<EditText>(R.id.add_task_text_input)
        val saveTaskImageButton = rootView.findViewById<ImageButton>(R.id.add_task_toolbar_action_image_button)
        lateinit var googleMap: GoogleMap
        var marker: Marker? = null

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { gMap ->
            googleMap = gMap
            googleMap.isMyLocationEnabled = true
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
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
            }
            catch (e: SecurityException) {
                Log.w(TAG, "onMapReady: UserInfo Rejected Location Request");
            }

            googleMap.setOnMapClickListener { latLng ->
                marker?.remove()
                marker = googleMap.addMarker(MarkerOptions().position(latLng))
                task.coords = latLng
            }
        }

        idOfEditableFile?.let { idOfEditableFile ->
            val editableFileDocumentReference = firestore.collection(resources.getString(R.string.firestore_tasks_collection)).document(idOfEditableFile)
            editableFileDocumentReference.get().addOnSuccessListener { documentSnapshot ->
                task = Task(documentSnapshot)
                textInputEditText.setText(task.text)
                startTimeInputTextView.text = task.startTime.toDigitalView()
                run {
                        val minutes = task.duration.toMinutes()
                        val hours = task.duration.toHours()

                        val hoursString = when(hours) {
                            0 -> ""
                            in 11..19 -> "$hours часов "
                            else -> when (hours%10) {
                                1 -> "$hours час "
                                in 2..4 -> "$hours часа "
                                in 5..9 -> "$hours часов "
                                else -> "$hours часов "
                            }
                        }
                        val minutesString = when(minutes) {
                            0 -> if( hoursString.isNotBlank() ) "" else "$minutes минут"
                            in 11..19 -> "$minutes минут"
                            else -> when (minutes%10) {
                                1 -> "$minutes минута"
                                in 2..4 -> "$minutes минуты"
                                in 5..9 -> "$minutes минут"
                                else -> "$minutes минут"
                            }
                        }

                        durationInputTextView.text =  "$hoursString$minutesString"
                    }
                run {
                        when(task.priority) {
                            0 -> {
                                priorityInputTextView.text = resources.getString(R.string.add_task_max_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMaxPriority) )
                            }
                            1 -> {
                                priorityInputTextView.text = resources.getString(R.string.add_task_high_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskHighPriority) )
                            }
                            2 -> {
                                priorityInputTextView.text = resources.getString(R.string.add_task_mid_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMidPriority) )
                            }
                            3 -> {
                                priorityInputTextView.text = resources.getString(R.string.add_task_low_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskLowPriority) )
                            }
                            else -> {
                                priorityInputTextView.text = resources.getString(R.string.add_task_min_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMinPriority) )
                            }
                        }
                    }
                task.coords?.let { latLng ->
                        val camPos = CameraPosition.Builder()
                            .target(latLng)
                            .zoom(12.2f)
                            .build()
                        val camUpdate = CameraUpdateFactory.newCameraPosition(camPos)
                        googleMap.moveCamera(camUpdate)
                        marker?.remove()
                        marker = googleMap.addMarker(MarkerOptions().position(latLng))

                    }
            }
        }

        startTimeInputTextView.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                activity,
                {_, hours, minutes ->
                    task.startTime = hours*60 + minutes
                    startTimeInputTextView.text = task.startTime.toDigitalView()
                },
                task.startTime.toHours(),
                task.startTime.toMinutes(),
                true
            )
            timePickerDialog.show()
            timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
            timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
        }

        durationInputTextView.setOnClickListener {
            val hmsPickerDialog = AlertDialog.Builder(context)
                .setView(R.layout.hms_picker_layout)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            run {
                val hmsPickerView = hmsPickerDialog.findViewById<HmsPickerView>(R.id.hms_picker_view)!!
                hmsPickerView.setHours( task.duration.toHours() )
                hmsPickerView.setMinutes( task.duration.toMinutes() )

                hmsPickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
                    hmsPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor( ContextCompat.getColor(context!!, R.color.colorAccent) )
                    hmsPickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor( ContextCompat.getColor(context!!, R.color.WHITE) )
                    hmsPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor( ContextCompat.getColor(context!!, R.color.WHITE) )

                    hmsPickerDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok)) { _, _ ->
                        val duration: Int = hmsPickerView.getHours()*60 + hmsPickerView.getMinutes()
                        task.duration = duration

                        val minutes = duration.toMinutes()
                        val hours = duration.toHours()

                        val hoursString = when(hours) {
                            0 -> ""
                            in 11..19 -> "$hours часов "
                            else -> when (hours%10) {
                                1 -> "$hours час "
                                in 2..4 -> "$hours часа "
                                in 5..9 -> "$hours часов "
                                else -> "$hours часов "
                            }
                        }
                        val minutesString = when(minutes) {
                            0 -> if( hoursString.isNotBlank() ) "" else "$minutes минут"
                            in 11..19 -> "$minutes минут"
                            else -> when (minutes%10) {
                                1 -> "$minutes минута"
                                in 2..4 -> "$minutes минуты"
                                in 5..9 -> "$minutes минут"
                                else -> "$minutes минут"
                            }
                        }
                        durationInputTextView.text =  "$hoursString$minutesString"
                    }
                }
            }

        priorityInputTextView.setOnClickListener {
                val popupMenu = popupMenu {
                    section {
                        item {
                            label = "Макс."
                            icon = R.drawable.ic_dot_black_24dp
                            iconColor = ContextCompat.getColor(context!!, R.color.addTaskMaxPriority)
                            callback = {
                                priorityInputTextView.text = resources.getString(R.string.add_task_max_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMaxPriority) )
                                task.priority = 0
                            }
                        }
                        item {
                            label = "Высокий"
                            icon = R.drawable.ic_dot_black_24dp
                            iconColor = ContextCompat.getColor(context!!, R.color.addTaskHighPriority)
                            callback = {
                                priorityInputTextView.text = resources.getString(R.string.add_task_high_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskHighPriority) )
                                task.priority = 1
                            }
                        }
                        item {
                            label = "Средний"
                            icon = R.drawable.ic_dot_black_24dp
                            iconColor = ContextCompat.getColor(context!!, R.color.addTaskMidPriority)
                            callback = {
                                priorityInputTextView.text = resources.getString(R.string.add_task_mid_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMidPriority) )
                                task.priority = 2
                            }
                        }
                        item {
                            label = "Низкий"
                            icon = R.drawable.ic_dot_black_24dp
                            iconColor = ContextCompat.getColor(context!!, R.color.addTaskLowPriority)
                            callback = {
                                priorityInputTextView.text = resources.getString(R.string.add_task_low_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskLowPriority) )
                                task.priority = 3
                            }
                        }
                        item {
                            label = "Мин."
                            icon = R.drawable.ic_dot_black_24dp
                            iconColor = ContextCompat.getColor(context!!, R.color.addTaskMinPriority)
                            callback = {
                                priorityInputTextView.text = resources.getString(R.string.add_task_min_priority)
                                priorityInputTextView.compoundDrawables[0].setTint( ContextCompat.getColor(context!!, R.color.addTaskMinPriority) )
                                task.priority = 4
                            }
                        }
                    }
                }
                popupMenu.show(context!!, it)
            }

        textInputEditText.addTextChangedListener {
                task.text = it.toString().replace("\\s+".toRegex(), " ")
            }

        saveTaskImageButton.setOnClickListener {
                if( task.text.isNotBlank() && currentUser != null ) {
                    idOfEditableFile?.let { idOfEditableFile ->
                        val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser.uid)
                        userDocumentReference.update(
                            "taskIds",
                            FieldValue.arrayRemove(idOfEditableFile)
                        )
                        firestore
                            .collection(resources.getString(R.string.firestore_tasks_collection))
                            .add(task)
                            .addOnSuccessListener { documentReference ->
                                userDocumentReference
                                    .update("taskIds", FieldValue.arrayUnion(documentReference.id))
                                    .addOnSuccessListener {
                                        dismiss()
                                    }
                            }
                    } ?: run {
                        val userDocumentReference = firestore.collection(getString(R.string.firestore_users_collection)).document(currentUser!!.uid)
                        firestore
                            .collection(resources.getString(R.string.firestore_tasks_collection))
                            .add(task)
                            .addOnSuccessListener { documentReference ->
                                userDocumentReference
                                    .update("taskIds", FieldValue.arrayUnion(documentReference.id) )
                                    .addOnSuccessListener {
                                        dismiss()
                                    }
                            }
                    }
                }
            }

    }

    private fun checkLocationPermission() {
        while(
            context != null &&
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), My_Permissions_Request_Location)
        }
    }

}