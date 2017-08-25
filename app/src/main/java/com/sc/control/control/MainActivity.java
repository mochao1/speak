package com.sc.control.control;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  @BindView(R.id.mic) ImageView mic;
  @BindView(R.id.bottom) LinearLayout bottom;
  @BindView(R.id.list_content) ListView listContent;
  @BindView(R.id.sensor_content) LinearLayout sensorContent;
  @BindView(R.id.hide) TextView hide;
  // 语音听写对象
  private SpeechRecognizer mIat;
  // 引擎类型
  private String mEngineType = SpeechConstant.TYPE_CLOUD;
  // 用HashMap存储听写结果
  private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
  private long exitTime = 0;
  List<String> words;
  List<Bitmap> imgs;
  ContentAdapter adapter;
  MediaPlayer player;
  int count = 0;
  boolean b = true;
  private String phoneNum;
  public static int SET_CODE = 0x00a1;
  private boolean mRegisteredSensor;
  private SensorManager mSensorManager;
  private float x, y, z;
  List<Sensor> sensors;
  List<Sensor> Has;
  List<TextView> contents;
  TextView lat;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ExitUtils.activities.add(this);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    SpeechUtility.createUtility(this, SpeechConstant.APPID + "=59424edc");
    initSpeak();
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
    tv3.setText("当前经纬度:\n" + LocalUtil.getLoc(this));
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
    mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
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
      mIat.stopListening();
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
        if (sensorContent.getVisibility() == View.VISIBLE) {
          sensorContent.setVisibility(View.GONE);
          hide.setText(getResources().getString(R.string.open));
        } else {
          sensorContent.setVisibility(View.VISIBLE);
          hide.setText(getResources().getString(R.string.hide));
          lat.setText("当前经纬度:\n" + LocalUtil.getLoc(this));
        }
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
            speakWord("连接网络超时！");
          }

          @Override public void onResponse(String response, int id) {
            String result = response.trim();
            String lastWord = result.substring(result.lastIndexOf(">") + 1).trim();
            if (lastWord.equals("")) {
              String str = "咪咪能力有限，问点简单的吧！";
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
            words.add("m" + e);
          }

          @Override public void onResponse(File response, int id) {
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
    if (mRegisteredSensor) {
      mSensorManager.unregisterListener(this);
      mRegisteredSensor = false;
    }
  }

  @Override public void onSensorChanged(SensorEvent sensorEvent) {
    x = sensorEvent.values[0];
    y = sensorEvent.values[1];
    z = sensorEvent.values[2];
    if (Has.size() > 0) {
      for (int i = 0; i < Has.size(); i++) {
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

          contents.get(i).setText("当前手机三轴角速度:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

          contents.get(i).setText("当前手机三轴加速度:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {

          contents.get(i).setText("当前手机方向:\nx=" + x + ",y=" + y + ",z=" + z);
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
          float temperatureValue = sensorEvent.values[0];
          BigDecimal bd = new BigDecimal(temperatureValue);
          double temperature = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
          contents.get(i).setText("当前手机温度:\n" + temperature + "℃");
        }
        if (Has.get(i).getType() == sensorEvent.sensor.getType()
            && sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {

          contents.get(i).setText("当前手机重力:\nx=" + x + ",y=" + y + ",z=" + z);
        }
      }
    }
  }

  @Override public void onAccuracyChanged(Sensor sensor, int i) {

  }
}
