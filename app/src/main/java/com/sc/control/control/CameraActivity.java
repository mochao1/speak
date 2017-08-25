package com.sc.control.control;

import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by Messi.Mo on 2017/8/22 0022.
 */

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
  @BindView(R.id.sf) SurfaceView sf;
  private Camera camera = null;
  Camera.Parameters parameters;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.camera_view);
    ButterKnife.bind(this);
    init();
  }

  private void init() {
    SurfaceHolder surfaceholder= sf.getHolder();
    surfaceholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    surfaceholder.addCallback(this);
  }

  @Override public void surfaceCreated(SurfaceHolder holder) {
    // 获取camera对象
    camera = Camera.open();
    try {
      // 设置预览监听
      camera.setPreviewDisplay(holder);
      Camera.Parameters parameters = camera.getParameters();

      if (this.getResources().getConfiguration().orientation
          != Configuration.ORIENTATION_LANDSCAPE) {
        parameters.set("orientation", "portrait");
        camera.setDisplayOrientation(90);
        parameters.setRotation(90);
      } else {
        parameters.set("orientation", "landscape");
        camera.setDisplayOrientation(0);
        parameters.setRotation(0);
      }
      camera.setParameters(parameters);
      // 启动摄像头预览
      camera.startPreview();
    } catch (IOException e) {
      e.printStackTrace();
      camera.release();
    }
  }

  @Override public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    camera.autoFocus(new Camera.AutoFocusCallback() {
      @Override public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
          initCamera();// 实现相机的参数初始化
          camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
        }
      }
    });
  }

  @Override public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    if (camera != null) {
      camera.stopPreview();
      camera.release();
    }
  }

  // 相机参数的初始化设置
  private void initCamera() {
    parameters = camera.getParameters();
    parameters.setPictureFormat(PixelFormat.JPEG);
    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
    setDispaly(parameters, camera);
    camera.setParameters(parameters);
    camera.startPreview();
    camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
  }

  // 控制图像的正确显示方向
  private void setDispaly(Camera.Parameters parameters, Camera camera) {
    if (Integer.parseInt(Build.VERSION.SDK) >=19) {
      setDisplayOrientation(camera, 90);
    } else {
      parameters.setRotation(90);
    }
  }

  // 实现的图像的正确显示
  private void setDisplayOrientation(Camera camera, int i) {
    Method downPolymorphic;
    try {
      downPolymorphic =
          camera.getClass().getMethod("setDisplayOrientation", int.class);
      if (downPolymorphic != null) {
        downPolymorphic.invoke(camera, i );
      }
    } catch (Exception e) {
      Log.e("Came_e", "图像出错");
    }
  }
}
