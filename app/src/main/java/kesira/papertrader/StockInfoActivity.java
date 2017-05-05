package kesira.papertrader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StockInfoActivity extends AppCompatActivity {

    private String ticker;
    private SharedPreferences prefs;
    private static final long MILLION = 1000000L;
    private static final long BILLION = 1000000000L;
    private CustomLineChart chart;
    private XAxis xAxis;
    private YAxis leftAxis;
    private LimitLine limitLine;
    private LineDataSet dataSet;
    private float prevClose;
    private float stockPrice;
    private int taskCounter = 0;
    private boolean done = false;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-4071292763824495~6413389765");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        chart = (CustomLineChart) findViewById(R.id.chart);
        chart.setNoDataText("Loading...");
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5, false);
        leftAxis = chart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        Description description = new Description();
        description.setText("MPAndroidChart by Philipp Jahoda");
        chart.setDescription(description);

        Intent intent = getIntent();
        ticker = intent.getStringExtra("ticker");

        prefs = getSharedPreferences("Save", Context.MODE_PRIVATE);
        if (prefs.getInt(ticker, 0) > 0) {
            findViewById(R.id.position).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.sharesOwned)).setText(String.valueOf(prefs.getInt(ticker, 0)));
            ((TextView) findViewById(R.id.costBasis)).setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat(ticker + "_cost", 0)));
        }

        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Quote/json?symbol=" + ticker);
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
        new RetrieveFeedTask().execute("https://www.google.com/finance/getprices?i=300&p=1d&f=d,c&q=" + ticker);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radio1D);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                switch (i) {
                    case R.id.radio1D:
                        leftAxis.addLimitLine(limitLine);
                        new RetrieveFeedTask().execute("https://www.google.com/finance/getprices?i=300&p=1d&f=d,c&q=" + ticker);
                        break;
                    case R.id.radio1W:
                        leftAxis.removeLimitLine(limitLine);
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A7%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio1M:
                        leftAxis.removeLimitLine(limitLine);
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A30%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio3M:
                        leftAxis.removeLimitLine(limitLine);
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A90%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio1Y:
                        leftAxis.removeLimitLine(limitLine);
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A365%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radio5Y:
                        leftAxis.removeLimitLine(limitLine);
                        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/InteractiveChart/json?parameters=%7B%22Normalized%22%3Afalse%2C%22NumberOfDays%22%3A1825%2C%22DataPeriod%22%3A%22Day%22%2C%22Elements%22%3A%5B%7B%22Symbol%22%3A%22" + ticker + "%22%2C%22Type%22%3A%22price%22%2C%22Params%22%3A%5B%22c%22%5D%7D%5D%7D");
                        break;
                    case R.id.radioMax:
                        leftAxis.removeLimitLine(limitLine);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh_menu, menu);

        Set<String> watchlistSet = prefs.getStringSet("watchlist", new HashSet<String>());
        if (prefs.getInt(ticker, 0) == 0) {
            inflater.inflate(R.menu.watchlist_menu, menu);
            if (!watchlistSet.contains(ticker)) {
                hideOption(R.id.remove);
            }
            else {
                hideOption(R.id.add);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            finish();
            startActivity(getIntent());
        }
        else if (item.getItemId() == R.id.add) {
            SharedPreferences.Editor editor = prefs.edit();
            Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
            watchlistSet.add(ticker);
            editor.putStringSet("watchlist", watchlistSet);
            editor.apply();
            showOption(R.id.remove);
            hideOption(R.id.add);
            Toast.makeText(this, "Stock added to watchlist", Toast.LENGTH_LONG).show();
        }
        else if (item.getItemId() == R.id.remove) {
            SharedPreferences.Editor editor = prefs.edit();
            Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
            watchlistSet.remove(ticker);
            editor.putStringSet("watchlist", watchlistSet);
            editor.apply();
            showOption(R.id.add);
            hideOption(R.id.remove);
            Toast.makeText(this, "Stock removed from watchlist", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void hideOption(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(false);
    }

    private void showOption(int id) {
        MenuItem item = menu.findItem(id);
        item.setVisible(true);
    }

    public void buy(View view) {
        DialogFragment newFragment = new BuyDialogFragment();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        args.putFloat("stockPrice", stockPrice);
        newFragment.setArguments(args);
        newFragment.show(getSupportFragmentManager(), "buy");
    }

    public void sell(View view) {
        DialogFragment newFragment = new SellDialogFragment();
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        args.putFloat("stockPrice", stockPrice);
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
        protected void onPreExecute() {
            taskCounter++;
        }

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
                        xAxis.resetAxisMaximum();
                        leftAxis.resetAxisMaximum();
                        leftAxis.resetAxisMinimum();

                        JSONArray values = jsonObject.getJSONArray("Elements").getJSONObject(0).getJSONObject("DataSeries").getJSONObject("close").getJSONArray("values");
                        ArrayList<Entry> entries = new ArrayList<>();
                        for (int i = 0; i < dates.length(); i++) {
                            entries.add(new Entry(i, Float.valueOf(String.valueOf(values.get(i)))));
                        }
                        LineDataSet dataSet = new LineDataSet(entries, "Label");
                        dataSet.setDrawHorizontalHighlightIndicator(false);
                        dataSet.setDrawCircles(false);
                        dataSet.setLineWidth(2);
                        if (entries.size() >= 2) {
                            if (entries.get(0).getY() <= entries.get(entries.size() - 1).getY()) {
                                dataSet.setColor(Color.parseColor("#33CC33"));
                            }
                            else {
                                dataSet.setColor(Color.RED);
                            }
                        }
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
                        stockPrice = Float.valueOf(jsonObject.getString("l_fix"));
                        float costBasis = prefs.getFloat(ticker + "_cost", 0);
                        int sharesOwned = prefs.getInt(ticker, 0);
                        ((TextView) findViewById(R.id.positionValue)).setText(NumberFormat.getCurrencyInstance().format(sharesOwned * stockPrice));
                        ((TextView) findViewById(R.id.percentageOfPortfolio)).setText(new DecimalFormat("0.00").format(sharesOwned * stockPrice / prefs.getFloat("portfolioValue", 0) * 100) + "%");
                        if (stockPrice - costBasis >= 0) {
                            ((TextView) findViewById(R.id.positionPerformance)).setText("+" + NumberFormat.getCurrencyInstance().format(sharesOwned * (stockPrice - costBasis)) + " (+" + new DecimalFormat("0.00").format((stockPrice - costBasis) / costBasis * 100) + "%)");
                            ((TextView) findViewById(R.id.positionPerformance)).setTextColor(Color.parseColor("#33CC33"));
                        }
                        else {
                            ((TextView) findViewById(R.id.positionPerformance)).setText(NumberFormat.getCurrencyInstance().format(sharesOwned * (stockPrice - costBasis)) + " (" + new DecimalFormat("0.00").format((stockPrice - costBasis) / costBasis * 100) + "%)");
                            ((TextView) findViewById(R.id.positionPerformance)).setTextColor(Color.RED);
                        }
                        ((TextView) findViewById(R.id.stockPrice)).setText("$" + jsonObject.getString("l"));
                        if (Float.valueOf(jsonObject.getString("cp")) >= 0) {
                            ((TextView) findViewById(R.id.stockPercentChange)).setText("+" + jsonObject.getString("cp") + "%");
                            ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.parseColor("#33CC33"));
                        }
                        else {
                            ((TextView) findViewById(R.id.stockPercentChange)).setText(String.format("%s%%", jsonObject.getString("cp")));
                            ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.RED);
                        }
                        ((TextView) findViewById(R.id.exchange)).setText(jsonObject.getString("e"));
                        if (!jsonObject.isNull("yld")) {
                            findViewById(R.id.dividendYield).setVisibility(View.VISIBLE);
                            findViewById(R.id.dividendYieldText).setVisibility(View.VISIBLE);
                            if (jsonObject.getString("yld").equals("")) {
                                ((TextView) findViewById(R.id.dividendYield)).setText("N/A");
                            }
                            else {
                                ((TextView) findViewById(R.id.dividendYield)).setText(jsonObject.getString("yld") + "%");
                            }
                        }
                        if (!jsonObject.isNull("ecp") && !jsonObject.getString("ecp").equals("0.00")) {
                            findViewById(R.id.extendedHours).setVisibility(View.VISIBLE);
                            findViewById(R.id.extendedHoursChange).setVisibility(View.VISIBLE);
                            if (Float.valueOf(jsonObject.getString("ecp")) > 0) {
                                ((TextView) findViewById(R.id.extendedHours)).setText("Extended Hours: $" + jsonObject.getString("el") + " ");
                                ((TextView) findViewById(R.id.extendedHoursChange)).setText(jsonObject.getString("ec") + " (+" + jsonObject.getString("ecp") + "%)");
                                ((TextView) findViewById(R.id.extendedHoursChange)).setTextColor(Color.parseColor("#33CC33"));
                            }
                            else {
                                ((TextView) findViewById(R.id.extendedHours)).setText("Extended Hours: " + jsonObject.getString("el") + " ");
                                ((TextView) findViewById(R.id.extendedHoursChange)).setText(jsonObject.getString("ec") + " (" + jsonObject.getString("ecp") + "%)");
                                ((TextView) findViewById(R.id.extendedHoursChange)).setTextColor(Color.RED);
                            }
                        }
                        prevClose = Float.valueOf(jsonObject.getString("pcls_fix"));
                        limitLine = new LimitLine(Float.valueOf(jsonObject.getString("pcls_fix")));
                        limitLine.setLineColor(Color.parseColor("#3F51B5"));
                        limitLine.setLineWidth(1);
                        limitLine.enableDashedLine(30, 30, 0);
                        leftAxis.addLimitLine(limitLine);
                    }
                }
                else {
                    final ArrayList<String> dates = new ArrayList<>();
                    SimpleDateFormat minFormat = new SimpleDateFormat("mmm", Locale.ENGLISH);
                    SimpleDateFormat hourMinFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
                    for (int i = 570; i <= 960; i += 5) {
                        Date date = minFormat.parse("" + i);
                        dates.add(hourMinFormat.format(date));
                    }
                    IAxisValueFormatter formatter = new IAxisValueFormatter() {
                        @Override
                        public String getFormattedValue(float value, AxisBase axis) {
                            try {
                                return dates.get((int) value);
                            }
                            catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                            }
                            return null;
                        }
                    };
                    xAxis.setValueFormatter(formatter);

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes())));
                    ArrayList<Entry> entries = new ArrayList<>();
                    String line;
                    for (int i = 0; i < 7; i++) {
                        bufferedReader.readLine();
                    }
                    line = bufferedReader.readLine();
                    String[] array = line.split(",");
                    entries.add(new Entry(0, Float.valueOf(array[1])));
                    while ((line = bufferedReader.readLine()) != null) {
                        array = line.split(",");
                        entries.add(new Entry(Integer.valueOf(array[0]), Float.valueOf(array[1])));
                    }
                    bufferedReader.close();
                    xAxis.setAxisMaximum(78);

                    dataSet = new LineDataSet(entries, "Label");
                    dataSet.setDrawHorizontalHighlightIndicator(false);
                    dataSet.setDrawCircles(false);
                    dataSet.setLineWidth(2);

                    done = false;
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            taskCounter--;
            if (!done && taskCounter == 0) {
                done = true;
                dataSet.setColor(((TextView) findViewById(R.id.stockPercentChange)).getCurrentTextColor());
                LineData lineData = new LineData(dataSet);
                lineData.setDrawValues(false);
                if (prevClose >= lineData.getYMax()) {
                    leftAxis.setAxisMaximum(prevClose + 0.1f * (prevClose - lineData.getYMin()));
                }
                else if (prevClose <= lineData.getYMin()) {
                    leftAxis.setAxisMinimum(prevClose - 0.1f * (lineData.getYMax() - prevClose));
                }
                chart.setData(lineData);
                chart.animateX(1000);
                chart.invalidate();
            }
        }
    }
}