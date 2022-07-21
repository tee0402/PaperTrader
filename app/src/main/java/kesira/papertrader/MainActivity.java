package kesira.papertrader;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private final Portfolio portfolio = Portfolio.getInstance();
    private EditText enterTicker;
    private String tickerSelected;
    private CustomLineChart chart;
    private XAxis xAxis;
    private int numDates;
    private List<Date> dates;
    private List<Entry> entries;
    private RadioGroup radioGroup;
    private final Map<Integer, ChartSetting> chartSettings = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.chart);
        ViewGroup.LayoutParams layoutParams = chart.getLayoutParams();
        layoutParams.height = getResources().getDisplayMetrics().heightPixels / 4;
        chart.setLayoutParams(layoutParams);
        chart.setNoDataText("Loading...");
        chart.setDragYEnabled(false);
        chart.setScaleYEnabled(false);
        chart.setDrawGridBackground(true);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setOnTouchListener((v, event) -> {
            v.performClick();
            if (event.getAction() == MotionEvent.ACTION_UP) {
                chart.highlightValues(null);
            }
            return super.onTouchEvent(event);
        });
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(4, false);
        radioGroup = findViewById(R.id.radioGroup);

        enterTicker = findViewById(R.id.enterTicker);
        enterTicker.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                addToWatchlist(v);
                return true;
            }
            return false;
        });
        findViewById(R.id.addTicker).setOnClickListener(this::addToWatchlist);

        NonScrollListView watchlist = findViewById(R.id.watchlistView);
        registerForContextMenu(watchlist);
        portfolio.initialize(this, findViewById(R.id.positionsView), watchlist);
    }

    void initializeChartData() {
        dates = portfolio.getDatesList();
        numDates = dates.size();
        List<Float> portfolioValues = portfolio.getPortfolioValuesList();

        Map<Integer, Calendar> startCalendars = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calendar.setTime(dates.get(numDates - 1));
        putStartCalendar(startCalendars, R.id.radioAll, calendar, Calendar.YEAR, -100);
        Calendar start5Y = putStartCalendar(startCalendars, R.id.radio5Y, calendar, Calendar.YEAR, -5);
        putStartCalendar(startCalendars, R.id.radio1Y, calendar, Calendar.YEAR, -1);
        Calendar start3M = putStartCalendar(startCalendars, R.id.radio3M, calendar, Calendar.MONTH, -3);
        putStartCalendar(startCalendars, R.id.radio1M, calendar, Calendar.MONTH, -1);
        putStartCalendar(startCalendars, R.id.radio1W, calendar, Calendar.WEEK_OF_MONTH, -1);

        entries = new ArrayList<>();
        List<String> markerDates = new ArrayList<>();
        SimpleDateFormat markerDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
        markerDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        for (int i = 0; i < numDates; i++) {
            Date date = dates.get(i);
            float portfolioValue = portfolioValues.get(i);
            entries.add(new Entry(i, portfolioValue));
            markerDates.add(markerDateFormat.format(date));

            calendar.setTime(date);
            Iterator<Map.Entry<Integer, Calendar>> iterator = startCalendars.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Calendar> startCalendar = iterator.next();
                if (calendar.after(startCalendar.getValue())) {
                    SimpleDateFormat xAxisFormat = new SimpleDateFormat(calendar.after(start3M) ? "MMM d" : (calendar.after(start5Y) ? "MMM yyyy" : "yyyy"), Locale.ENGLISH);
                    xAxisFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    chartSettings.put(startCalendar.getKey(), new ChartSetting(i, new BigDecimal(portfolioValue), xAxisFormat));
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
        CustomMarker marker = new CustomMarker(this);
        marker.setChartView(chart);
        chart.setMarker(marker);
        marker.setMarkerDates(markerDates);
        setChartData(radioGroup.getCheckedRadioButtonId());
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> setChartData(checkedId));
    }

    private Calendar putStartCalendar(Map<Integer, Calendar> startCalendars, int checkedId, Calendar calendar, int field, int amount) {
        Calendar startCalendar = (Calendar) calendar.clone();
        startCalendar.add(field, amount);
        startCalendars.put(checkedId, startCalendar);
        return startCalendar;
    }

    private void setChartData(int checkedId) {
        ChartSetting chartSetting = chartSettings.get(checkedId);
        assert chartSetting != null;
        if (portfolio.isPortfolioValueReady()) {
            showPortfolioValuePerformance(chartSetting);
        }
        int initialIndex = chartSetting.getInitialIndex();
        LineData lineData = chartSetting.getLineData();
        if (lineData == null) {
            LineDataSet lineDataSet = new LineDataSet(entries.subList(initialIndex, numDates), "Portfolio Values");
            lineDataSet.setDrawHorizontalHighlightIndicator(false);
            lineDataSet.setDrawValues(false);
            lineDataSet.setDrawCircles(false);
            lineDataSet.setLineWidth(2);
            boolean isPositivePortfolio = portfolio.isPositiveChange(BigDecimal.valueOf(entries.get(numDates - 1).getY()), BigDecimal.valueOf(entries.get(initialIndex).getY()));
            lineDataSet.setColor(isPositivePortfolio ? Color.parseColor("#33CC33") : Color.RED);
            lineDataSet.setDrawFilled(true);
            lineDataSet.setFillDrawable(ContextCompat.getDrawable(this, isPositivePortfolio ? R.drawable.fill_green : R.drawable.fill_red));
            lineData = new LineData(lineDataSet);
            chartSetting.setLineData(lineData);
        }
        chart.setData(lineData);
        SimpleDateFormat xAxisFormat = chartSetting.getXAxisFormat();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                if (i < initialIndex || i >= numDates) {
                    return "";
                }
                return xAxisFormat.format(dates.get(i));
            }
        });
        chart.invalidate();
    }

    void showPortfolioValuePerformance(ChartSetting chartSetting) {
        if (chartSetting == null) {
            chartSetting = chartSettings.get(radioGroup.getCheckedRadioButtonId());
        }
        if (chartSetting != null) {
            BigDecimal initialPortfolioValue = chartSetting.getInitialPortfolioValue();
            BigDecimal portfolioValue = portfolio.getPortfolioValue();
            BigDecimal change = portfolio.roundCurrency(portfolioValue.subtract(initialPortfolioValue));
            boolean positive = portfolio.isPositive(change);
            TextView portfolioValuePerformance = findViewById(R.id.portfolioValuePerformance);
            portfolioValuePerformance.setText((positive ? " +" : " ") + portfolio.formatCurrency(change) + (positive ? " (+" : " (") + portfolio.createPercentage(change, initialPortfolioValue) + ")");
            portfolioValuePerformance.setTextColor(positive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    private void addToWatchlist(View v) {
        String ticker = enterTicker.getText().toString().toUpperCase().replaceAll("[^A-Z.]", "");
        if (ticker.matches("^[A-Z]+$|^[A-Z]+[.][A-Z]+$")) {
            portfolio.addIfValid(ticker);
        } else {
            Toast.makeText(this, "Invalid ticker", Toast.LENGTH_LONG).show();
        }
        enterTicker.getText().clear();
        enterTicker.clearFocus();
        hideSoftInput(v);
    }

    private void hideSoftInput(View v) {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    hideSoftInput(v);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.context_menu_remove, menu);
        tickerSelected = ((Stock) ((NonScrollListView) v).getItemAtPosition(((AdapterContextMenuInfo) menuInfo).position)).getTicker();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        portfolio.remove(tickerSelected);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        inflater.inflate(R.menu.refresh_menu, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchMenuItemHelper.getInstance().initialize(searchMenuItem);
        ((SearchView) searchMenuItem.getActionView()).setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            recreate();
        }
        return super.onOptionsItemSelected(item);
    }
}