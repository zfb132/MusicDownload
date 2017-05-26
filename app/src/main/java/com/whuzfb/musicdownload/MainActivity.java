package com.whuzfb.musicdownload;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zfb15 on 2017/5/25.
 */

public class MainActivity extends Activity{
    public static String url_searchMusic="https://c.y.qq.com/soso/fcgi-bin/client_search_cp?new_json=1&aggr=1&cr=1&flag_qc=0&p=%1&n=%10&w=";
    public static String url_vkey="https://c.y.qq.com/base/fcgi-bin/fcg_music_express_mobile3.fcg?format=json&platform=yqq&cid=205361747&songmid=";
    public static String url_play="http://dl.stream.qqmusic.qq.com/";
    public static String url_download="";

    public static String keyword="惊鸿一面";
    public static String guid="";
    public static String vkey="";
    public static String mid="";
    public static String media_mid="";
    public static String singer="";

    public static String cliptext="";
    public static boolean flag=true;

    public final static String IMEI_1="864855026227282";
    public final static String IMEI_2="867050025665727";

    public static Map<String, String> cookies_music=new HashMap<>();
    public static Map<String, String> header_lyric=new HashMap<>();

    public Button btn_next=null;
    public Button btn_search=null;
    public EditText et_keyword=null;
    public TextView tv_show=null;

    public Context context=null;
    public NotificationManager notificationManager=null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if((!getIMEI().equals(IMEI_1))&&(!getIMEI().equals(IMEI_2))){
            unInstall(MainActivity.this.getPackageName());
            finish();
        }

        //创建目录
        createdirs();

        if(!netConnect()){
            Toast.makeText(MainActivity.this,"请先连接网络后再使用",Toast.LENGTH_SHORT).show();
        }
        if(!wifiConnect()){
            Toast.makeText(MainActivity.this,"建议您连接WIFI后使用",Toast.LENGTH_SHORT).show();
        }

        btn_next=(Button)findViewById(R.id.btn_next);
        btn_search=(Button)findViewById(R.id.btn_search);
        et_keyword=(EditText)findViewById(R.id.edit_keyword);
        tv_show=(TextView)findViewById(R.id.tv_show);
        tv_show.setTextIsSelectable(true);

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setClass(MainActivity.this,MusicList.class);
                startActivity(intent);
            }
        });
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断是否联网
                if(netConnect()){
                    //判断关键词是否为空
                    if(et_keyword.getText().toString().equals("")){
                        showToast("请先输入歌曲名字",Toast.LENGTH_SHORT);
                    }else {
                        tv_show.setText("");
                        new Thread(networkTask).start();
                        //showToast("文件保存到/sdcard/MusicDownload/url.txt",Toast.LENGTH_LONG);
                    }
                }else{
                    showToast("请先连接网络后再使用",Toast.LENGTH_SHORT);
                }
            }
        });

        context=getApplicationContext();
        String ns=Context.NOTIFICATION_SERVICE;
        notificationManager=(NotificationManager)getSystemService(ns);

    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            if(val.equals("ERROR")){
                tv_show.setText("未搜索到任何相关内容！");
            }else {
                tv_show.setText(tv_show.getText().toString()+val);
                if(data.getInt("times")==(data.getInt("total"))){
                    textToClipBoard(cliptext);
                    showNote(1,"url已保存，点击打开");
                }
            }
        }
    };

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            keyword=et_keyword.getText().toString();
            doAll(keyword);
        }
    };

    public void showNote(int id,String text){
        CharSequence tickertext="这是一个显示在状态了的通知！";
        CharSequence contexttitle="通知";
        CharSequence contexttext=text;
        long time=System.currentTimeMillis();

        File file=new File("/sdcard/MusicDownload/url.txt");

        //Intent notificationintent=new Intent(context,MainActivity.class);
        //notificationintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //PendingIntent对象指定了当用户单击扩展的Notification时应用程序如何跳转
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
        PendingIntent pendingIntent=PendingIntent.getActivity(context,0,intent,0);

        Notification.Builder builder=new Notification.Builder(context);
        //设置通知栏左边的大图标
        //builder.setLargeIcon();
        //设置通知栏右边的小图标
        builder.setSmallIcon(R.drawable.ic_download);
        //通知首次出现在通知栏，带上升动画效果的
        builder.setTicker(tickertext);
        //通知产生的时间，会在通知信息里显示
        builder.setWhen(time);
        //设置通知的内容
        builder.setContentText(contexttext);
        //设置通知的标题
        builder.setContentTitle(contexttitle);
        //设置通知的优先级
        //builder.setPriority(Notification.PRIORITY_MAX);
        //设置这个标志当用户单击面板就可以让通知自动取消
        builder.setAutoCancel(true);
        //设置它为一个正在进行的通知。（通常用来表示一个后台任务，用户积极参与或以某种方式正在等待）
        //builder.setOngoing(true);
        //向通知添加声音、闪灯和震动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性
        //builder.setDefaults(Notification.DEFAULT_VIBRATE|Notification.DEFAULT_SOUND);
        builder.setContentIntent(pendingIntent);
        Notification notification=builder.build();
        notification.defaults=notification.DEFAULT_SOUND;
        //发起通知
        notificationManager.notify(id,notification);
        //取消显示通知
        //mNotificationManager.cancel(1);
    }

    public void doAll(String key){
        JSONArray list=searchMusicInfo(key);
        //未搜索到的情况下list的内容为[]
        if(list.toString().equals("[]")){
            flag=false;
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", "ERROR");
            msg.setData(data);
            handler.sendMessage(msg);
        }
        for (int i=0;i<list.length();i++){
            JSONObject ma=null;
            JSONObject mb=null;
            JSONObject mc=null;
            try{
                ma=new JSONObject(list.get(i).toString());
            }catch (Exception e){
                e.printStackTrace();
            }
            try {
                mid=""+ma.get("mid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                mb=new JSONObject(ma.get("file").toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                media_mid=""+mb.get("media_mid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                mc=new JSONObject(ma.getJSONArray("singer").get(0).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                singer=""+mc.get("title");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d("guid","wcdgvui");
            guid=getGuid();

            vkey=getVkey(media_mid,mid);

            url_download=getMusicDownloadUrl(media_mid,vkey);

            //setMusicCookies();
            Message msg = new Message();
            Bundle data = new Bundle();
            String temp=i+"\nkeyword="+key+"\nmid="+mid+"\nsinger="+singer+"\nguid="+guid+"\nvkey="+vkey+"\nurl_download="+url_download+"\n\n";
            if(i==0){
                Log.d("i===0","wchcgiuhhrbjjrbhj");
                cliptext=url_download;
                writeData("/sdcard/MusicDownload/url.txt","关键词："+keyword+"\t\t歌手："+singer+"\n下载网址：\n"+url_download+"\n\n",false);
            }else {
                writeData("/sdcard/MusicDownload/url.txt","关键词："+keyword+"\t\t歌手："+singer+"\n下载网址：\n"+url_download+"\n\n",true);
            }

            data.putInt("times",i);
            data.putInt("total",list.length()-1);
            data.putString("value", temp);

            msg.setData(data);
            handler.sendMessage(msg);
        }
    }

    public JSONArray searchMusicInfo(String word){
        String url=url_searchMusic;
        Log.d("word",word);

        url=url+word;
        Log.d("url1",url);
        Connection.Response res = null;
        try {
            res = Jsoup.connect(url)
                    .header("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
                    .timeout(10000).ignoreContentType(true).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String body = res.body().
                replace("callback(","")
                .replace(")","");
        Log.d("res",body);
        writeData("/sdcard/MusicDownload/test.json",body,false);
        JSONArray list=null;
        JSONObject json = null;
        try {
            json = new JSONObject(body);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            list=json.getJSONObject("data").getJSONObject("song").getJSONArray("list");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void textToClipBoard(String str){
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        //setText()然后getText()感觉很方便，不过被弃用了
        //cm.setText("");
        ClipData cp;
        cp=ClipData.newPlainText("text",str);
        cm.setPrimaryClip(cp);
        showToast("已将第一个结果复制到剪切板",Toast.LENGTH_SHORT);
    }

    public boolean isInstalled(Context context,String name){
        // 直接尝试获得该name的应用信息，如果失败说明未安装
        PackageInfo packageInfo;
        try {
            packageInfo=context.getPackageManager().getPackageInfo(name,0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo=null;
            e.printStackTrace();
        }
        if(packageInfo==null){
            return false;
        }else{
            return true;
        }
    }

    public void unInstall(String packageName){
        Uri uri=Uri.parse("package:"+packageName);
        Intent intent=new Intent(Intent.ACTION_DELETE,uri);
        startActivity(intent);
    }

    public boolean wifiConnect(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net=cm.getActiveNetworkInfo();
        if(net!=null&&net.getType()==ConnectivityManager.TYPE_WIFI){
            return true;
        }
        return false;
    }

    public boolean netConnect(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net=cm.getActiveNetworkInfo();
        if(net!=null&&net.isAvailable()){
            return true;
        }
        return false;
    }

    public String getIMEI(){
        TelephonyManager tm=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getDeviceId();
    }

    public String getVkey(String filename,String id){
        String url=url_vkey;
        url=url+id+"&filename=C400"+filename+".m4a&guid="+guid;
        Connection.Response res = null;
        try {
            res = Jsoup.connect(url)
                    .header("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0")
                    .timeout(10000).ignoreContentType(true).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject json = null;
        try {
            json = new JSONObject(res.body());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            json=new JSONObject(json.getJSONObject("data").getJSONArray("items").get(0).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            return ""+json.get("vkey");
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getGuid(){
        return ""+(Math.round(Math.random() * 2147483647) * (System.currentTimeMillis()%1000)) % 10000000000L;
    }

    public String getMusicDownloadUrl(String filename,String key){
        return url_play+"C400"+filename+".m4a?vkey="+key+"&guid="+guid;
    }

    public void setMusicCookies(){
        cookies_music.put("pgv_pvi","290881536");
        cookies_music.put("pgv_si","s4324782080");
        cookies_music.put("pgv_pvid",guid);
        cookies_music.put("qqmusic_fromtag","66");
    }

    public void setLyricHeaders(){
        header_lyric.put("cookie","skey=@LVJPZmJUX; p");
        header_lyric.put("Referer","https://y.qq.com/portal/player.html");

        /*
        try {
            Jsoup.connect(url_play)
                    .headers(header_lyric)
                    .timeout(10000).ignoreContentType(true).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

    }

    public void createdirs(){
        if (checkSDCard()) {
            File recordPath = Environment.getExternalStorageDirectory();
            File path = new File(recordPath.getPath() + File.separator + "MusicDownload");
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    //("创建目录失败", Toast.LENGTH_LONG);
                    return;
                }
            }
            recordPath = path;
        } else {
            //showToast("SD卡未连接", Toast.LENGTH_LONG);
            return;
        }
    }

    public boolean checkSDCard() {
        //检测SD卡是否插入手机
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    public void writeData(String filename,String string,boolean append){
        try{
            //文件输出流，如果目标文件不存在，新建一个；如果已存在，默认覆盖
            FileOutputStream fileOutputStream=new FileOutputStream(filename,append);
            byte[] bytes=string.getBytes();
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readSDFile(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte [] buffer = new byte[length];
        fis.read(buffer);
        String res = new String(buffer);
        fis.close();
        return res;
    }

    public void showToast(String str,int duration) {
        Toast toast = Toast.makeText(this, str, duration);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 200);
        toast.show();
    }
}
