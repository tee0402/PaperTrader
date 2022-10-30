package kesira.papertrader;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.navigation.NavigationView;

import java.math.BigDecimal;
import java.text.DateFormat;
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

public class MainFragment extends Fragment {
    private final Portfolio portfolio = Portfolio.getInstance();
    private View view;
    private MainActivity activity;
    private CustomLineChart chart;
    private XAxis xAxis;
    private RadioGroup radioGroup;
    private EditText enterTicker;
    private List<Date> dates;
    private int numDates;
    private List<Entry> entries;
    private final Map<Integer, ChartSetting> chartSettings = new HashMap<>();
    private String tickerSelected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);
        activity = (MainActivity) requireActivity();

        DrawerLayout drawerLayout = view.findViewById(R.id.drawerLayout);
        NavigationView navigationView = view.findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .hide(this)
                    .add(R.id.fragmentContainerView, HistoryFragment.class, null)
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
            drawerLayout.close();
            return false;
        });
        activity.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                activity.setActionBarTitle(getString(R.string.portfolio));
                menuInflater.inflate(R.menu.search_menu, menu);
                menuInflater.inflate(R.menu.refresh_menu, menu);
                MenuItem searchMenuItem = menu.findItem(R.id.search);
                SearchMenuItemHelper.getInstance().initialize(searchMenuItem);
                ((SearchView) searchMenuItem.getActionView()).setSearchableInfo(((SearchManager) activity.getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(activity.getComponentName()));
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (activity.getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.refresh) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .replace(R.id.fragmentContainerView, MainFragment.class, null)
                                .commit();
                    } else if (itemId == android.R.id.home) {
                        if (drawerLayout.isOpen()) {
                            drawerLayout.close();
                        } else {
                            drawerLayout.open();
                        }
                    }
                }
                return false;
            }
        }, getViewLifecycleOwner());

        chart = view.findViewById(R.id.chart);
        chart.initialize(getResources().getDisplayMetrics().heightPixels / 4, activity);
        chart.getAxisLeft().setEnabled(false);
        xAxis = chart.getXAxis();
        radioGroup = view.findViewById(R.id.radioGroup);

        enterTicker = view.findViewById(R.id.enterTicker);
        enterTicker.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                addToWatchlist(v);
                return true;
            }
            return false;
        });
        view.findViewById(R.id.addTicker).setOnClickListener(this::addToWatchlist);

        NonScrollListView watchlist = view.findViewById(R.id.watchlistView);
        registerForContextMenu(watchlist);
        portfolio.initialize(this, view.findViewById(R.id.positionsView), watchlist);

        return view;
    }

    private void addToWatchlist(View v) {
        String ticker = enterTicker.getText().toString().toUpperCase().replaceAll("[^A-Z.]", "");
        if (ticker.matches("^[A-Z]+$|^[A-Z]+[.][A-Z]+$")) {
            portfolio.addIfValid(ticker);
        } else {
            Toast.makeText(activity, "Invalid ticker", Toast.LENGTH_LONG).show();
        }
        enterTicker.getText().clear();
        enterTicker.clearFocus();
        activity.hideSoftInput(v);
    }

    void showCash(String cash) {
        ((TextView) view.findViewById(R.id.cash)).setText(cash);
    }
    void setPositionsVisibility(boolean visible) {
        view.findViewById(R.id.positions).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    void setProgressBarVisibility(boolean positions, boolean visible) {
        view.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    void showPortfolioValueIfReady() {
        if (portfolio.isPortfolioValueReady()) {
            BigDecimal portfolioValue = portfolio.getPortfolioValue();
            ((TextView) view.findViewById(R.id.portfolioValue)).setText(portfolio.formatCurrency(portfolioValue));
            showPortfolioValuePerformance(chartSettings.get(radioGroup.getCheckedRadioButtonId()), portfolioValue);
        }
    }
    void showPortfolioValuePerformance(ChartSetting chartSetting, BigDecimal portfolioValue) {
        if (chartSetting != null) {
            BigDecimal initialPortfolioValue = chartSetting.getInitialPortfolioValue();
            BigDecimal change = portfolio.roundCurrency(portfolioValue.subtract(initialPortfolioValue));
            boolean positive = portfolio.isPositive(change);
            TextView portfolioValuePerformance = view.findViewById(R.id.portfolioValuePerformance);
            portfolioValuePerformance.setText((positive ? " +" : " ") + portfolio.formatCurrency(change) + (positive ? " (+" : " (") + portfolio.createPercentage(change, initialPortfolioValue) + ")");
            portfolioValuePerformance.setTextColor(positive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    void initializeChartData(List<Date> dates, List<Float> portfolioValues) {
        this.dates = dates;
        numDates = dates.size();

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
        DateFormat markerDateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
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
                    DateFormat xAxisFormat = new SimpleDateFormat(calendar.after(start3M) ? "MMM d" : (calendar.after(start5Y) ? "MMM yyyy" : "yyyy"), Locale.ENGLISH);
                    xAxisFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                    chartSettings.put(startCalendar.getKey(), new ChartSetting(i, new BigDecimal(portfolioValue), xAxisFormat));
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
        CustomMarker marker = new CustomMarker(activity);
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
            showPortfolioValuePerformance(chartSetting, portfolio.getPortfolioValue());
        }
        int initialIndex = chartSetting.getInitialIndex();
        LineData lineData = chartSetting.getLineData();
        if (lineData == null) {
            LineDataSet lineDataSet = new LineDataSet(entries.subList(initialIndex, numDates), "Portfolio Values");
            lineDataSet.setDrawHorizontalHighlightIndicator(false);
            lineDataSet.setDrawValues(false);
            lineDataSet.setDrawCircles(false);
            lineDataSet.setLineWidth(2);
            boolean isPositivePortfolio = portfolio.isPositiveChange(BigDecimal.valueOf(entries.get(initialIndex).getY()), BigDecimal.valueOf(entries.get(numDates - 1).getY()));
            lineDataSet.setColor(isPositivePortfolio ? Color.parseColor("#33CC33") : Color.RED);
            lineDataSet.setDrawFilled(true);
            lineDataSet.setFillDrawable(ContextCompat.getDrawable(activity, isPositivePortfolio ? R.drawable.fill_green : R.drawable.fill_red));
            lineData = new LineData(lineDataSet);
            chartSetting.setLineData(lineData);
        }
        chart.setData(lineData);
        DateFormat xAxisFormat = chartSetting.getXAxisFormat();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return i < initialIndex || i >= numDates ? "" : xAxisFormat.format(dates.get(i));
            }
        });
        chart.invalidate();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        activity.getMenuInflater().inflate(R.menu.context_menu_remove, menu);
        tickerSelected = ((Stock) ((NonScrollListView) v).getItemAtPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position)).getTicker();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        portfolio.remove(tickerSelected);
        return true;
    }
}