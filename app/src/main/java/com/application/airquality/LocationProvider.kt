package com.application.airquality

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.lang.Exception

class LocationProvider(val context: Context) {


    // Loacation 클래스는 위도, 경도, 고도와 같은 위치정보를 가지고 있는 클래스
    private var location: Location? = null

    // LocationManager는 시스템 위치 서비스에 접근하는 서비스
    private var locationManager: LocationManager? = null

    init {
        getLocation() // 초기화시에 위치를 가져옴

    }

    private fun getLocation(): Location? {
        try {

            //위치 시스템 서비스를 가져옴
            locationManager = context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

            var gpsLocation: Location? = null
            var networkLocation: Location? = null


            // Gps Provider 와 Network Provider 가 활성화 되어 있는지 확인
            val isGPSEnabled: Boolean =
                locationManager!!.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )

            val isNetworkEnabled: Boolean =
                locationManager!!.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )

            //GPS 와 Network가 둘 다 불가능한 상황이면 null 을 반환
            if (!isGPSEnabled && !isNetworkEnabled) {
                return null


            } else {
                val hasFineLocationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) // ACCESS_COARSE_LOCATION 보다 더 정밀한 위치 정보를 얻을 수 있음

                val hasCoarseLocationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) // 도시 Block 단위의 정밀도의 위치 정보를 얻을 수 있음


                // 만약 두 개의 권한이 없다면 null 을 반환
                if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED
                )
                    return null


                // Network 를 통한 위치 파악이 가능한 경우 위치를 가져옴
                if (isNetworkEnabled) {
                    networkLocation = locationManager?.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER
                    )
                }
                //GPS 를 통한 위치 파악이 가능한 경우에 위치를 가져옴
                if (isGPSEnabled) {
                    gpsLocation = locationManager?.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER
                    )
                }


                // 두 개의 위치 중에 더 정확한 것을 가져옴
                if (gpsLocation != null && networkLocation != null) {

                    if (gpsLocation.accuracy > networkLocation.accuracy) {
                        location = gpsLocation
                        return gpsLocation
                    } else {
                        location = networkLocation
                        return networkLocation
                    }

                // 만약 가능한 위치 정보가 한개만 있는 경우
                } else {
                    if (gpsLocation != null) {
                        location = gpsLocation

                    }
                    if (networkLocation != null) {
                        location = networkLocation
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location

    }


    //위도 정보를 함수로 가져옴
    fun getLocationLatitude(): Double {

        return location?.latitude ?: 0.0
    }

    //경도 정보를 함수로 가져옴
    fun getLocationLongitude(): Double {

        return location?.longitude ?: 0.0
    }

}