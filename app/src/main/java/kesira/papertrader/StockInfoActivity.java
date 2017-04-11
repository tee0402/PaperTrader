package kesira.papertrader;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StockInfoActivity extends AppCompatActivity {

    private String ticker;
    private static final long MILLION = 1000000L;
    private static final long BILLION = 1000000000L;
    private LineChart chart;
    private XAxis xAxis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        chart = (LineChart) findViewById(R.id.chart);
        chart.setNoDataText("Loading...");
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        Description description = new Description();
        description.setText("MPAndroidChart by Philipp Jahoda");
        chart.setDescription(description);

        Intent intent = getIntent();
        ticker = intent.getStringExtra("ticker");
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Quote/json?symbol=" + ticker);
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A365%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radio1Y);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                switch (i) {
                    case R.id.radio1W:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A7%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio1M:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A30%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio3M:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A90%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio1Y:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A365%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio5Y:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A1825%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radioMax:
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A36500%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                }
            }
        });

        TextView stockPrice = (TextView) findViewById(R.id.stockPrice);
        stockPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                findViewById(R.id.buy).setEnabled(true);
                findViewById(R.id.sell).setEnabled(true);
            }
        });
    }

    public void buy(View view) {
        DialogFragment newFragment = new BuyDialogFragment();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        newFragment.setArguments(args);
        newFragment.show(getSupportFragmentManager(), "buy");
    }

    public void sell(View view) {
        DialogFragment newFragment = new SellDialogFragment();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        newFragment.setArguments(args);
        newFragment.show(getSupportFragmentManager(), "sell");
    }

    private String convertBigNumber(double num, int decimalPlaces) {
        double scale = 10;
        if (num < MILLION) {
            return new DecimalFormat("#,###").format(num);
        }
        if (num >= MILLION && num < BILLION) {
            return Math.round((num / MILLION) * Math.pow(scale, decimalPlaces)) / Math.pow(scale, decimalPlaces) + "M";
        }
        else{
            return Math.round((num / BILLION) * Math.pow(scale, decimalPlaces)) / Math.pow(scale, decimalPlaces) + "B";
        }
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) url.openConnection();
                }
                while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String stockInfo = "";
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stockInfo += line + "\n";
                    }
                    bufferedReader.close();
                    return stockInfo.replaceAll("// ", "");
                }
                finally {
                    urlConnection.disconnect();
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                result = "Error getting stock info";
            }
            Log.i("INFO", result);
            try {
                Object json = new JSONTokener(result).nextValue();
                if (json instanceof JSONObject) {
                    JSONObject jsonObject = new JSONObject(result);
                    if (!jsonObject.isNull("Positions")) {
                        final JSONArray dates = jsonObject.getJSONArray("Dates");
                        SimpleDateFormat datesFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMM d", Locale.ENGLISH);
                        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM YYYY", Locale.ENGLISH);
                        SimpleDateFormat yearFormat = new SimpleDateFormat("YYYY", Locale.ENGLISH);
                        if (dates.length() < 100) {
                            for (int i = 0; i < dates.length(); i++) {
                                Date date = datesFormat.parse(dates.get(i).toString());
                                dates.put(i, monthDayFormat.format(date));
                            }
                        }
                        else if (dates.length() >= 100 && dates.length() < 365) {
                            for (int i = 0; i < dates.length(); i++) {
                                Date date = datesFormat.parse(dates.get(i).toString());
                                dates.put(i, monthYearFormat.format(date));
                            }
                        }
                        else {
                            for (int i = 0; i < dates.length(); i++) {
                                Date date = datesFormat.parse(dates.get(i).toString());
                                dates.put(i, yearFormat.format(date));
                            }
                        }
                        IAxisValueFormatter formatter = new IAxisValueFormatter() {
                            @Override
                            public String getFormattedValue(float value, AxisBase axis) {
                                try {
                                    return dates.get((int) value).toString();
                                }
                                catch (Exception e) {
                                    Log.e("Exception", e.getMessage());
                                }
                                return null;
                            }
                        };
                        xAxis.setValueFormatter(formatter);
                        JSONArray values = jsonObject.getJSONArray("Elements").getJSONObject(0).getJSONObject("DataSeries").getJSONObject("close").getJSONArray("values");
                        ArrayList<Entry> entries = new ArrayList<>();
                        for (int i = 0; i < dates.length(); i++) {
                            entries.add(new Entry(i, Float.valueOf(String.valueOf(values.get(i)))));
                        }
                        LineDataSet dataSet = new LineDataSet(entries, "Label");
                        dataSet.setDrawHorizontalHighlightIndicator(false);
                        dataSet.setDrawCircles(false);
                        dataSet.setLineWidth(2);
                        dataSet.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
                        dataSet.setValueTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                        LineData lineData = new LineData(dataSet);
                        lineData.setDrawValues(false);
                        chart.setData(lineData);
                        chart.animateX(1000);
                        chart.invalidate();
                    }
                    else {
                        ((TextView) findViewById(R.id.stockName)).setText(jsonObject.getString("Name"));
                        ((TextView) findViewById(R.id.stockMarketCap)).setText("$" + convertBigNumber(Double.valueOf(jsonObject.getString("MarketCap")), 2));
                    }
                }
                else if (json instanceof JSONArray) {
                    JSONArray jsonArray = new JSONArray(result);
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    if (!jsonObject.isNull("Exchange")) {
                        if (jsonObject.getString("Exchange").equals("NYSE") || jsonObject.getString("Exchange").equals("BATS Trading Inc")) {
                            new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NYSE%3A" + jsonObject.getString("Symbol"));
                        }
                        else if (jsonObject.getString("Exchange").equals("NASDAQ")) {
                            new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NASDAQ%3A" + jsonObject.getString("Symbol"));
                        }
                    }
                    else {
                        ((TextView) findViewById(R.id.stockPrice)).setText(jsonObject.getString("l"));
                        if (Float.valueOf(jsonObject.getString("cp")) >= 0) {
                            ((TextView) findViewById(R.id.stockPercentChange)).setText("+" + jsonObject.getString("cp") + "%");
                            ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.parseColor("#33CC33"));
                        }
                        else {
                            ((TextView) findViewById(R.id.stockPercentChange)).setText(String.format("%s%%", jsonObject.getString("cp")));
                            ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.RED);
                        }
                        ((TextView) findViewById(R.id.exchange)).setText(jsonObject.getString("e"));
                    }
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }
    }
}