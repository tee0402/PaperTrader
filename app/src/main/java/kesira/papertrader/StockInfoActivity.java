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

import java.math.BigDecimal;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private CustomLineChart chart;
    private XAxis xAxis;
    private YAxis leftAxis;

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
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
//            if (checkedId == R.id.radio1D) {
//                getPreviousClose();
//                leftAxis.addLimitLine(limitLine);
//            } else {
//                leftAxis.removeLimitLine(limitLine);
//            }
            getBars(checkedId);
        });

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

    private void getPreviousClose() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String result = APIHelper.get("https://api.polygon.io/v1/open-close/" + ticker + "/" + subDate(Calendar.DAY_OF_WEEK, 1) + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
            try {
                JSONObject jsonObject = new JSONObject(result).getJSONObject("results");
                String previousClose = jsonObject.getString("close");
                handler.post(() -> {
                    LimitLine limitLine = new LimitLine(Float.parseFloat(previousClose));
                    limitLine.setLineColor(Color.parseColor("#3F51B5"));
                    limitLine.setLineWidth(1);
                    limitLine.enableDashedLine(30, 30, 0);
                    leftAxis.addLimitLine(limitLine);
                });
            } catch (JSONException e) {
                Log.e("Exception", e.getMessage());
            }
        });
    }

    private String subDate(int field, int amount) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        } else if (dayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -2);
        }
        calendar.add(field, -amount);
        return dateFormat.format(calendar.getTime());
    }

    private void getBars(int checkedId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String url = "https://api.polygon.io/v2/aggs/ticker/" + ticker + "/range/";
            if (checkedId == R.id.radio1D) {
                url += "5/minute/" + subDate(Calendar.DAY_OF_WEEK, 0);
            } else if (checkedId == R.id.radio1W) {
                url += "30/minute/" + subDate(Calendar.DAY_OF_WEEK, 7);
            } else if (checkedId == R.id.radio1M) {
                url += "1/day/" + subDate(Calendar.MONTH, 1);
            } else if (checkedId == R.id.radio3M) {
                url += "1/day/" + subDate(Calendar.MONTH, 3);
            } else if (checkedId == R.id.radio1Y) {
                url += "1/day/" + subDate(Calendar.YEAR, 1);
            } else if (checkedId == R.id.radio2Y) {
                url += "1/day/" + subDate(Calendar.YEAR, 2);
            }
            url += "/" + subDate(Calendar.DAY_OF_WEEK, 0) + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up";
            System.out.println(url);
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
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return xAxisValues.get((int) value);
                    }
                });
                LineDataSet lineDataSet = new LineDataSet(entries, "Quotes");
                lineDataSet.setDrawHorizontalHighlightIndicator(false);
                lineDataSet.setDrawValues(false);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setLineWidth(2);
                lineDataSet.setColor(entries.get(0).getY() <= entries.get(length - 1).getY() ? Color.parseColor("#33CC33") : Color.RED);
//                if (prevClose >= lineData.getYMax()) {
//                    leftAxis.setAxisMaximum(prevClose + 0.1f * (prevClose - lineData.getYMin()));
//                } else if (prevClose <= lineData.getYMin()) {
//                    leftAxis.setAxisMinimum(prevClose - 0.1f * (lineData.getYMax() - prevClose));
//                }
                LineData lineData = new LineData(lineDataSet);
                chart.setData(lineData);
                handler.post(() -> chart.animateX(1000));
            } catch (JSONException e) {
                Log.e("Exception", e.getMessage());
            }
        });
    }

    private void getBars() {
        try {
            String result = "".replaceAll("// ", "");
            Object json = new JSONTokener(result).nextValue();
            if (json instanceof JSONObject) {
                xAxis.resetAxisMaximum();
                leftAxis.resetAxisMaximum();
                leftAxis.resetAxisMinimum();
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
                xAxis.setAxisMaximum(78);
            }
        } catch (JSONException | ParseException e) {
            Log.e("Exception", e.getMessage());
        }
    }
}