package com.jiangtao.exchangerate;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jiangtao on 2018/3/1.
 */

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "MyRecyclerViewAdapter";
    private Map<String, Double> mRates;
    private Map<String, Double> mExchangeResults = new HashMap<>();
    private List<String> mCountries;

    private int mEditPos = -1;

    public MyRecyclerViewAdapter(Map<String, Double> rates, List<String> countries) {
        mRates = rates;
        mExchangeResults.putAll(mRates);
        mCountries = countries;
    }

    public void setData(Map<String, Double> rates) {
        mExchangeResults.clear();
        mExchangeResults.putAll(rates);
    }

    @Override
    public MyRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rate_list_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(v);

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditPos = -1;
                notifyDataSetChanged();
            }
        });

        viewHolder.rateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditPos = viewHolder.getLayoutPosition();
                notifyDataSetChanged();

            }
        });

        viewHolder.moneyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                double input = 0;
                if (s.length() > 0) {
                    input = Double.valueOf(s.toString());
                }
                int position = viewHolder.getLayoutPosition();
                String name = mCountries.get(position);
                Double rate = mRates.get(name);

                if (rate != null) {
                    for (String key : mExchangeResults.keySet()) {
                        double newNum = mRates.get(key) * (input / rate);
                        Log.d(TAG, "afterTextChanged, key:" + key + ", newNum:" + newNum);

                        mExchangeResults.put(key, newNum);
                    }
                    for (int i = 0; i < getItemCount(); i++) {
                        if (i != position) {
                            notifyItemChanged(i);
                        }
                    }
                }
            }
        });

        return viewHolder;

    }

    @Override
    public void onBindViewHolder(MyRecyclerViewAdapter.ViewHolder holder, int position) {

        holder.rateText.setVisibility(position == mEditPos ? View.GONE : View.VISIBLE);
        holder.moneyEdit.setVisibility(position == mEditPos ? View.VISIBLE : View.GONE);

        String country = mCountries.get(position);
        holder.countryText.setText(country);
        Double rate = mExchangeResults.get(country);

        DecimalFormat df = new DecimalFormat("#.######");
        holder.rateText.setText(rate == null ? "" : df.format(rate));

    }

    @Override
    public int getItemCount() {
        return mCountries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView countryText;
        TextView rateText;
        EditText moneyEdit;

        public ViewHolder(View itemView) {
            super(itemView);
            countryText = itemView.findViewById(R.id.country_text);
            rateText = itemView.findViewById(R.id.rate_text);
            moneyEdit = itemView.findViewById(R.id.money_edit);
        }
    }
}
