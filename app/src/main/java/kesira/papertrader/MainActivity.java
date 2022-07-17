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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final Portfolio portfolio = Portfolio.getInstance();
    private EditText enterTicker;
    private CustomLineChart chart;
    private String tickerSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.portfolioChart);
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

    private void addToWatchlist(View v) {
        String ticker = enterTicker.getText().toString();
        if (!ticker.equals("")) {
            portfolio.addIfValid(ticker);
            enterTicker.getText().clear();
            enterTicker.clearFocus();
            hideSoftInput(v);
        }
    }

    private void hideSoftInput(View v) {
        ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    void setChartData() {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> markerDates = new ArrayList<>();
        List<Date> dates = portfolio.getDatesList();
        List<Float> portfolioValues = portfolio.getPortfolioValuesList();
        int size = dates.size();

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(4, false);
        SimpleDateFormat xAxisFormat = new SimpleDateFormat(size <= 260 ? "MMM d" : "MMM yyyy", Locale.ENGLISH);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return xAxisFormat.format(dates.get((int) value));
            }
        });

        SimpleDateFormat markerDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(i, portfolioValues.get(i)));
            markerDates.add(markerDateFormat.format(dates.get(i)));
        }
        CustomMarker marker = new CustomMarker(this);
        marker.setChartView(chart);
        chart.setMarker(marker);
        marker.setMarkerDates(markerDates);
        LineDataSet lineDataSet = new LineDataSet(entries, "Portfolio Values");
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2);
        boolean isPositivePortfolio = portfolio.isPositivePortfolio(BigDecimal.valueOf(entries.get(size - 1).getY()));
        lineDataSet.setColor(isPositivePortfolio ? Color.parseColor("#33CC33") : Color.RED);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillDrawable(ContextCompat.getDrawable(this, isPositivePortfolio ? R.drawable.fill_green : R.drawable.fill_red));
        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);
        chart.invalidate();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        inflater.inflate(R.menu.refresh_menu, menu);
        ((SearchView) menu.findItem(R.id.search).getActionView()).setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            portfolio.refresh();
        }
        return super.onOptionsItemSelected(item);
    }
}