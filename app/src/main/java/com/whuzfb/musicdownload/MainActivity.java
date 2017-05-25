package com.whuzfb.musicdownload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    //public static String keyword="惊鸿一面";
    public static String guid="";
    public static String vkey="";
    public static String mid="";
    public static String media_mid="";
    public static String singer="";

    public static Map<String, String> cookies_music=new HashMap<>();
    public static Map<String, String> header_lyric=new HashMap<>();

    public Button btn_next=null;
    public Button btn_search=null;
    public EditText et_keyword=null;
    public TextView tv_show=null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                if(!et_keyword.getText().toString().equals(""))

                    new Thread(networkTask).start();

            }
        });
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String val = data.getString("value");
            tv_show.setText(tv_show.getText().toString()+val);

            // TODO
            // UI界面的更新等相关操作
        }
    };

    Runnable networkTask = new Runnable() {
        @Override
        public void run() {
            doAll(et_keyword.getText().toString());
        }
    };

    public void doAll(String key){
        JSONArray list=searchMusicInfo(key);
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

            guid=getGuid();

            vkey=getVkey(media_mid,mid);

            url_download=getMusicDownloadUrl(media_mid,vkey);

            //setMusicCookies();
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("value", i+"\nkeyword="+key+"\nmid="+mid+"\nsinger="+singer+"\nguid="+guid+"\nvkey="+vkey+"\nurl_download="+url_download+"\n\n");
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

    public void writeData(String filename,String string){
        try{
            //文件输出流，如果目标文件不存在，新建一个；如果已存在，默认覆盖
            FileOutputStream fileOutputStream=new FileOutputStream(filename);
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
}
