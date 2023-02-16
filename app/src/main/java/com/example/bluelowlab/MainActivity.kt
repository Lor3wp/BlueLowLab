package com.example.bluelowlab

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelowlab.ui.theme.BlueLowLabTheme
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// BT stuff
var bluetoothAdapter: BluetoothAdapter? = null

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.getAdapter()

        setContent {
            BlueLowLabTheme {

                val result = remember { mutableStateOf<Int?>(100) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    result.value = it.resultCode
                }

                LaunchedEffect(key1 = true){

                    Dexter.withContext(this@MainActivity)
                        .withPermissions(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                        )
                        .withListener(object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                launcher.launch(intent)
                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permissions: List<PermissionRequest?>?,
                                token: PermissionToken?
                            ) {

                            }
                        })
                        .check()
                }

                HomeScreen(bluetoothAdapter = bluetoothAdapter!!)
            }
        }
    }
}

fun buildAdvertiseData(): AdvertiseData {
    val dataBuilder = AdvertiseData.Builder()
        .setIncludeDeviceName(true)

    return dataBuilder.build()
}


fun buildAdvertiseSettings(): AdvertiseSettings {
    return AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTimeout(0)
        .build()
}

//        @SuppressLint("MissingPermission")
//        fun startAdvertisement() {
//            advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
//
//            if (advertiseCallback == null) {
//                advertiseCallback = DeviceAdvertiseCallback()
//
//                advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
//            }
//        }

        class DeviceAdvertiseCallback : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                val errorMessage = "Advertise failed with error: $errorCode"
                Log.d(TAG, "failed $errorMessage")
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                Log.d(TAG, "successfully started")
            }
        }

// Stops scanning after X milliseconds.
const val SCAN_PERIOD: Long = 500

class BluetoothViewModel() : ViewModel() {
    private val mResults = java.util.HashMap<String, ScanResult>()
    val scanResults = MutableLiveData<List<ScanResult>>(null)
    val fScanning = MutableLiveData<Boolean>(false)

    @SuppressLint("MissingPermission")
    fun scanDevices(scanner: BluetoothLeScanner) {
        viewModelScope.launch(Dispatchers.IO) {
            fScanning.postValue(true)
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            scanner.startScan(null, settings, leScanCallback)
            delay(SCAN_PERIOD)
            scanner.stopScan(leScanCallback)
            scanResults.postValue(mResults.values.toList())
            fScanning.postValue(false)
        }
    }
    private val leScanCallback: ScanCallback = object : ScanCallback() {override fun onScanResult(callbackType: Int, result: ScanResult) {super.onScanResult(callbackType, result)
        val device = result.device
        val deviceAddress = device.address
        mResults!![deviceAddress] = result
        Log.d("DBG", "Device address: $deviceAddress (${result.isConnectable})")}
    }
}


/**
 *
 * Screens
 *
 * */
@Composable
fun HomeScreen(bluetoothAdapter: BluetoothAdapter) {
    Surface(modifier = Modifier.fillMaxSize()) {
        ShowDevices(bluetoothAdapter = bluetoothAdapter, model = BluetoothViewModel())
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDevices(bluetoothAdapter: BluetoothAdapter, model: BluetoothViewModel) {

    var advertiser: BluetoothLeAdvertiser? = null
    var advertiseCallback: AdvertiseCallback? = null
    var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    var advertiseData: AdvertiseData = buildAdvertiseData()
    advertiser = bluetoothAdapter?.bluetoothLeAdvertiser

    if (advertiseCallback == null) {
        advertiseCallback = DeviceAdvertiseCallback()

        advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    val context = LocalContext.current
    val value: List<ScanResult>? by model.scanResults.observeAsState()
    val valueSet = value?.toSortedSet( compareBy{it.device.address} )?.sortedByDescending { it.rssi }?.filter { it.device.name != null && it.device.name.contains("BT_TESTING") }
    val fScanning: Boolean by model.fScanning.observeAsState(false)
    val foundDevicesAmount: Int = valueSet?.size ?: 0
    val locaLBT by remember { mutableStateOf("${bluetoothAdapter.name}") }
    bluetoothAdapter.setName("BT_TESTING")

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 4.dp),
                onClick = {
                    model.scanDevices(bluetoothAdapter!!.bluetoothLeScanner)
                    println("burana ${value.toString()}")
                }) {
                Text(text = "Hello Börje!")
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "LocalName: ${locaLBT}")
            Text(text = "Elements: ${foundDevicesAmount}")
            Column(modifier = Modifier
                .verticalScroll(state = ScrollState(1), enabled = true)) {
                valueSet?.forEach {
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, end = 32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "MAC: ${it.device.address}",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Text(
                                text = "NAME: ${it.device.name}",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Text(
                                text = "RSSI: ${it.rssi} dBm",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                    )
                }
            }
        }

    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun DefaultPreview() {
    BlueLowLabTheme {
        HomeScreen(bluetoothAdapter = bluetoothAdapter!!)
    }
}