package com.application.airquality

import android.content.Intent
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.application.airquality.databinding.ActivityMainBinding
import com.application.airquality.retrofit.AirQualityResponse
import com.application.airquality.retrofit.AirQualityService
import com.application.airquality.retrofit.RetrofitConnection
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.create
import java.io.IOException
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    // 런타임 권한 요청시 필요한 요청 코드
    private val PERMISSION_REQUEST_CODE = 100


    // 요청할 권한 리스트
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    // 위치 서비스 요청시 필요
    lateinit var getGPSPermissionLauncher: ActivityResultLauncher<Intent>

    // 위도 경도를 가져올 시 필요
    lateinit var locationProvider: LocationProvider


    //위도와 경도를 저장
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    val startMapActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult> {
                override fun onActivityResult(result: ActivityResult?) {
                    if (result?.resultCode ?: Activity.RESULT_CANCELED == Activity.RESULT_OK) {
                        latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                        longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                        updateUI()
                    }

                }

            })
    // 전면 광고
    var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
        setRefreshButton()
        setFab()
        setBannerAds()
    }

    private fun setInterstitialAds() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            "ca-app-pub-8208941848665388/7576679447",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("ads log", "전면 광고가 로드 실패했습니다. ${adError.responseInfo}")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(intersititialAd: InterstitialAd) {
                    Log.d("ads log", "전면 광고가 로드되었습니다.")
                    mInterstitialAd = intersititialAd
                }
            })
    }

    private fun setBannerAds() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        binding.adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                Log.d("ads log", "배너 광고를 클릭했습니다.")
            }

            override fun onAdClosed() {
                Log.d("ads log", "배너 광고를 닫았습니다.")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("ads log", "배너 광고가 로드 실패했습니다. ${adError.responseInfo}")
            }


            override fun onAdLoaded() {
                Log.d("ads log", "배너 광고가 로드되었습니다.")
            }

            override fun onAdOpened() {
                Log.d("ads log", "배너 광고를 열었습니다.")
            }
        }

    }


    override fun onResume() {
        super.onResume()
        setInterstitialAds()
    }


    private fun setFab() {
        binding.fab.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("ads log", "전면 광고가 닫혔습니다.")

                        val intent = Intent(this@MainActivity, MapActivity::class.java)
                        intent.putExtra("currentLat", latitude)
                        intent.putExtra("currentLng", longitude)
                        startMapActivityResult.launch(intent)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("ads log", "전면 광고가 열리는 데 실패 했습니다.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("ads log", "전면 광고가 성공적으로 열렸습니다.")
                        mInterstitialAd = null
                    }
                }
                mInterstitialAd!!.show(this)

            } else {
                Log.d("InterstitialAd", "전면 광고가 로딩되지 않았습니다.")
                Toast.makeText(this@MainActivity, "잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG)
                    .show()
            }


        }
    }

    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }

    private fun updateUI() {
        locationProvider = LocationProvider(this)

        if (latitude == 0.0 || longitude == 0.0) {
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }

        if (latitude != 0.0 || longitude != 0.0) {

            val address = getCurrentAddress(latitude, longitude)
            address?.let {
                binding.tvLocationTitle.text = "${it.thoroughfare}"
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }
            getAirQualityData(latitude, longitude)

        } else {

            Toast.makeText(this, "위도, 경도 정보를 가져올 수 없었습니다. 새로고침을 눌러주세요.", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun getAirQualityData(latitude: Double, longitude: Double) {
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)
        retrofitAPI.getAirQualityData(
            latitude.toString(),
            longitude.toString(),
            "022c0b61-85e6-447a-9b63-eca17429bfe0"
        )
            .enqueue(object : Callback<AirQualityResponse> {
                override fun onResponse(
                    call: Call<AirQualityResponse>,
                    response: Response<AirQualityResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "최신 정보 업데이트 완료!", Toast.LENGTH_SHORT)
                            .show()
                        response.body()?.let { updateAirUI(it) }
                    } else {
                        Toast.makeText(this@MainActivity, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT)
                            .show()

                    }
                }

                override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                    t.printStackTrace()
                    Toast.makeText(this@MainActivity, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }


            })


    }

    private fun updateAirUI(airQualityData: AirQualityResponse) {
        val pollutionData = airQualityData.data.current.pollution

        binding.tvCount.text = pollutionData.aqius.toString()

        val dateTime =
            ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(
                ZoneId.of("Asia/Seoul")
            )
                .toLocalDateTime()
        val dataFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        binding.tvCheckTime.text = dateTime.format(dataFormatter).toString()

        when (pollutionData.aqius) {
            in 0..50 -> {
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)

            }
            in 51..150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)

            }
            in 151..200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)

            }
            else -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }


        }

    }

    private fun checkAllPermissions() {
        if (!isLocationServicesAvailable()) {
            showDialogForLocationServiceSetting();
        } else {
            isRunTimePermissionsGranted();
        }
    }


    fun isLocationServicesAvailable(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))


    }

    private fun isRunTimePermissionsGranted() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {

            var checkResult = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break

                }
            }

            if (checkResult) {

                updateUI()

            } else {
                Toast.makeText(
                    this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해 주세요.", Toast.LENGTH_LONG
                ).show()
                finish()
            }


        }
    }

    private fun showDialogForLocationServiceSetting() {
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (isLocationServicesAvailable()) {
                    isRunTimePermissionsGranted()
                } else {
                    Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(
            this@MainActivity
        )
        builder.setTitle("위치 서비스 활성화")
        builder.setMessage("위치 서비스가 꺼져 있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
            val callGPSSettingIntent = Intent(
                Settings.ACTION_LOCATION_SOURCE_SETTINGS
            )
            getGPSPermissionLauncher.launch(callGPSSettingIntent)

        })

        builder.setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()
                Toast.makeText(
                    this@MainActivity, "기기에서 위치서비스(GPS) 설정 후 사용해주세요",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            })
        builder.create().show()


    }

    fun getCurrentAddress(latitude: Double, longitude: Double): Address? {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>?

        addresses = try {
            geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException: IOException) {

            Toast.makeText(this, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        if (addresses == null || addresses.size == 0) {

            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null

        }
        val address: Address = addresses[0]
        return address

    }


}