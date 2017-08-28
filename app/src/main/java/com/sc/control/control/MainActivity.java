package com.sc.control.control;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import okhttp3.Call;
import org.json.JSONException;
import org.json.JSONObject;
import utils.ExitUtils;
import utils.HttpUrl;
import utils.JsonParser;
import utils.LocalUtil;
import utils.SharePreUtil;
import utils.User;

public class MainActivity extends AppCompatActivity
    implements SensorEventListener, LocationListener {

  @BindView(R.id.mic) ImageView mic;
  @BindView(R.id.bottom) LinearLayout bottom;
  @BindView(R.id.list_content) ListView listContent;
  @BindView(R.id.sensor_content) LinearLayout sensorContent;
  @BindView(R.id.hide) TextView hide;
  // 语音听写对象
  private SpeechRecognizer mIat;
  // 用HashMap存储听写结果
  private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
  private long exitTime = 0;
  List<String> words;
  List<Bitmap> imgs;
  ContentAdapter adapter;
  MediaPlayer player;
  int count, num;
  boolean b = true;
  //private String phoneNum;
  public static int SET_CODE = 0x00a1;
  private SensorManager mSensorManager;
  List<Sensor> sensors;
  List<Sensor> Has;
  List<TextView> contents;
  TextView lat;
  private double latitude = 0.0;
  private double longitude = 0.0;
  private LocationManager locationManager;
  PopupMenu popup;
  String speak_err,answer_err,mp3_err;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ExitUtils.activities.add(this);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    SpeechUtility.createUtility(this, SpeechConstant.APPID + "=59424edc");
    popup=new PopupMenu(this,hide);
    popup.getMenuInflater().inflate(R.menu.selcet_menu, popup.getMenu());
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()){
          case R.id.phoneInfo:
            if (sensorContent.getVisibility() == View.VISIBLE) {
              sensorContent.setVisibility(View.GONE);
              menuItem.setTitle(getResources().getString(R.string.open));
            } else {
              sensorContent.setVisibility(View.VISIBLE);
              menuItem.setTitle(getResources().getString(R.string.hide));
            }
            break;
          case R.id.httpInfo:
            Intent intent=new Intent(MainActivity.this,HttpContentActivity.class);
            intent.putExtra("xf",speak_err);
            intent.putExtra("mimi",answer_err);
            intent.putExtra("tts",mp3_err);
            startActivity(intent);
            break;
        }
        return false;
      }
    });
    initSpeak();
    getLoc(this);
    initSensor();
    initList();
  }

  private void initSensor() {
    sensors = new ArrayList<>();
    contents = new ArrayList<>();
    Has = new ArrayList<>();
    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
    sensors.add(mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));
    for (int i = 0; i < sensors.size(); i++) {
      if (sensors.get(i) != null) {
        mSensorManager.registerListener(this, sensors.get(i), SensorManager.SENSOR_DELAY_NORMAL);
        Has.add(sensors.get(i));
        TextView tv = new TextView(this);
        contents.add(tv);
        sensorContent.addView(tv);
      }
    }
    PackageManager pack = getPackageManager();
    String version = null;
    try {
      PackageInfo info = pack.getPackageInfo(this.getPackageName(), 0);
      version = info.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    TextView tv1 = new TextView(this);
    tv1.setText("手机识别码:\n" + StartActivity.getUniquePsuedoID());
    sensorContent.addView(tv1);
    TextView tv2 = new TextView(this);
    tv2.setText("手机版本号:\n" + version);
    sensorContent.addView(tv2);
    TextView tv3 = new TextView(this);
    tv3.setText("当前经纬度:\n" + longitude + "," + latitude);
    sensorContent.addView(tv3);
    lat = tv3;
  }

  private void initList() {
    words = new ArrayList<>();
    imgs = new ArrayList<>();
    User user = SharePreUtil.getObject("msp", this, "user", User.class);
    //TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    //phoneNum = tm.getLine1Number();//获取本机号码
    if (user != null) {
      byte[] byteArray = Base64.decode(user.getImg(), Base64.DEFAULT);
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
      Bitmap bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
      imgs.add(bitmap);
    } else {
      imgs.add(BitmapFactory.decodeResource(getResources(), R.mipmap.smile));
    }
    imgs.add(BitmapFactory.decodeResource(getResources(), R.mipmap.smill));
    words.add("m" + getResources().getString(R.string.hello));
    adapter = new ContentAdapter(words, imgs, this);
    listContent.setAdapter(adapter);
    mIat.startListening(mRecListener);
  }

  private void initSpeak() {
    mIat = SpeechRecognizer.createRecognizer(this, null);
    mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
    mIat.setParameter(SpeechConstant.DOMAIN, "iat");
    mIat.setParameter(SpeechConstant.ACCENT, "mandarin");
    // 设置听写引擎
    mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    // 设置返回结果格式
    mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
    // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
    mIat.setParameter(SpeechConstant.VAD_BOS, "10000");

    // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
    mIat.setParameter(SpeechConstant.VAD_EOS, "3000");

    // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
    mIat.setParameter(SpeechConstant.ASR_PTT, "1");
  }

  private void printResult(RecognizerResult results) {
    String text = JsonParser.parseIatResult(results.getResultString());

    String sn = null;
    // 读取json结果中的sn字段
    try {
      JSONObject resultJson = new JSONObject(results.getResultString());
      sn = resultJson.optString("sn");
    } catch (JSONException e) {
      e.printStackTrace();
    }

    mIatResults.put(sn, text);

    StringBuilder resultBuilder = new StringBuilder();
    for (String key : mIatResults.keySet()) {
      resultBuilder.append(mIatResults.get(key));
    }
    if (count == 0) {
      words.clear();
      count = 1;
    }
    words.add("u" + resultBuilder.toString());
    listContent.invalidateViews();
    requestService(resultBuilder.toString());
  }

  private RecognizerListener mRecListener = new RecognizerListener() {
    @Override public void onVolumeChanged(int i, byte[] bytes) {

    }

    @Override public void onBeginOfSpeech() {
      //Toast.makeText(MainActivity.this, "开启语音", Toast.LENGTH_SHORT).show();
    }

    @Override public void onEndOfSpeech() {
      //Toast.makeText(MainActivity.this, "结束语音，等待回答", Toast.LENGTH_SHORT).show();
      //mIat.stopListening();
    }

    @Override public void onResult(RecognizerResult recognizerResult, boolean b) {
      mIat.stopListening();
      if (!b) printResult(recognizerResult);
    }

    @Override public void onError(SpeechError speechError) {
      if (b) {
        Toast.makeText(MainActivity.this, "可以语音", Toast.LENGTH_SHORT).show();
        mIat.startListening(mRecListener);
      }
      speak_err=speechError.toString();
    }

    @Override public void onEvent(int i, int i1, int i2, Bundle bundle) {

    }
  };

  @OnClick({ R.id.set, R.id.mic, R.id.hide }) public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.set:
        b = false;
        startActivityForResult(new Intent(MainActivity.this, SetInfoActivity.class), SET_CODE);
        mIat.stopListening();
        break;
      case R.id.mic:
        switchSpeak();
        listContent.invalidateViews();
        break;
      case R.id.hide:
        popup.show();
        break;
    }
  }

  private void switchSpeak() {
    if (b) {
      Toast.makeText(this, "关闭语音", Toast.LENGTH_SHORT).show();
      bottom.setBackgroundColor(getResources().getColor(R.color.lightpink));
      mic.setImageResource(R.mipmap.micon);
      mIat.stopListening();
      b = false;
    } else {
      Toast.makeText(this, "开启语音", Toast.LENGTH_SHORT).show();
      mIat.startListening(mRecListener);
      bottom.setBackgroundColor(getResources().getColor(R.color.colorAccent));
      mic.setImageResource(R.mipmap.arecord);
      b = true;
    }
  }

  public void requestService(final String speakResult) {
    OkHttpUtils.get()
        .url(HttpUrl.url)
        .addParams("mobile_id", StartActivity.getUniquePsuedoID())
        .addParams("q", speakResult)
        .addParams("latlnt", LocalUtil.getLoc(MainActivity.this))
        .build()
        .execute(new StringCallback() {
          @Override public void onError(Call call, Exception e, int id) {
            //speakWord("连接网络超时！");
            Toast.makeText(MainActivity.this,"连接网络超时!",Toast.LENGTH_SHORT).show();
            answer_err="访问mimi.php出问题了："+e.toString();
          }

          @Override public void onResponse(String response, int id) {
            answer_err="mimi.php返回的信息："+response;
            String result = response.trim();
            String lastWord = result.substring(result.lastIndexOf(">") + 1).trim();
            if (lastWord.equals("")) {
              String str = "这么简单的问题不要问我！";
              speakWord(str);
            } else {
              speakWord(lastWord);
            }
          }
        });
  }

  public void speakWord(final String res) {
    player = new MediaPlayer();
    player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override public void onCompletion(MediaPlayer mediaPlayer) {
        File file =
            new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/answer.mp3");
        file.delete();
        player.stop();
        player.release();
        player = null;
        if (b) mIat.startListening(mRecListener);
      }
    });
    OkHttpUtils.get()
        .url(HttpUrl.Resulturl)
        .addParams("tl", "zh")
        .addParams("q", res)
        .build()
        .execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(),
            "answer.mp3") {
          @Override public void onError(Call call, Exception e, int id) {
            mp3_err="tts.php访问错误:"+e.toString();
          }

          @Override public void onResponse(File response, int id) {
            mp3_err="下载的MP3地址："+response.getAbsolutePath();
            try {
              player.reset();
              player.setDataSource(response.getAbsolutePath());
              player.prepare();
              player.start();
            } catch (IOException e) {
              e.printStackTrace();
            }
            words.add("m" + res);
            listContent.invalidateViews();
          }
        });
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      exit();
      return false;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void exit() {
    if ((System.currentTimeMillis() - exitTime) > 2000) {
      Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
      exitTime = System.currentTimeMillis();
    } else {
      sendData();
      ExitUtils.finishAll();
      System.exit(0);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == SET_CODE) {
      if (SharePreUtil.getObject("msp", this, "user", User.class) != null) {
        byte[] byteArray =
            Base64.decode(SharePreUtil.getObject("msp", this, "user", User.class).getImg(),
                Base64.DEFAULT);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        Bitmap img = BitmapFactory.decodeStream(byteArrayInputStream);
        imgs.set(0, img);
        adapter.notifyDataSetChanged();
      }
      b = true;
      mIat.startListening(mRecListener);
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    if (null != mIat) {
      // 退出时释放连接
      mIat.cancel();
      mIat.destroy();
      if (player != null) {
        player.stop();
        player.release();
      }
    }
    if (mSensorManager != null && Has.size() > 0) {
      for (int i = 0; i < Has.size(); i++) {
        mSensorManager.unregisterListener(this, Has.get(i));
      }
    }
  }

  @Override public void onSensorChanged(SensorEvent sensorEvent) {
    float x = sensorEvent.values[0];
    float y = sensorEvent.values[1];
    float z = sensorEvent.values[2];
    num++;
    if (Has.size() > 0) {
      for (int i = 0; i < Has.size(); i++) {
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

          contents.get(i).setText("当前手机三轴角速度值:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

          contents.get(i).setText("当前手机三轴加速度值:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {

          contents.get(i).setText("当前手机压强值:\nx=" + x);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {

          BigDecimal bd = new BigDecimal(x);
          double humidity = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
          contents.get(i).setText("当前环境湿度值:\nx=" + humidity);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {

          contents.get(i).setText("当前手机方向:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
          BigDecimal bd = new BigDecimal(x);
          double temperature = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
          contents.get(i).setText("当前环境温度值:\n" + temperature + "℃");
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {

          contents.get(i).setText("当前手机重力值:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
          double value = Math.sqrt(x * x + y * y + z * z);
          String str = String.format("X:%8.4f , Y:%8.4f , Z:%8.4f ,总值为：%8.4f", x, y, z, value);
          contents.get(i).setText("当前手机三轴磁场值:\n" + str);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {

          contents.get(i).setText("当前环境光线值:\n" + x);
        }
      }
    }
  }

  @Override public void onAccuracyChanged(Sensor sensor, int i) {

  }

  public void getLoc(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    //获取所有可用的位置提供器
    List<String> providers = locationManager.getProviders(true);
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    }
    if (providers.contains(LocationManager.NETWORK_PROVIDER))
    //如果是Network
    {
      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, this);
    }
    if (providers.contains(LocationManager.GPS_PROVIDER)) {
      //如果是GPS
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
    }
  }

  @Override public void onLocationChanged(Location location) {
    if (location != null) {
      latitude = location.getLatitude(); // 纬度
      longitude = location.getLongitude(); // 经度
      lat.setText("当前经纬度:\n" + longitude + "," + latitude);
    }
  }

  @Override public void onStatusChanged(String s, int i, Bundle bundle) {

  }

  @Override public void onProviderEnabled(String s) {

  }

  @Override public void onProviderDisabled(String s) {

  }

  public void sendData() {
    User user = SharePreUtil.getObject("msp", this, "user", User.class);
    String name = "NoName";
    if (user != null) {
      name = user.getName();
    }
    OkHttpUtils.get()
        .url(HttpUrl.Loginurl)
        .addParams("mobile_id", StartActivity.getUniquePsuedoID())
        .addParams("name", name)
        .build()
        .execute(null);
  }
}
