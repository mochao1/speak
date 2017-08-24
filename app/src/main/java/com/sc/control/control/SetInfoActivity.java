package com.sc.control.control;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * Created by Messi.Mo on 2017/8/18 0018.
 */

public class SetInfoActivity extends AppCompatActivity {

  @BindView(R.id.language) TextView language;
  @BindView(R.id.user_name) EditText userName;
  @BindView(R.id.head_img) ImageView headImg;
  private static final int CODE_GALLERY_REQUEST = 0xa0;
  private static final int CODE_RESULT_REQUEST = 0xa2;
  private static final int CODE_TAKE_PHOTO = 0xa3;
  @BindView(R.id.man) RadioButton man;
  @BindView(R.id.woman) RadioButton woman;
  User user;
  private Bitmap bitmap;
  public static Bitmap photo = null;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ExitUtils.activities.add(this);
    setContentView(R.layout.set_info);
    ButterKnife.bind(this);
    user = SharePreUtil.getObject("msp", this, "user", User.class);
    if (user != null) {
      userName.setText(user.getName());
      if ("男".equals(user.getSex())) {
        man.setChecked(true);
      } else if ("女".equals(user.getSex())) {
        woman.setChecked(true);
      }
      byte[] byteArray = Base64.decode(user.getImg(), Base64.DEFAULT);
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
      bitmap = BitmapFactory.decodeStream(byteArrayInputStream);
      headImg.setImageBitmap(bitmap);
    }
  }

  @OnClick({ R.id.back, R.id.select, R.id.save, R.id.camera })
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.back:
        setResult(MainActivity.SET_CODE);
        finish();
        break;
      case R.id.select:
        choseHeadImageFromGallery();
        break;
      case R.id.camera:
        takePhoto();
        break;
      case R.id.save:
        saveInfo();
        break;
    }
  }

  private void takePhoto() {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
    //Uri fileUri = Uri.fromFile(getOutputMediaFile(MEDIA_TYPE_IMAGE));
    //intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
    startActivityForResult(intent, CODE_TAKE_PHOTO);
  }

  private void saveInfo() {
    String sex = null;
    String lang = language.getText().toString();
    String name = userName.getText().toString().trim();
    String img = null;
    Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
    if (man.isChecked()) {
      sex = man.getText().toString();
    } else {
      sex = woman.getText().toString();
    }
    if (bitmap == null) {
      Toast.makeText(this, "请选择头像", Toast.LENGTH_SHORT).show();
    } else {
      //第一步:将Bitmap压缩至字节数组输出流ByteArrayOutputStream
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
      //第二步:利用Base64将字节数组输出流中的数据转换成字符串String
      byte[] byteArray = byteArrayOutputStream.toByteArray();
      img = new String(Base64.encodeToString(byteArray, Base64.DEFAULT));
      user = new User(lang, name, sex, img);
      SharePreUtil.putObject("msp", SetInfoActivity.this, "user", User.class, user);
      Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
    }
  }

  // 从本地相册选取图片作为头像
  private void choseHeadImageFromGallery() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
    startActivityForResult(intent, CODE_GALLERY_REQUEST);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    //用户没有进行有效的设置操作，返回
    if (resultCode == RESULT_CANCELED) {
      Toast.makeText(SetInfoActivity.this, "取消操作", Toast.LENGTH_SHORT).show();
      return;
    }
    if (requestCode == CODE_GALLERY_REQUEST) {
      crop(intent.getData(), 150);
    }
    if (requestCode == CODE_RESULT_REQUEST) {

      if (intent != null) setPicToView(intent);
    }
    if (requestCode == CODE_TAKE_PHOTO) {
       if(intent!=null&&intent.hasExtra("data")){
         bitmap=intent.getParcelableExtra("data");
         bitmap=BitmapUtils.createScaleBitmap(bitmap,headImg.getWidth(),headImg.getHeight(),1);
         headImg.setImageBitmap(bitmap);

       }
    }
    super.onActivityResult(requestCode, resultCode, intent);
  }

  //将进行剪裁后的图片显示到UI界面上
  private void setPicToView(Intent picData) {
    Bundle bundle = picData.getExtras();
    if (bundle != null) {
      bitmap = bundle.getParcelable("data");
      headImg.setImageBitmap(bitmap);
    }
  }

  private void crop(Uri uri, int size) {
    Intent intent = new Intent("com.android.camera.action.CROP");
    intent.setDataAndType(uri, "image/*");
    // crop为true是设置在开启的intent中设置显示的view可以剪裁
    intent.putExtra("crop", "true");
    // aspectX aspectY 是宽高的比例
    intent.putExtra("aspectX", 1);
    intent.putExtra("aspectY", 1);
    // outputX,outputY 是剪裁图片的宽高
    intent.putExtra("outputX", size);
    intent.putExtra("outputY", size);
    intent.putExtra("return-data", true);

    startActivityForResult(intent, CODE_RESULT_REQUEST);
  }

  private static File getOutputMediaFile(int type) {

    File mediaStorageDir = null;
    try {

      mediaStorageDir =
          new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
              "MyPhoto");
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (!mediaStorageDir.exists()) {
      if (!mediaStorageDir.mkdirs()) {
        return null;
      }
    }

    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    File mediaFile;
    if (type == MEDIA_TYPE_IMAGE) {
      mediaFile =
          new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    } else {
      return null;
    }
    return mediaFile;
  }
}
