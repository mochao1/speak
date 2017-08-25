package utils;

import android.graphics.Bitmap;

/**
 * Created by messi.mo on 2017-08-21.
 */

public class User {

  private String language;
  private String name;
  private String sex;
  private String img;

  public String getImg() {
    return img;
  }

  public void setImg(String img) {
    this.img = img;
  }

  public User(String language, String name, String sex, String img) {
    this.language = language;
    this.name = name;
    this.sex = sex;
    this.img=img;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  @Override public String toString() {
    return "User{"
        + "language='"
        + language
        + '\''
        + ", name='"
        + name
        + '\''
        + ", sex='"
        + sex
        + '\''
        + ", img='"
        + img
        + '\''
        + '}';
  }
}
