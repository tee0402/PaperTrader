package kesira.papertrader;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
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

public class MainActivity extends AppCompatActivity {
    private EditText enterTicker;
    private String tickerSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        new Portfolio(this, findViewById(R.id.positionsView), watchlist);
    }

    private void addToWatchlist(View v) {
        String ticker = enterTicker.getText().toString();
        if (!ticker.equals("")) {
            Portfolio.addIfValid(ticker);
            enterTicker.getText().clear();
            enterTicker.clearFocus();
            hideSoftInput(v);
        }
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
        Portfolio.remove(tickerSelected);
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
            Portfolio.refresh();
        }
        return super.onOptionsItemSelected(item);
    }
}