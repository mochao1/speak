package com.sc.control.control;

import android.app.Application;
import android.os.StrictMode;

/**
 * Created by Messi.Mo on 2017/8/23 0023.
 */

public class MyApplication extends Application{
  @Override public void onCreate() {
    super.onCreate();
    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();StrictMode.setVmPolicy(builder.build());builder.detectFileUriExposure();
  }
}
