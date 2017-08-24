package com.sc.control.control;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by messi.mo on 2017-08-21.
 */

public class ContentAdapter extends BaseAdapter{
  List<String> list_content=new ArrayList<>();
  List<Bitmap> imgs=new ArrayList<>();
  Context context;
  @Override public int getCount() {
    return list_content.size();
  }

  @Override public Object getItem(int i) {
    return i;
  }

  @Override public long getItemId(int i) {
    return i;
  }

  @Override public View getView(int i, View view, ViewGroup viewGroup) {
    ViewHolder holder = null;
    String type=list_content.get(i).substring(0,1);

      if(type.equals("u")){
        view = LayoutInflater.from(context)
            .inflate(R.layout.two_content, viewGroup, false);
      }else if(type.equals("m")){
        view = LayoutInflater.from(context)
            .inflate(R.layout.text_content, viewGroup, false);
      }
      TextView word= (TextView) view.findViewById(R.id.content);
      ImageView img= (ImageView) view.findViewById(R.id.img);
      holder = new ViewHolder(word,img);
    holder.content.setText(list_content.get(i).substring(1));
    if(type.equals("u")){
      holder.image.setImageBitmap(imgs.get(0));
      holder.content.setBackground(context.getResources().getDrawable(R.drawable.corner));
    }else if(type.equals("m")){
      holder.image.setImageBitmap(imgs.get(1));
      holder.content.setBackground(context.getResources().getDrawable(R.drawable.mcorner));
    }
    return view;
  }

  public ContentAdapter(List<String> list_content, List<Bitmap> imgs,Context context) {
    this.list_content = list_content;
    this.imgs = imgs;
    this.context=context;
  }

  class ViewHolder {
    TextView content;
    ImageView image;

    public ViewHolder(TextView content, ImageView image) {
      this.content = content;
      this.image = image;
    }
  }

}
