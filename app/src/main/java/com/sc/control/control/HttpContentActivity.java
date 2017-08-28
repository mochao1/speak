package com.sc.control.control;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class HttpContentActivity extends AppCompatActivity {

  @BindView(R.id.content_http) TextView contentHttp;
  String speak_err,answer_err,mp3_err;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.http_content);
    ButterKnife.bind(this);
    Intent intent=getIntent();
    speak_err=intent.getStringExtra("xf");
    answer_err=intent.getStringExtra("mimi");
    mp3_err=intent.getStringExtra("tts");
    contentHttp.setText("讯飞语音服务器访问情况:"+speak_err+"\n咪咪服务器访问情况:"+answer_err+"\n生成mp3服务器访问情况:"+mp3_err);
  }
}
