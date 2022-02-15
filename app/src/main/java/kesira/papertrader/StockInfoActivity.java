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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StockInfoActivity extends AppCompatActivity {
    private String ticker;
    private BigDecimal stockPrice;
    private BigDecimal stockChange;
    private BigDecimal stockPercentChange;
    private static final long MILLION = 1000000L;
    private static final long BILLION = 1000000000L;
    private static final long TRILLION = 1000000000000L;
    private CustomLineChart chart;
    private XAxis xAxis;
    private YAxis yAxis;
    private final HashMap<Integer, ChartSetting> chartSettings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        ticker = getIntent().getStringExtra("ticker");
        getTickerDetails();
        stockPrice = Portfolio.getQuote(ticker);
        if (stockPrice != null) {
            ((TextView) findViewById(R.id.stockPrice)).setText(Portfolio.formatCurrency(stockPrice));
        }
        stockChange = Portfolio.getChange(ticker);
        stockPercentChange = Portfolio.getPercentChange(ticker);
        setChange(stockChange, stockPercentChange);

        chart = (CustomLineChart) findViewById(R.id.chart);
        chart.setNoDataText("Loading...");
        chart.setDragYEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
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
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> getChartData(checkedId));
        getChartData(R.id.radio1D);

        findViewById(R.id.buy).setOnClickListener(v -> showTradeDialogFragment(true));
        findViewById(R.id.sell).setOnClickListener(v -> showTradeDialogFragment(false));

        updatePosition();
    }

    private void getTickerDetails() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String result = APIHelper.get("https://api.polygon.io/v3/reference/tickers/" + ticker + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
            try {
                JSONObject jsonObject = new JSONObject(result).getJSONObject("results");
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
            boolean changePositive = Portfolio.isPositive(change);
            TextView changeText = findViewById(R.id.stockChange);
            changeText.setText((changePositive ? "+" : "") + Portfolio.formatCurrency(change) + (changePositive ? " (+" : " (") + Portfolio.formatPercentage(percentChange) + ")");
            changeText.setTextColor(changePositive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    private void getChartData(int checkedId) {
        ChartSetting chartSetting = chartSettings.get(checkedId);
        if (chartSetting == null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                String url = "https://api.polygon.io/v2/aggs/ticker/" + ticker + "/range/";
                if (checkedId == R.id.radio1D) {
                    url += "5/minute/" + APIHelper.subToday(Calendar.DAY_OF_WEEK, 0);
                } else if (checkedId == R.id.radio1W) {
                    url += "30/minute/" + APIHelper.subToday(Calendar.DAY_OF_WEEK, 7);
                } else if (checkedId == R.id.radio1M) {
                    url += "1/day/" + APIHelper.subToday(Calendar.MONTH, 1);
                } else if (checkedId == R.id.radio3M) {
                    url += "1/day/" + APIHelper.subToday(Calendar.MONTH, 3);
                } else if (checkedId == R.id.radio1Y) {
                    url += "1/day/" + APIHelper.subToday(Calendar.YEAR, 1);
                } else if (checkedId == R.id.radio2Y) {
                    url += "1/day/" + APIHelper.subToday(Calendar.YEAR, 2);
                }
                url += "/" + APIHelper.subToday(Calendar.DAY_OF_WEEK, 0) + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up";
                String result = APIHelper.get(url);
                try {
                    JSONArray jsonArray = new JSONObject(result).getJSONArray("results");
                    int length = jsonArray.length();
                    ArrayList<Entry> entries = new ArrayList<>();
                    ArrayList<String> xAxisValues = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat xAxisFormat = new SimpleDateFormat(checkedId == R.id.radio1D ? "h:mm a" : (checkedId == R.id.radio1W || checkedId == R.id.radio1M ? "MMM d" : "MMM yyyy"), Locale.ENGLISH);
                    xAxisFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    for (int i = 0; i < length; i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        entries.add(i, new Entry(i, Float.parseFloat(jsonObject.getString("c"))));
                        calendar.setTimeInMillis(jsonObject.getLong("t"));
                        xAxisValues.add(xAxisFormat.format(calendar.getTime()));
                    }
                    LineDataSet lineDataSet = new LineDataSet(entries, "Quotes");
                    lineDataSet.setDrawHorizontalHighlightIndicator(false);
                    lineDataSet.setDrawValues(false);
                    lineDataSet.setDrawCircles(false);
                    lineDataSet.setLineWidth(2);
                    BigDecimal open = BigDecimal.valueOf(entries.get(0).getY());
                    BigDecimal change = BigDecimal.valueOf(entries.get(length - 1).getY()).subtract(open);
                    lineDataSet.setColor((checkedId == R.id.radio1D ? Portfolio.isPositive(stockChange) : Portfolio.isPositive(change)) ? Color.parseColor("#33CC33") : Color.RED);
                    LineData lineData = new LineData(lineDataSet);
                    ChartSetting setting = new ChartSetting(lineData, xAxisValues, checkedId == R.id.radio1D ? stockChange : Portfolio.roundCurrency(change), checkedId == R.id.radio1D ? stockPercentChange : Portfolio.roundPercentage(Portfolio.divide(change, open)));
                    if (checkedId == R.id.radio1D) {
                        BigDecimal previousClose = Portfolio.getPreviousClose(ticker);
                        if (previousClose != null) {
                            LimitLine limitLine = new LimitLine(previousClose.floatValue());
                            limitLine.setLineColor(Color.parseColor("#3F51B5"));
                            limitLine.setLineWidth(1);
                            limitLine.enableDashedLine(30, 30, 0);
                            limitLine.setLabel("Previous close " + Portfolio.formatCurrency(previousClose));
                            setting.setLimitLine(limitLine);
                        }
                    }
                    chartSettings.put(checkedId, setting);
                    setChart(setting);
                    handler.post(() -> chart.animateX(500));
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
        LimitLine limitLine = chartSetting.getLimitLine();
        if (limitLine == null) {
            yAxis.removeAllLimitLines();
            yAxis.resetAxisMaximum();
            yAxis.resetAxisMinimum();
        } else {
            yAxis.addLimitLine(limitLine);
            float previousClose = limitLine.getLimit();
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
            ((TextView) findViewById(R.id.shares)).setText(shares.toPlainString());
            BigDecimal totalValue = Portfolio.roundCurrency(shares.multiply(stockPrice));
            ((TextView) findViewById(R.id.totalValue)).setText(Portfolio.formatCurrency(totalValue));
            ((TextView) findViewById(R.id.percentageOfPortfolio)).setText(Portfolio.isPortfolioValueReady() ? Portfolio.createPercentage(totalValue, Portfolio.getPortfolioValue()) : "");
            BigDecimal averageCost = Portfolio.getCost(ticker);
            ((TextView) findViewById(R.id.averageCost)).setText(Portfolio.formatCurrency(averageCost));
            BigDecimal priceChange = stockPrice.subtract(averageCost);
            boolean priceChangePositive = Portfolio.isPositive(priceChange);
            TextView performanceText = (TextView) findViewById(R.id.performance);
            performanceText.setText((priceChangePositive ? "+" : "") + Portfolio.formatCurrency(Portfolio.roundCurrency(shares.multiply(priceChange))) + (priceChangePositive ? " (+" : " (") + Portfolio.createPercentage(priceChange, averageCost) + ")");
            performanceText.setTextColor(priceChangePositive ? Color.parseColor("#33CC33") : Color.RED);
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
}