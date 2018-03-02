package com.jiangtao.interviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.3 Mobile/14E277 Safari/603.1.30";
    private static final String TAG = "MainActivity";

    private EditText mUrlEdit;
    private ImageView mIconImage;

    private static final int MSG_ICON_URL_OK = 0x1000;
    private static final int MSG_ICON_URL_FAILED = 0x1001;
    private static final int MSG_ICON_DOWNLOADED = 0x1002;
    private static final int MSG_ICON_DOWNLOAD_FAILED = 0x1003;

    static class MyHandler extends Handler {
        WeakReference<MainActivity> mmWeakReference;

        public MyHandler(MainActivity activity) {
            mmWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ICON_URL_OK:
                    String iconUrl = (String) msg.obj;
                    Log.d(TAG, "handleMessage, " + iconUrl);
                    mmWeakReference.get().requestIcon(iconUrl);
                    break;
                case MSG_ICON_URL_FAILED:
                    Toast.makeText(mmWeakReference.get(), "Couldn't find icon url.", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_ICON_DOWNLOADED:
                    Bitmap icon = (Bitmap) msg.obj;
                    mmWeakReference.get().mIconImage.setImageBitmap(icon);
                    break;
                case MSG_ICON_DOWNLOAD_FAILED:
                    Toast.makeText(mmWeakReference.get(), "Couldn't download icon.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUrlEdit = findViewById(R.id.url_edit);
        mIconImage = findViewById(R.id.icon);

        mUrlEdit.setText("https://tieba.baidu.com");
    }


    public void onResolveClick(View view) {
        String url = mUrlEdit.getText().toString().trim();
        if (URLUtil.isValidUrl(url)) {
            requestHtml(url);
        }else{
            Toast.makeText(this, "Invalid address", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestHtml(final String urlStr) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("user-agent", USER_AGENT);
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        Log.d(TAG, "start");
                        resolveIconUrl(is);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_ICON_URL_FAILED);
                }
            }

            private void resolveIconUrl(InputStream is) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                try {
                    Pattern p = Pattern.compile("<link[^<]*rel=\"apple-touch-icon.+?>");
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = p.matcher(line);
                        if (matcher.find()) {
                            String group = matcher.group();
                            Pattern p2 = Pattern.compile("href=\".+?\"");
                            Matcher matcher2 = p2.matcher(group);
                            if (matcher2.find()) {
                                String group2 = matcher2.group();
                                String iconUrlStr = group2.substring(6, group2.length() - 1);
                                if (!iconUrlStr.startsWith("http:") && !iconUrlStr.startsWith("https:")) {
                                    iconUrlStr = "https:" + iconUrlStr;
                                }
                                Message msg = mHandler.obtainMessage(MSG_ICON_URL_OK, iconUrlStr);
                                mHandler.sendMessage(msg);
                                return;
                            }
                        }
                    }
                    mHandler.sendEmptyMessage(MSG_ICON_URL_FAILED);
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_ICON_URL_FAILED);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

    }

    private void requestIcon(final String iconUrl) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(iconUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("user-agent", USER_AGENT);
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        Log.d(TAG, "start 2");
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            Message msg = mHandler.obtainMessage(MSG_ICON_DOWNLOADED, bmp);
                            mHandler.sendMessage(msg);
                        } else {
                            mHandler.sendEmptyMessage(MSG_ICON_DOWNLOAD_FAILED);
                        }
                        is.close();
                        Log.d(TAG, "end");
                    } else {
                        mHandler.sendEmptyMessage(MSG_ICON_DOWNLOAD_FAILED);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_ICON_DOWNLOAD_FAILED);
                } finally {

                }
            }
        });
        t.start();
    }
}
