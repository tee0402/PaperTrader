package kesira.papertrader;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockInfoActivity extends AppCompatActivity {
    private String ticker;
    private BigDecimal stockPrice;
    private static final long MILLION = 1000000L;
    private static final long BILLION = 1000000000L;
    private static final long TRILLION = 1000000000000L;
    private CustomLineChart chart;
    private XAxis xAxis;
    private YAxis leftAxis;
    private LimitLine limitLine;
    private LineDataSet dataSet;
    private float prevClose;
    private int taskCounter = 0;
    private boolean done = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        ticker = getIntent().getStringExtra("ticker");
        stockPrice = Portfolio.getQuote(ticker);
        if (stockPrice != null) {
            ((TextView) findViewById(R.id.stockPrice)).setText(Portfolio.formatCurrency(stockPrice));
        }
        BigDecimal percentChange = Portfolio.getPercentChange(ticker);
        if (percentChange != null) {
            boolean percentChangePositive = percentChange.compareTo(BigDecimal.ZERO) >= 0;
            TextView percentChangeText = findViewById(R.id.stockPercentChange);
            percentChangeText.setText((percentChangePositive ? "+" : "") + Portfolio.formatPercentage(percentChange));
            percentChangeText.setTextColor(percentChangePositive ? Color.parseColor("#33CC33") : Color.RED);
        }

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

        getTickerDetails();

        findViewById(R.id.buy).setOnClickListener(v -> showTradeDialogFragment(true));
        findViewById(R.id.sell).setOnClickListener(v -> showTradeDialogFragment(false));

        updatePosition();

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.check(R.id.radio1D);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio1D) {
                leftAxis.addLimitLine(limitLine);
            } else {
                leftAxis.removeLimitLine(limitLine);
            }
//            getBars(checkedId);
        });
    }

    private void getTickerDetails() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                URL u = new URL("https://api.polygon.io/v3/reference/tickers/" + ticker + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) u.openConnection();
                } while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stockInfo = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stockInfo.append(line).append("\n");
                    }
                    bufferedReader.close();
                    try {
                        JSONObject jsonObject = new JSONObject(stockInfo.toString()).getJSONObject("results");
                        String name = jsonObject.getString("name");
                        String exchange = jsonObject.getString("primary_exchange").replace("XNYS", "NYSE").replace("XNAS", "NASDAQ");
                        String marketCap = createMarketCapString(jsonObject.getDouble("market_cap"));
                        handler.post(() -> {
                            ((TextView) findViewById(R.id.stockName)).setText(name);
                            ((TextView) findViewById(R.id.exchange)).setText(exchange);
                            ((TextView) findViewById(R.id.marketCap)).setText(marketCap);
                        });
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("Exception", e.getMessage());
            }
        });
    }

    private String createMarketCapString(double num) {
        DecimalFormat marketCapFormat = new DecimalFormat("$#.##");
        if (num < MILLION) {
            return marketCapFormat.format(num);
        } else if (num < BILLION) {
            return marketCapFormat.format(num / MILLION) + "M";
        } else if (num < TRILLION) {
            return marketCapFormat.format(num / BILLION) + "B";
        } else {
            return marketCapFormat.format(num / TRILLION) + "T";
        }
    }

    private void showTradeDialogFragment(boolean buy) {
        DialogFragment tradeDialogFragment = new TradeDialogFragment(buy);
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        args.putString("stockPrice", stockPrice.toPlainString());
        tradeDialogFragment.setArguments(args);
        tradeDialogFragment.show(getSupportFragmentManager(), buy ? "buy" : "sell");
    }

    void updatePosition() {
        boolean inPositions = Portfolio.inPositions(ticker);
        findViewById(R.id.sell).setEnabled(inPositions);
        findViewById(R.id.position).setVisibility(inPositions ? View.VISIBLE : View.GONE);
        if (inPositions) {
            BigDecimal shares = new BigDecimal(Portfolio.getShares(ticker));
            ((TextView) findViewById(R.id.sharesOwned)).setText(shares.toPlainString());
            BigDecimal positionValue = Portfolio.roundCurrency(shares.multiply(stockPrice));
            ((TextView) findViewById(R.id.positionValue)).setText(Portfolio.formatCurrency(positionValue));
            ((TextView) findViewById(R.id.percentageOfPortfolio)).setText(Portfolio.valueReady() ? Portfolio.createPercentage(positionValue, Portfolio.getValue()) : "");
            BigDecimal costBasis = Portfolio.getCost(ticker);
            ((TextView) findViewById(R.id.costBasis)).setText(Portfolio.formatCurrency(costBasis));
            BigDecimal priceChange = stockPrice.subtract(costBasis);
            boolean priceChangePositive = priceChange.compareTo(BigDecimal.ZERO) >= 0;
            TextView positionPerformanceText = (TextView) findViewById(R.id.positionPerformance);
            positionPerformanceText.setText((priceChangePositive ? "+" : "") + Portfolio.formatCurrency(Portfolio.roundCurrency(shares.multiply(priceChange))) + (priceChangePositive ? " (+" : " (") + Portfolio.createPercentage(priceChange, costBasis) + ")");
            positionPerformanceText.setTextColor(priceChangePositive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh_menu, menu);
        inflater.inflate(Portfolio.inWatchlist(ticker) ? R.menu.remove_watchlist_menu : R.menu.add_watchlist_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.refresh) {
            finish();
            startActivity(getIntent());
        } else if (itemId == R.id.add) {
            Portfolio.add(ticker);
            invalidateOptionsMenu();
            Toast.makeText(this, "Stock added to watchlist", Toast.LENGTH_LONG).show();
        } else if (itemId == R.id.remove) {
            Portfolio.remove(ticker);
            invalidateOptionsMenu();
            Toast.makeText(this, "Stock removed from watchlist", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getBars(int checkedId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String url = "https://api.polygon.io/v2/aggs/ticker/" + ticker + "/range/";
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int date = calendar.get(Calendar.DATE);
            calendar.set(year, month, date);
            if (checkedId == R.id.radio1D) {
                url += "5/minute/2022-02-08/2022-02-08";
            } else if (checkedId == R.id.radio1W) {
                url += "30/minute/2022-02-02/2022-02-08";
            } else if (checkedId == R.id.radio1M) {
                url += "1/day/2022-01-09/2022-02-08";
            } else if (checkedId == R.id.radio3M) {
                url += "1/day/2021-11-09/2022-02-08";
            } else if (checkedId == R.id.radio1Y) {
                url += "1/day/2021-02-09/2022-02-08";
            } else if (checkedId == R.id.radio2Y) {
                url += "1/week/2020-02-09/2022-02-08";
            }
            url += "?adjusted=true&sort=asc&limit=120&apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up";
            try {
                URL u = new URL(url);
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) u.openConnection();
                } while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stockInfo = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stockInfo.append(line).append("\n");
                    }
                    bufferedReader.close();
                    try {
                        JSONObject jsonObject = new JSONObject(stockInfo.toString()).getJSONObject("results");
                        String name = jsonObject.getString("name");
                        String exchange = jsonObject.getString("primary_exchange").replace("XNYS", "NYSE").replace("XNAS", "NASDAQ");
                        String marketCap = createMarketCapString(jsonObject.getDouble("market_cap"));
                        handler.post(() -> {
                            ((TextView) findViewById(R.id.stockName)).setText(name);
                            ((TextView) findViewById(R.id.exchange)).setText(exchange);
                            ((TextView) findViewById(R.id.marketCap)).setText(marketCap);
                        });
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("Exception", e.getMessage());
            }
        });
    }

    private void getBars() {
        taskCounter++;
        new Thread(() -> {
            try {
                URL u = new URL("");
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) u.openConnection();
                } while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stockInfo = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stockInfo.append(line).append("\n");
                    }
                    bufferedReader.close();
                    try {
                        String result = stockInfo.toString().replaceAll("// ", "");
                        Object json = new JSONTokener(result).nextValue();
                        if (json instanceof JSONObject) {
                            JSONObject jsonObject = new JSONObject(result);
                            if (!jsonObject.isNull("Positions")) {
                                final JSONArray dates = jsonObject.getJSONArray("Dates");
                                SimpleDateFormat datesFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                                SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMM d", Locale.ENGLISH);
                                SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);
                                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.ENGLISH);
                                if (dates.length() < 100) {
                                    for (int i = 0; i < dates.length(); i++) {
                                        dates.put(i, monthDayFormat.format(Objects.requireNonNull(datesFormat.parse(dates.get(i).toString()))));
                                    }
                                } else if (dates.length() >= 100 && dates.length() < 365) {
                                    for (int i = 0; i < dates.length(); i++) {
                                        dates.put(i, monthYearFormat.format(Objects.requireNonNull(datesFormat.parse(dates.get(i).toString()))));
                                    }
                                } else {
                                    for (int i = 0; i < dates.length(); i++) {
                                        dates.put(i, yearFormat.format(Objects.requireNonNull(datesFormat.parse(dates.get(i).toString()))));
                                    }
                                }
                                xAxis.setValueFormatter(new ValueFormatter() {
                                    @Override
                                    public String getFormattedValue(float value) {
                                        try {
                                            return dates.get((int) value).toString();
                                        } catch (JSONException e) {
                                            Log.e("Exception", e.getMessage());
                                        }
                                        return null;
                                    }
                                });
                                xAxis.resetAxisMaximum();
                                leftAxis.resetAxisMaximum();
                                leftAxis.resetAxisMinimum();

                                JSONArray values = jsonObject.getJSONArray("Elements").getJSONObject(0).getJSONObject("DataSeries").getJSONObject("close").getJSONArray("values");
                                ArrayList<Entry> entries = new ArrayList<>();
                                for (int i = 0; i < dates.length(); i++) {
                                    entries.add(new Entry(i, Float.parseFloat(String.valueOf(values.get(i)))));
                                }
                                LineDataSet dataSet = new LineDataSet(entries, "Label");
                                dataSet.setDrawHorizontalHighlightIndicator(false);
                                dataSet.setDrawCircles(false);
                                dataSet.setLineWidth(2);
                                if (entries.size() >= 2) {
                                    if (entries.get(0).getY() <= entries.get(entries.size() - 1).getY()) {
                                        dataSet.setColor(Color.parseColor("#33CC33"));
                                    } else {
                                        dataSet.setColor(Color.RED);
                                    }
                                }
                                LineData lineData = new LineData(dataSet);
                                lineData.setDrawValues(false);
                                chart.setData(lineData);
                                chart.animateX(1000);
                                chart.invalidate();
                            }
                        } else if (json instanceof JSONArray) {
                            JSONObject jsonObject = new JSONArray(result).getJSONObject(0);
                            prevClose = Float.parseFloat(jsonObject.getString("pcls_fix"));
                            limitLine = new LimitLine(Float.parseFloat(jsonObject.getString("pcls_fix")));
                            limitLine.setLineColor(Color.parseColor("#3F51B5"));
                            limitLine.setLineWidth(1);
                            limitLine.enableDashedLine(30, 30, 0);
                            leftAxis.addLimitLine(limitLine);
                        } else {
                            final ArrayList<String> dates = new ArrayList<>();
                            SimpleDateFormat minFormat = new SimpleDateFormat("mmm", Locale.ENGLISH);
                            SimpleDateFormat hourMinFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
                            for (int i = 570; i <= 960; i += 5) {
                                dates.add(hourMinFormat.format(Objects.requireNonNull(minFormat.parse("" + i))));
                            }
                            xAxis.setValueFormatter(new ValueFormatter() {
                                @Override
                                public String getFormattedValue(float value) {
                                    return dates.get((int) value);
                                }
                            });

                            bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result.getBytes())));
                            ArrayList<Entry> entries = new ArrayList<>();
                            for (int i = 0; i < 7; i++) {
                                bufferedReader.readLine();
                            }
                            line = bufferedReader.readLine();
                            String[] array = line.split(",");
                            entries.add(new Entry(0, Float.parseFloat(array[1])));
                            while ((line = bufferedReader.readLine()) != null) {
                                array = line.split(",");
                                entries.add(new Entry(Integer.parseInt(array[0]), Float.parseFloat(array[1])));
                            }
                            bufferedReader.close();
                            xAxis.setAxisMaximum(78);

                            dataSet = new LineDataSet(entries, "Label");
                            dataSet.setDrawHorizontalHighlightIndicator(false);
                            dataSet.setDrawCircles(false);
                            dataSet.setLineWidth(2);

                            done = false;
                        }
                    } catch (IOException | JSONException | ParseException e) {
                        Log.e("Exception", e.getMessage());
                    }
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("Exception", e.toString());
            }
            taskCounter--;
            if (!done && taskCounter == 0 && dataSet != null) {
                done = true;
                dataSet.setColor(((TextView) findViewById(R.id.stockPercentChange)).getCurrentTextColor());
                LineData lineData = new LineData(dataSet);
                lineData.setDrawValues(false);
                if (prevClose >= lineData.getYMax()) {
                    leftAxis.setAxisMaximum(prevClose + 0.1f * (prevClose - lineData.getYMin()));
                } else if (prevClose <= lineData.getYMin()) {
                    leftAxis.setAxisMinimum(prevClose - 0.1f * (lineData.getYMax() - prevClose));
                }
                chart.setData(lineData);
                chart.animateX(1000);
                chart.invalidate();
            }
        }).start();
    }
}