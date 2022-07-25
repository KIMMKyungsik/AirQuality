package com.application.airquality

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.application.airquality.databinding.ActivityMainBinding
import com.application.airquality.databinding.ActivityMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding: ActivityMapBinding

    private var mMap: GoogleMap? = null
    var currentLat: Double = 0.0
    var currentLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLat = intent.getDoubleExtra("currentLat", 0.0)
        currentLng = intent.getDoubleExtra("currentLng", 0.0)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.btnCheckHere.setOnClickListener {
            mMap?.let {
                val intent = Intent()
                intent.putExtra("latitude",it.cameraPosition.target.latitude)
                intent.putExtra("longitude",it.cameraPosition.target.longitude)
                setResult(Activity.RESULT_OK,intent)
                finish()

            }
        }
    }
    // 지도가 실행되었을 때 실행되는 함수
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.let {
            val currentLocation = LatLng(currentLat ,currentLng)
            it.setMaxZoomPreference(20.0f)
            it.setMinZoomPreference(12.0f)
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,16f))
        }


        setMarker()
        binding.fabCurrentLocation.setOnClickListener {
            val locationProvider = LocationProvider(this@MapActivity)
            val latitude = locationProvider.getLocationLatitude()
            val longitude = locationProvider.getLocationLongitude()
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude,longitude),16f))
            setMarker()
        }
    }
    //마커를 설정하는 함수
    private fun setMarker() {
        mMap?.let {
            it.clear() // 지도에 있는 마커를 먼저 삭제
            val markerOptions = MarkerOptions()
            markerOptions.position(it.cameraPosition.target) // 마커의 위치 설정
            markerOptions.title("마커 위치") // 마커의 이름 설정
            val marker = it.addMarker(markerOptions) // 지도에 마커를 추가하고, 마커 객체를 리턴한다

            it.setOnCameraMoveListener {
                marker?.let { marker ->
                    marker.position = it.cameraPosition.target

                }
            }
        }

    }


}