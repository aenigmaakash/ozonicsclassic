package com.intrax.ozonics

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_device_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayList


class DeviceList : AppCompatActivity() {

    private val mPairedDevices = arrayOfNulls<BluetoothDevice>(50)
    lateinit var myBluetooth: BluetoothAdapter
    private val deviceName = "Phoenix"
    private var firstTime: Boolean = true
    var mBTDevices: ArrayList<BluetoothDevice> = ArrayList()
    private var broadcastReceiverFlag = false




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        firstTime = intent.getBooleanExtra("first_time", true)
        firstTime = intent.getBooleanExtra("first_time", true)

        //ellipse2Layout.visibility = View.INVISIBLE
        deviceLayout.visibility = View.INVISIBLE
        scanLayout.visibility = View.INVISIBLE
        select.visibility = View.INVISIBLE
        ellipse1Layout.visibility = View.INVISIBLE
        ellipse2.visibility = View.INVISIBLE
        GlobalScope.launch(Dispatchers.Main){
            logoAnimation()
            delay(1000)
            scanLayout.visibility = View.VISIBLE
        }

        val myBluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        myBluetooth = myBluetoothManager.adapter
        checkBTPermissions()


        deviceList.setOnItemClickListener { parent, view, position, id ->
            val macId = view.findViewById<TextView>(R.id.deviceAddress).text.toString()
            //Toast.makeText(this, macId, Toast.LENGTH_SHORT).show()
            myBluetooth.cancelDiscovery()
            broadcastReceiverFlag = false
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("DeviceAddress", macId)
            startActivity(intent)
            finish()
        }

        scan.setOnClickListener {
            if(!checkGpsStatus()){
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setMessage("Location must be enabled, please turn it on")
                    .setCancelable(false)
                    .setPositiveButton("Enable", DialogInterface.OnClickListener { dialog, _ ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                        finish()
                    })
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                        dialog.cancel()
                    })
                val alert = dialogBuilder.create()
                alert.setTitle("Location Request")
                alert.show()
            }
            else{
                if (!myBluetooth.isEnabled) {
                    val turnBTon = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(turnBTon, 1)
                }
                else if (myBluetooth.isEnabled){
                    GlobalScope.launch(Dispatchers.Main) {
                        searchDevices()
                    }
                }
            }

        }


    }

    private fun logoAnimation(){
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f)
        val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -700f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(logoLayout, scaleX, scaleY, translateY)
        animator.duration = 1000
        animator.start()
    }

    private fun scanAnimation() {
        GlobalScope.launch(Dispatchers.Main) {
            scan.visibility = View.INVISIBLE
            select.visibility = View.VISIBLE
            delay(500)
            select.visibility = View.INVISIBLE
            scan.visibility = View.VISIBLE
            val ellipse1scaleXpos = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f)
            val ellipse1scaleYpos = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f)
            val ellipse2scaleXpos = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.16f)
            val ellipse2scaleYpos = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.16f)
            //val fade = PropertyValuesHolder.ofFloat(View.ALPHA, 1f)
            val translateY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -300f)

            val animateEllipse1 = ObjectAnimator
                .ofPropertyValuesHolder(ellipse1Layout, ellipse1scaleXpos, ellipse1scaleYpos)
            val animateScan = ObjectAnimator
                .ofPropertyValuesHolder(scanLayout, translateY)
            val animateEllipse2 = ObjectAnimator
                .ofPropertyValuesHolder(ellipse2, ellipse2scaleXpos, ellipse2scaleYpos)

            animateEllipse1.repeatMode = ObjectAnimator.REVERSE
            animateEllipse1.repeatCount = ObjectAnimator.INFINITE
            animateEllipse2.repeatMode = ObjectAnimator.REVERSE
            animateEllipse2.repeatCount = ObjectAnimator.INFINITE
            animateEllipse1.duration = 1000
            animateEllipse2.duration = 1000
            animateScan.duration = 2000

            ellipse1Layout.animation = AnimationUtils.loadAnimation(this@DeviceList, R.anim.fade_in)
            ellipse2.animation = AnimationUtils.loadAnimation(this@DeviceList, R.anim.fade_in)
            ellipse1Layout.visibility = View.VISIBLE
            ellipse2.visibility = View.VISIBLE

            animateEllipse1.start()
            animateScan.start()
            animateEllipse2.start()
        }
    }

    private fun searchDevices(){
        if(myBluetooth.isDiscovering)
            myBluetooth.cancelDiscovery()
        if(!myBluetooth.isDiscovering){
            checkBTPermissions()
            scanAnimation()
            myBluetooth.startDiscovery()
            GlobalScope.launch(Dispatchers.Main) {
                deviceLayout.animation = AnimationUtils.loadAnimation(this@DeviceList, R.anim.fade_in)
                deviceLayout.visibility = View.VISIBLE
            }
            val intent = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(bluetoothBroadcastReceiver, intent)
            broadcastReceiverFlag = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==1){
            if(resultCode==Activity.RESULT_OK){
                searchDevices()
            }
            else{
                Toast.makeText(this, "Bluetooth is not enabled.\nPlease enable in settings!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(broadcastReceiverFlag)
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        if(myBluetooth.isEnabled && checkGpsStatus())
            searchDevices()
    }

    override fun onPause() {
        super.onPause()
        if(broadcastReceiverFlag)
        unregisterReceiver(bluetoothBroadcastReceiver)
    }

    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if(ContextCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION),
                    2)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            2 -> {
                if (grantResults.isEmpty()) {
                }
                else {
                    for(i in 0 until (grantResults.size)) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                            Toast.makeText(this,
                                "Please allow all the permissions",
                                Toast.LENGTH_LONG)
                                .show()
                    }
                }
            }
        }
    }

    private val bluetoothBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                //Log.d("DeviceName:", device.name + device.address)
                if (device.name!=null && device.name.contains(deviceName)){
                    if(!mBTDevices.contains(device)){
                        mBTDevices.add(device)
                        val deviceListAdapter = DeviceListAdapter(context, R.layout.device_list_layout, mBTDevices)
                        deviceList.adapter = deviceListAdapter

                    }
                }
            }
        }
    }

    private fun checkGpsStatus(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}
