package kesira.papertrader;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class StockInfoActivity extends AppCompatActivity {
    private final Portfolio portfolio = Portfolio.getInstance();
    private String ticker;
    private BigDecimal stockPrice;
    private BigDecimal stockChange;
    private BigDecimal stockPercentChange;
    private static final long MILLION = 1000000L;
    private static final long BILLION = 1000000000L;
    private static final long TRILLION = 1000000000000L;
    private CustomLineChart chart;
    private CustomMarker marker;
    private XAxis xAxis;
    private YAxis yAxis;
    private final HashMap<Integer, ChartSetting> chartSettings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        ticker = getIntent().getStringExtra("ticker");
        getTickerDetails();
        stockPrice = portfolio.getQuote(ticker);
        if (stockPrice != null) {
            ((TextView) findViewById(R.id.stockPrice)).setText(portfolio.formatCurrency(stockPrice));
        }
        stockChange = portfolio.getChange(ticker);
        stockPercentChange = portfolio.getPercentChange(ticker);
        setChange(stockChange, stockPercentChange);

        chart = findViewById(R.id.chart);
        chart.setNoDataText("Loading...");
        chart.setDragYEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setOnTouchListener((v, event) -> {
            v.performClick();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                chart.highlightValues(null);
            }
            return super.onTouchEvent(event);
        });
        marker = new CustomMarker(this);
        marker.setChartView(chart);
        chart.setMarker(marker);
        Description description = new Description();
        description.setText("MPAndroidChart by Philipp Jahoda");
        chart.setDescription(description);
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(4, false);
        yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> getChartData(checkedId));
        getChartData(R.id.radio1D);

        findViewById(R.id.buy).setOnClickListener(v -> showTradeDialogFragment(true));
        findViewById(R.id.sell).setOnClickListener(v -> showTradeDialogFragment(false));

        updatePosition();
    }

    private void getTickerDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String result = APIHelper.get("https://api.polygon.io/v3/reference/tickers/" + ticker + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
            try {
                JSONObject jsonObject = new JSONObject(result).getJSONObject("results");
                String name = jsonObject.getString("name");
                String exchange = jsonObject.getString("primary_exchange").replace("XNYS", "NYSE").replace("XNAS", "NASDAQ");
                String marketCap = createMarketCapString(jsonObject.getDouble("market_cap"));
                String description = jsonObject.getString("description");
                new Handler(Looper.getMainLooper()).post(() -> {
                    ((TextView) findViewById(R.id.stockName)).setText(name);
                    ((TextView) findViewById(R.id.exchange)).setText(exchange);
                    ((TextView) findViewById(R.id.marketCap)).setText(marketCap);
                    ((TextView) findViewById(R.id.description)).setText(description);
                });
            } catch (JSONException e) {
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

    private void setChange(BigDecimal change, BigDecimal percentChange) {
        if (change != null && percentChange != null) {
            TextView changeText = findViewById(R.id.stockChange);
            boolean changePositive = portfolio.isPositive(change);
            changeText.setText((changePositive ? "+" : "") + portfolio.formatCurrency(change) + (changePositive ? " (+" : " (") + portfolio.formatPercentage(percentChange) + ")");
            changeText.setTextColor(changePositive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    private void getChartData(int checkedId) {
        ChartSetting chartSetting = chartSettings.get(checkedId);
        if (chartSetting == null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                String url = "https://api.polygon.io/v2/aggs/ticker/" + ticker + "/range/";
                boolean radio1D = checkedId == R.id.radio1D;
                if (radio1D) {
                    url += "5/minute/" + APIHelper.getToday();
                } else if (checkedId == R.id.radio1W) {
                    url += "30/minute/" + APIHelper.getRangeStart(Calendar.WEEK_OF_MONTH, 1);
                } else if (checkedId == R.id.radio1M) {
                    url += "1/day/" + APIHelper.getRangeStart(Calendar.MONTH, 1);
                } else if (checkedId == R.id.radio3M) {
                    url += "1/day/" + APIHelper.getRangeStart(Calendar.MONTH, 3);
                } else if (checkedId == R.id.radio1Y) {
                    url += "1/day/" + APIHelper.getRangeStart(Calendar.YEAR, 1);
                } else if (checkedId == R.id.radio2Y) {
                    url += "1/day/" + APIHelper.getRangeStart(Calendar.YEAR, 2);
                }
                url += "/" + APIHelper.getToday() + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up";
                String result = APIHelper.get(url);
                try {
                    JSONArray jsonArray = new JSONObject(result).getJSONArray("results");
                    ArrayList<Entry> entries = new ArrayList<>();
                    ArrayList<String> xAxisValues = new ArrayList<>();
                    ArrayList<String> markerDates = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
                    SimpleDateFormat xAxisFormat = new SimpleDateFormat(radio1D ? "h:mm a" : (checkedId == R.id.radio1W || checkedId == R.id.radio1M ? "MMM d" : "MMM yyyy"), Locale.ENGLISH);
                    xAxisFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    SimpleDateFormat markerDateFormat = new SimpleDateFormat(radio1D ? "h:mm a" : (checkedId == R.id.radio1W ? "EEE, MMM d h:mm a" : (checkedId == R.id.radio1M ? "EEE, MMM d" : "MMM d, yyyy")), Locale.ENGLISH);
                    markerDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    ChartSetting setting;
                    int numResults = jsonArray.length();
                    if (radio1D) {
                        ArrayList<Entry> premarketEntries = new ArrayList<>();
                        ArrayList<Entry> afterHoursEntries = new ArrayList<>();
                        int skipped = 0;
                        for (int i = 0; i < numResults; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Entry entry = new Entry(i - skipped, Float.parseFloat(jsonObject.getString("c")));
                            calendar.setTimeInMillis(jsonObject.getLong("t"));
                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            if (hour < 8) {
                                skipped++;
                                continue;
                            }
                            int minute = calendar.get(Calendar.MINUTE);
                            if (hour < 9 || (hour == 9 && minute <= 30)) {
                                premarketEntries.add(entry);
                                if (hour == 9 && minute == 30) {
                                    entries.add(entry);
                                }
                            } else if (hour >= 16) {
                                afterHoursEntries.add(entry);
                                if (hour == 16 && minute == 0) {
                                    entries.add(entry);
                                }
                            } else {
                                entries.add(entry);
                            }
                            Date date = calendar.getTime();
                            xAxisValues.add(xAxisFormat.format(date));
                            markerDates.add(markerDateFormat.format(date));
                        }
                        int color = portfolio.isPositive(stockChange) ? Color.parseColor("#33CC33") : Color.RED;
                        LineDataSet premarketDataSet = new LineDataSet(premarketEntries, "Premarket");
                        premarketDataSet.setHighlightEnabled(false);
                        premarketDataSet.setDrawValues(false);
                        premarketDataSet.setDrawCircles(false);
                        premarketDataSet.setColor(color);
                        LineDataSet afterHoursDataSet = new LineDataSet(afterHoursEntries, "After Hours");
                        afterHoursDataSet.setHighlightEnabled(false);
                        afterHoursDataSet.setDrawValues(false);
                        afterHoursDataSet.setDrawCircles(false);
                        afterHoursDataSet.setColor(color);
                        LineDataSet lineDataSet = new LineDataSet(entries, "Quotes");
                        lineDataSet.setDrawHorizontalHighlightIndicator(false);
                        lineDataSet.setDrawValues(false);
                        lineDataSet.setDrawCircles(false);
                        lineDataSet.setLineWidth(2);
                        lineDataSet.setColor(color);
                        LineData lineData = new LineData(premarketDataSet, lineDataSet, afterHoursDataSet);
                        setting = new ChartSetting(lineData, xAxisValues, markerDates, stockChange, stockPercentChange);
                        BigDecimal previousClose = portfolio.getPreviousClose(ticker);
                        if (previousClose != null) {
                            LimitLine previousCloseLimitLine = new LimitLine(previousClose.floatValue());
                            previousCloseLimitLine.setLineColor(Color.BLACK);
                            previousCloseLimitLine.setLineWidth(1);
                            previousCloseLimitLine.enableDashedLine(10, 10, 0);
                            previousCloseLimitLine.setLabel("Previous close " + portfolio.formatSimpleCurrency(previousClose));
                            setting.setPreviousCloseLimitLine(previousCloseLimitLine);
                        }
                    } else {
                        for (int i = 0; i < numResults; i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            entries.add(new Entry(i, Float.parseFloat(jsonObject.getString("c"))));
                            calendar.setTimeInMillis(jsonObject.getLong("t"));
                            Date date = calendar.getTime();
                            xAxisValues.add(xAxisFormat.format(date));
                            markerDates.add(markerDateFormat.format(date));
                        }
                        BigDecimal open = BigDecimal.valueOf(entries.get(0).getY());
                        BigDecimal change = BigDecimal.valueOf(entries.get(numResults - 1).getY()).subtract(open);
                        LineDataSet lineDataSet = new LineDataSet(entries, "Quotes");
                        lineDataSet.setDrawHorizontalHighlightIndicator(false);
                        lineDataSet.setDrawValues(false);
                        lineDataSet.setDrawCircles(false);
                        lineDataSet.setLineWidth(2);
                        lineDataSet.setColor(portfolio.isPositive(change) ? Color.parseColor("#33CC33") : Color.RED);
                        LineData lineData = new LineData(lineDataSet);
                        setting = new ChartSetting(lineData, xAxisValues, markerDates, portfolio.roundCurrency(change), portfolio.roundPercentage(portfolio.divide(change, open)));
                    }
                    chartSettings.put(checkedId, setting);
                    setChart(setting);
                    new Handler(Looper.getMainLooper()).post(() -> chart.animateX(500));
                } catch (JSONException e) {
                    Log.e("Exception", e.getMessage());
                }
            });
        } else {
            setChart(chartSetting);
            chart.animateX(500);
        }
    }

    private void setChart(ChartSetting chartSetting) {
        setChange(chartSetting.getChange(), chartSetting.getPercentChange());
        LineData lineData = chartSetting.getLineData();
        LimitLine previousCloseLimitLine = chartSetting.getPreviousCloseLimitLine();
        if (previousCloseLimitLine == null) {
            yAxis.removeAllLimitLines();
            yAxis.resetAxisMaximum();
            yAxis.resetAxisMinimum();
        } else {
            yAxis.addLimitLine(previousCloseLimitLine);
            float previousClose = previousCloseLimitLine.getLimit();
            float yMax = lineData.getYMax();
            float yMin = lineData.getYMin();
            if (previousClose >= yMax) {
                yAxis.setAxisMaximum(previousClose + 0.1f * (previousClose - yMin));
            } else if (previousClose <= yMin) {
                yAxis.setAxisMinimum(previousClose - 0.1f * (yMax - previousClose));
            }
        }
        chart.setData(lineData);
        ArrayList<String> xAxisValues = chartSetting.getXAxisValues();
        int numXAxisValues = xAxisValues.size();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < numXAxisValues) {
                    return xAxisValues.get(index);
                }
                return "";
            }
        });
        marker.setMarkerDates(chartSetting.getMarkerDates());
    }

    private void showTradeDialogFragment(boolean buy) {
        Bundle args = new Bundle();
        args.putString("ticker", ticker);
        args.putString("stockPrice", stockPrice.toPlainString());
        TradeDialogFragment tradeDialogFragment = new TradeDialogFragment(buy);
        tradeDialogFragment.setArguments(args);
        tradeDialogFragment.show(getSupportFragmentManager(), buy ? "buy" : "sell");
    }

    void updatePosition() {
        boolean inPositions = portfolio.inPositions(ticker);
        findViewById(R.id.sell).setEnabled(inPositions);
        findViewById(R.id.position).setVisibility(inPositions ? View.VISIBLE : View.GONE);
        if (inPositions) {
            BigDecimal shares = new BigDecimal(portfolio.getShares(ticker));
            ((TextView) findViewById(R.id.shares)).setText(shares.toPlainString());
            BigDecimal totalValue = portfolio.roundCurrency(shares.multiply(stockPrice));
            ((TextView) findViewById(R.id.totalValue)).setText(portfolio.formatCurrency(totalValue));
            ((TextView) findViewById(R.id.percentageOfPortfolio)).setText(portfolio.isPortfolioValueReady() ? portfolio.createPercentage(totalValue, portfolio.getPortfolioValue()) : "");
            BigDecimal averageCost = portfolio.getCost(ticker);
            ((TextView) findViewById(R.id.averageCost)).setText(portfolio.formatCurrency(averageCost));
            BigDecimal priceChange = stockPrice.subtract(averageCost);
            boolean priceChangePositive = portfolio.isPositive(priceChange);
            TextView performanceText = findViewById(R.id.performance);
            performanceText.setText((priceChangePositive ? "+" : "") + portfolio.formatCurrency(portfolio.roundCurrency(shares.multiply(priceChange))) + (priceChangePositive ? " (+" : " (") + portfolio.createPercentage(priceChange, averageCost) + ")");
            performanceText.setTextColor(priceChangePositive ? Color.parseColor("#33CC33") : Color.RED);
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!portfolio.inPositions(ticker)) {
            getMenuInflater().inflate(portfolio.inWatchlist(ticker) ? R.menu.remove_watchlist_menu : R.menu.add_watchlist_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add) {
            portfolio.add(ticker);
            invalidateOptionsMenu();
            Toast.makeText(this, "Stock added to watchlist", Toast.LENGTH_LONG).show();
        } else if (itemId == R.id.remove) {
            portfolio.remove(ticker);
            invalidateOptionsMenu();
            Toast.makeText(this, "Stock removed from watchlist", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }
}