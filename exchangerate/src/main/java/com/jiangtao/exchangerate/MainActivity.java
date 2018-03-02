package com.jiangtao.exchangerate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int MSG_UPDATE_SUCCESS = 0x1000;
    private static final int MSG_UPDATE_FAILED = 0x1001;
    private static final String TAG = "MainActivity";
    private static final String KEY_COUNTRIES = "countries";

    private HashMap<String, Double> mRates = new HashMap<>();
    private List<String> mCountries = new ArrayList<>();

    static class MyHandler extends Handler {
        WeakReference<MainActivity> mmWeakReference;

        public MyHandler(MainActivity activity) {
            mmWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_SUCCESS:
                    String jsonStr = (String) msg.obj;
                    Map<String, Double> rates = mmWeakReference.get().mRates;
                    try {

                        JSONObject jsonObject = new JSONObject(jsonStr);
                        String base = jsonObject.getString("base");
                        String date = jsonObject.getString("date");
//                        Log.d(TAG, "base:" + base + ", date:" + date);
                        JSONObject ratesObject = jsonObject.getJSONObject("rates");
                        rates.clear();
                        rates.put(base, 1.0);

                        Iterator<String> iterable = ratesObject.keys();
                        while (iterable.hasNext()) {
                            String key = iterable.next();
                            rates.put(key, ratesObject.getDouble(key));
//                            Log.d(TAG, key + ":" + ratesObject.getDouble(key));
                        }

                        makeUSDBase(rates);

                        //if country list was not cached before
                        if (mmWeakReference.get().mCountries.isEmpty()) {
                            mmWeakReference.get().mCountries.addAll(rates.keySet());
                        }

                        ((MyRecyclerViewAdapter)mmWeakReference.get().mRecyclerView.getAdapter()).setData(rates);
                        mmWeakReference.get().mRecyclerView.getAdapter().notifyDataSetChanged();

                        Toast.makeText(mmWeakReference.get(), "Updated on "+date, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_UPDATE_FAILED:
                    Toast.makeText(mmWeakReference.get(), "Update failed.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        private void makeUSDBase(Map<String, Double> rates) {
            double usdRate = rates.get("USD");
            for (String key : rates.keySet()) {
                double rateBasedOnUSD = rates.get(key) / usdRate;
                rates.put(key, rateBasedOnUSD);
            }
        }
    }

    Handler mHandler = new MyHandler(this);

    private SharedPreferences mSharedPrefs;

    private RecyclerView mRecyclerView;
    private MyRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSharedPrefs = getPreferences(MODE_PRIVATE);

        initCountryListFromCache();

        initView();
    }

    private void initView() {
        mRecyclerView = findViewById(R.id.rates_rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MyRecyclerViewAdapter(mRates, mCountries);
        mRecyclerView.setAdapter(mAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {

                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
//                Log.d(TAG, "onMove, from:" + fromPosition + ", to:" + toPosition);
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(mCountries, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(mCountries, i, i - 1);
                    }
                }
                mAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void initCountryListFromCache() {
        String countryStr = mSharedPrefs.getString(KEY_COUNTRIES, null);
        if (countryStr != null) {
            String[] countries = countryStr.split(":");
            mCountries.addAll(Arrays.asList(countries));
        }
    }

    private void cacheCountryList() {
        StringBuilder countryStrBuilder = new StringBuilder();
        for (String name : mCountries) {
            countryStrBuilder.append(name).append(":");
        }
        countryStrBuilder.deleteCharAt(countryStrBuilder.length() - 1);
        String s = countryStrBuilder.toString();
        Log.d(TAG, "cacheCountryList, " + s);
        mSharedPrefs.edit().putString(KEY_COUNTRIES, s).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestNewRate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_update) {
            requestNewRate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestNewRate() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.fixer.io/latest");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        try {
                            while ((line = br.readLine()) != null) {
                                stringBuilder.append(line);
                            }
                            Message msg = mHandler.obtainMessage(MSG_UPDATE_SUCCESS, stringBuilder.toString());
                            mHandler.sendMessage(msg);
                        } catch (IOException e) {
                            mHandler.sendEmptyMessage(MSG_UPDATE_FAILED);
                        } finally {
                            is.close();
                        }
                    } else {
                        mHandler.sendEmptyMessage(MSG_UPDATE_FAILED);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_UPDATE_FAILED);
                } finally {

                }
            }
        });
        t.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cacheCountryList();
    }


}
