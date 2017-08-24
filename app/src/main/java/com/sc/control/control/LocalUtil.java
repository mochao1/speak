package com.sc.control.control;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;
import java.util.List;

/**
 * Created by Messi.Mo on 2017/8/21 0021.
 */

public class LocalUtil {
  private static double latitude = 0.0;
  private static double longitude = 0.0;
  private static LocationManager locationManager;
  private static String locationProvider;
  private static Location location;

  public static String getLoc(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    //获取所有可用的位置提供器
    List<String> providers = locationManager.getProviders(true);
    if (providers.contains(LocationManager.GPS_PROVIDER)) {
      //如果是GPS
      locationProvider = LocationManager.GPS_PROVIDER;
    } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
      //如果是Network
      locationProvider = LocationManager.NETWORK_PROVIDER;
    }
    if (locationProvider != null) {
      if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
          != PackageManager.PERMISSION_GRANTED
          && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
          != PackageManager.PERMISSION_GRANTED) {
      }
      location = locationManager.getLastKnownLocation(locationProvider);
      if (location != null) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
      } else {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,
            new LocationListener() {
              // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
              @Override public void onStatusChanged(String provider, int status, Bundle extras) {
              }

              // Provider被enable时触发此函数，比如GPS被打开
              @Override public void onProviderEnabled(String provider) {
              }

              // Provider被disable时触发此函数，比如GPS被关闭
              @Override public void onProviderDisabled(String provider) {
              }

              // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
              @Override public void onLocationChanged(Location location) {
                if (location != null) {
                  latitude = location.getLatitude(); // 纬度
                  longitude = location.getLongitude(); // 经度
                }
              }
            });
      }
    }

    return longitude + "," + latitude;
  }
}