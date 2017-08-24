package com.sc.control.control;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import okhttp3.Call;

/**
 * Created by Messi.Mo on 2017/8/18 0018.
 */

public class StartActivity extends AppCompatActivity {
  @BindView(R.id.start_img) ImageView startImg;
  Date data;
  @BindView(R.id.time) TextView time;
  Location location;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ExitUtils.activities.add(this);
    setContentView(R.layout.start_activity);
    ButterKnife.bind(this);
    startImg.setImageResource(R.mipmap.logo2);
    data = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    time.setText(formatter.format(data));
    requestPermission();
  }

  private void requestPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) { //表示未授权时
      ActivityCompat.requestPermissions(this, new String[] {
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.CAMERA,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
      }, 1);
    } else {
      sendData();
    }
  }

  private void startMain() {
    new Handler().postDelayed(new Runnable() {
      @Override public void run() {
        startActivity(new Intent(StartActivity.this, MainActivity.class));
        finish();
      }
    }, 2000);
  }

  public void sendData() {
    User user = SharePreUtil.getObject("msp", this, "user", User.class);
    String name = "NoName";
    if (user != null) {
      name = user.getName();
    }

    OkHttpUtils.get()
        .url(HttpUrl.Loginurl)
        .addParams("mobile_id", getUniquePsuedoID())
        .addParams("name", name)
        .build()
        .execute(null);
    startMain();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case 1:
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //同意权限申请
          sendData();
        } else {
          Toast.makeText(this, "权限被拒绝了", Toast.LENGTH_SHORT).show();
        }
        break;
      default:
        break;
    }
  }

  public static String getUniquePsuedoID() {
    String serial = null;
    String m_szDevIDShort = "35"
        + Build.BOARD.length() % 10
        + Build.BRAND.length() % 10
        + Build.CPU_ABI.length() % 10
        + Build.DEVICE.length() % 10
        + Build.DISPLAY.length() % 10
        + Build.HOST.length() % 10
        + Build.ID.length() % 10
        + Build.MANUFACTURER.length() % 10
        + Build.MODEL.length() % 10
        + Build.PRODUCT.length() % 10
        + Build.TAGS.length() % 10
        + Build.TYPE.length() % 10
        + Build.USER.length() % 10;
    try {
      serial = android.os.Build.class.getField("SERIAL").get(null).toString();
      return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    } catch (Exception exception) {
      serial = "serial";
    }
    return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
  }
}