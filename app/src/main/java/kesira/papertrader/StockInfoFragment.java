package kesira.papertrader;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class StockInfoFragment extends Fragment {
    private final Portfolio portfolio = Portfolio.getInstance();
    private View view;
    private MainActivity activity;
    private final List<QueryDocumentSnapshot> history = new ArrayList<>();
    private HistoryArrayAdapter adapter;
    private String ticker;
    private BigDecimal previousClose;
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
    private final Map<Integer, ChartSetting> chartSettings = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_stock_info, container, false);
        activity = (MainActivity) requireActivity();

        Bundle bundle = requireArguments();
        ticker = bundle.getString("ticker");
        getTickerDetails();
        boolean fromNonPortfolio = bundle.containsKey("quote");
        previousClose = fromNonPortfolio ? new BigDecimal(bundle.getString("previousClose")) : portfolio.getPreviousClose(ticker);
        stockPrice = fromNonPortfolio ? new BigDecimal(bundle.getString("quote")) : portfolio.getQuote(ticker);
        if (stockPrice != null) {
            ((TextView) view.findViewById(R.id.stockPrice)).setText(portfolio.formatCurrency(stockPrice));
        }
        stockChange = fromNonPortfolio ? new BigDecimal(bundle.getString("change")) : portfolio.getChange(ticker);
        stockPercentChange = fromNonPortfolio ? new BigDecimal(bundle.getString("percentChange")) : portfolio.getPercentChange(ticker);
        setChange(stockChange, stockPercentChange);

        chart = view.findViewById(R.id.chart);
        ViewGroup.LayoutParams layoutParams = chart.getLayoutParams();
        layoutParams.height = getResources().getDisplayMetrics().heightPixels / 3;
        chart.setLayoutParams(layoutParams);
        chart.setNoDataText("Loading...");
        chart.setDragYEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setOnTouchListener((v, event) -> {
            v.performClick();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                chart.highlightValues(null);
            }
            return activity.onTouchEvent(event);
        });
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(4, false);
        yAxis = chart.getAxisLeft();
        yAxis.setDrawAxisLine(false);
        marker = new CustomMarker(activity, true);
        marker.setChartView(chart);
        chart.setMarker(marker);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> getChartData(checkedId));
        getChartData(R.id.radio1D);

        view.findViewById(R.id.buy).setOnClickListener(v -> showTradeDialogFragment(true));
        view.findViewById(R.id.sell).setOnClickListener(v -> showTradeDialogFragment(false));

        updatePosition();

        adapter = new HistoryArrayAdapter(activity, history);
        NonScrollListView historyListView = view.findViewById(R.id.historyListView);
        historyListView.setAdapter(adapter);
        view.findViewById(R.id.showAll).setOnClickListener(v -> activity.getSupportFragmentManager().beginTransaction()
                .hide(this)
                .add(R.id.fragmentContainerView, StockHistoryFragment.class, bundle)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit());
        updateHistory();

        activity.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                activity.setActionBarTitle(ticker);
                activity.setActionBarUpIndicatorAsBack();
                if (!portfolio.inPositions(ticker)) {
                    menuInflater.inflate(portfolio.inWatchlist(ticker) ? R.menu.remove_watchlist_menu : R.menu.add_watchlist_menu, menu);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (activity.getSupportFragmentManager().getBackStackEntryCount() == 1) {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.add) {
                        portfolio.add(ticker, previousClose, stockPrice, stockChange, stockPercentChange);
                        activity.invalidateOptionsMenu();
                        Toast.makeText(activity, "Stock added to watchlist", Toast.LENGTH_LONG).show();
                    } else if (itemId == R.id.remove) {
                        portfolio.remove(ticker);
                        activity.invalidateOptionsMenu();
                        Toast.makeText(activity, "Stock removed from watchlist", Toast.LENGTH_LONG).show();
                    } else if (itemId == android.R.id.home) {
                        activity.onBackPressed();
                    }
                }
                return false;
            }
        }, getViewLifecycleOwner());

        return view;
    }

    private void getTickerDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String result = APIHelper.get("https://www.alphavantage.co/query?function=OVERVIEW&symbol=" + ticker + "&apikey=1275");
            try {
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.length() == 0) {
                    result = APIHelper.get("https://api.polygon.io/v3/reference/tickers/" + ticker + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                    jsonObject = new JSONObject(result).getJSONObject("results");
                    String name = jsonObject.getString("name");
                    String exchange = jsonObject.getString("primary_exchange").replace("ARCX", "NYSE Arca").replace("XNAS", "NASDAQ");
                    String marketCap = createMarketCapString(jsonObject.getDouble("share_class_shares_outstanding") * stockPrice.doubleValue());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        ((TextView) view.findViewById(R.id.stockName)).setText(name);
                        ((TextView) view.findViewById(R.id.exchange)).setText(exchange);
                        ((TextView) view.findViewById(R.id.marketCap)).setText(marketCap);
                    });
                } else {
                    String name = jsonObject.getString("Name");
                    String exchange = jsonObject.getString("Exchange");
                    String marketCap = createMarketCapString(jsonObject.getDouble("MarketCapitalization"));
                    String peRatio = jsonObject.getString("PERatio");
                    String divYield = jsonObject.getString("DividendYield");
                    String dividendYield = divYield.equals("0") ? "None" : portfolio.formatPercentage(new BigDecimal(divYield));
                    String description = jsonObject.getString("Description");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        ((TextView) view.findViewById(R.id.stockName)).setText(name);
                        ((TextView) view.findViewById(R.id.exchange)).setText(exchange);
                        ((TextView) view.findViewById(R.id.marketCap)).setText(marketCap);
                        ((TextView) view.findViewById(R.id.peRatio)).setText(peRatio);
                        ((TextView) view.findViewById(R.id.dividendYield)).setText(dividendYield);
                        ((TextView) view.findViewById(R.id.description)).setText(description);
                    });
                }
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
            TextView changeText = view.findViewById(R.id.stockChange);
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
                boolean radio1W = checkedId == R.id.radio1W;
                boolean radio1M = checkedId == R.id.radio1M;
                if (radio1D) {
                    url += "5/minute/" + APIHelper.getToday();
                } else if (radio1W) {
                    url += "30/minute/" + APIHelper.getRangeStart(Calendar.WEEK_OF_MONTH, 1);
                } else if (radio1M) {
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
                    List<Entry> entries = new ArrayList<>();
                    List<String> xAxisValues = new ArrayList<>();
                    List<String> markerDates = new ArrayList<>();
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
                    SimpleDateFormat xAxisFormat = new SimpleDateFormat(radio1D ? "h:mm a" : (radio1W || radio1M ? "MMM d" : "MMM yyyy"), Locale.ENGLISH);
                    xAxisFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    SimpleDateFormat markerDateFormat = new SimpleDateFormat(radio1D ? "h:mm a" : (radio1W ? "EEE, MMM d h:mm a" : (radio1M ? "EEE, MMM d" : "MMM d, yyyy")), Locale.ENGLISH);
                    markerDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    ChartSetting setting;
                    int numResults = jsonArray.length();
                    if (radio1D) {
                        List<Entry> premarketEntries = new ArrayList<>();
                        List<Entry> afterHoursEntries = new ArrayList<>();
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
        List<String> xAxisValues = chartSetting.getXAxisValues();
        int numXAxisValues = xAxisValues.size();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                if (i < 0 || i >= numXAxisValues) {
                    return "";
                }
                return xAxisValues.get(i);
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
        tradeDialogFragment.show(getChildFragmentManager(), buy ? "buy" : "sell");
    }

    void updatePosition() {
        boolean inPositions = portfolio.inPositions(ticker);
        view.findViewById(R.id.sell).setEnabled(inPositions);
        view.findViewById(R.id.position).setVisibility(inPositions ? View.VISIBLE : View.GONE);
        if (inPositions) {
            BigDecimal shares = new BigDecimal(portfolio.getShares(ticker));
            ((TextView) view.findViewById(R.id.shares)).setText(shares.toPlainString());
            BigDecimal totalValue = portfolio.roundCurrency(shares.multiply(stockPrice));
            ((TextView) view.findViewById(R.id.totalValue)).setText(portfolio.formatCurrency(totalValue));
            ((TextView) view.findViewById(R.id.percentageOfPortfolio)).setText(portfolio.isPortfolioValueReady() ? portfolio.createPercentage(totalValue, portfolio.getPortfolioValue()) : "");
            BigDecimal averageCost = portfolio.getCost(ticker);
            ((TextView) view.findViewById(R.id.averageCost)).setText(portfolio.formatCurrency(averageCost));
            BigDecimal priceChange = stockPrice.subtract(averageCost);
            boolean priceChangePositive = portfolio.isPositive(priceChange);
            TextView performanceText = view.findViewById(R.id.performance);
            performanceText.setText((priceChangePositive ? "+" : "") + portfolio.formatCurrency(portfolio.roundCurrency(shares.multiply(priceChange))) + (priceChangePositive ? " (+" : " (") + portfolio.createPercentage(priceChange, averageCost) + ")");
            performanceText.setTextColor(priceChangePositive ? Color.parseColor("#33CC33") : Color.RED);
        }
        activity.invalidateOptionsMenu();
    }

    void updateHistory() {
        history.clear();
        portfolio.queryHistory(history, adapter, ticker, true, view.findViewById(R.id.history), view.findViewById(R.id.showAll));
    }
}