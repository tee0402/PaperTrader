package kesira.papertrader;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private EditText enterTicker;
    private String tickerSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enterTicker = (EditText) findViewById(R.id.enterTicker);
        enterTicker.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                addToWatchlist(v);
                return true;
            }
            return false;
        });
        enterTicker.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        findViewById(R.id.addTicker).setOnClickListener(this::addToWatchlist);

        NonScrollListView watchlist = findViewById(R.id.watchlist);
        registerForContextMenu(watchlist);
        new Portfolio(this, findViewById(R.id.positions), watchlist);
        setCashText();
        showOrHidePositions();
        if (Portfolio.containsWatchlist()) {
            findViewById(R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
        }
    }

    private void addToWatchlist(View v) {
        String ticker = enterTicker.getText().toString();
        if (!ticker.equals("")) {
            Portfolio.add(ticker);
            enterTicker.getText().clear();
            enterTicker.clearFocus();
            v.requestFocus();
        }
    }

    private void setCashText() {
        ((TextView) findViewById(R.id.cash)).setText(Portfolio.getCashString());
    }

    private void showOrHidePositions() {
        int visibility = Portfolio.containsPositions() ? View.VISIBLE : View.GONE;
        findViewById(R.id.positionsText).setVisibility(visibility);
        findViewById(R.id.positionList).setVisibility(visibility);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        setCashText();
        showOrHidePositions();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.context_menu_remove, menu);
        tickerSelected = ((Stock) ((NonScrollListView) v).getItemAtPosition(((AdapterContextMenuInfo) menuInfo).position)).getTicker();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Portfolio.remove(tickerSelected);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh_menu, menu);
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.search);
        collapseSearchOnTouch(findViewById(R.id.mainActivity), searchMenuItem);
        ((SearchView) searchMenuItem.getActionView()).setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));

        return true;
    }

    private void collapseSearchOnTouch(View view, MenuItem searchMenuItem) {
        if (!(view instanceof SearchView)) {
            view.setOnTouchListener((v, event) -> {
                v.performClick();
                searchMenuItem.collapseActionView();
                return false;
            });
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                collapseSearchOnTouch(viewGroup.getChildAt(i), searchMenuItem);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            Portfolio.refresh();
        }
        return super.onOptionsItemSelected(item);
    }

    void showPortfolioValue(String portfolioValue, String portfolioValueChange, String portfolioValueChangePercentage, boolean positive) {
        ((TextView) findViewById(R.id.portfolioValue)).setText(portfolioValue);
        TextView portfolioValuePerformanceText = (TextView) findViewById(R.id.portfolioValuePerformance);
        portfolioValuePerformanceText.setText((positive ? " +" : " ") + portfolioValueChange + (positive ? " (+" : " (") + portfolioValueChangePercentage + ")");
        portfolioValuePerformanceText.setTextColor(positive ? Color.parseColor("#33CC33") : Color.RED);
    }
}