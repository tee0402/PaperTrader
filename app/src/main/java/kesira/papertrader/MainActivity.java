package kesira.papertrader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<CustomRow> positionsRows = new ArrayList<>();
    private final ArrayList<CustomRow> watchlistRows = new ArrayList<>();
    private PositionsAdapter positionsAdapter;
    private WatchlistAdapter watchlistAdapter;
    private String tickerSelected;
    private TextView cash;
    private float portfolioValue;
    private SharedPreferences prefs;
    private int positionsCount;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    addToWatchlist(v);
                    return true;
                }
                return false;
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    hideKeyboard(view);
                }
            }
        });

        ListView positions = (ListView) findViewById(R.id.positions);
        positionsAdapter = new PositionsAdapter(getBaseContext(), positionsRows);
        positions.setAdapter(positionsAdapter);
        positions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), StockInfoActivity.class);
                intent.putExtra("ticker", positionsRows.get(i).getTicker());
                startActivity(intent);
            }
        });

        ListView watchlist = (ListView) findViewById(R.id.watchlist);
        registerForContextMenu(watchlist);
        watchlistAdapter = new WatchlistAdapter(getBaseContext(), watchlistRows);
        watchlist.setAdapter(watchlistAdapter);
        watchlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), StockInfoActivity.class);
                intent.putExtra("ticker", watchlistRows.get(i).getTicker());
                startActivity(intent);
            }
        });

        cash = (TextView) findViewById(R.id.cash);
        prefs = getSharedPreferences("Save", Context.MODE_PRIVATE);
        if (prefs.getFloat("cash", -1) == -1) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("cash", 10000);
            editor.apply();
            cash.setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)));
        }
        else {
            cash.setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)));
        }
        portfolioValue = prefs.getFloat("cash", -1);

        Set<String> positionsSet = prefs.getStringSet("positions", new HashSet<String>());
        Set<String> watchlistSet = prefs.getStringSet("watchlist", new HashSet<String>());
        positionsCount = positionsSet.size();
        Iterator<String> positionsIterator = positionsSet.iterator();
        Iterator<String> watchlistIterator = watchlistSet.iterator();
        for (int i = 0; i < positionsSet.size(); i++) {
            new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + positionsIterator.next());
        }
        for (int i = 0; i < watchlistSet.size(); i++) {
            new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + watchlistIterator.next());
        }

        if (positionsCount == 0) {
            showPortfolioValue();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        cash.setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)));

        positionsAdapter.notifyDataSetChanged();
        for (int i = 0; i < positionsRows.size(); i++) {
            if (prefs.getInt(positionsRows.get(i).getTicker(), 0) == 0) {
                watchlistRows.add(positionsRows.get(i));
                watchlistAdapter.notifyDataSetChanged();
                positionsRows.remove(i);
                positionsAdapter.notifyDataSetChanged();
            }
        }
        for (int i = 0; i < watchlistRows.size(); i++) {
            if (prefs.getInt(watchlistRows.get(i).getTicker(), 0) > 0) {
                positionsRows.add(watchlistRows.get(i));
                positionsAdapter.notifyDataSetChanged();
                watchlistRows.remove(i);
                watchlistAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_remove, menu);
        ListView listView = (ListView) v;
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        tickerSelected = ((CustomRow) listView.getItemAtPosition(acmi.position)).getTicker();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        removeFromWatchlist(tickerSelected);
        watchlistAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            finish();
            startActivity(getIntent());
        }
        return true;
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void addToWatchlist(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String ticker = editText.getText().toString();
        if (!containsTicker(ticker)) {
            new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
        }
        editText.getText().clear();
        editText.clearFocus();
        view.requestFocus();
    }

    private void removeFromWatchlist(String ticker) {
        for (int i = 0; i < watchlistRows.size(); i++) {
            if (watchlistRows.get(i).getTicker().equals(ticker)) {
                watchlistRows.remove(i);
                break;
            }
        }
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
        watchlistSet.remove(ticker);
        editor.putStringSet("watchlist", watchlistSet);
        editor.apply();
    }

    private boolean containsTicker(String ticker) {
        Set<String> positionsSet = prefs.getStringSet("positions", new HashSet<String>());
        Set<String> watchlistSet = prefs.getStringSet("watchlist", new HashSet<String>());
        return positionsSet.contains(ticker) || watchlistSet.contains(ticker);
    }

    private void showPortfolioValue() {
        TextView portfolioValueText = (TextView) findViewById(R.id.portfolioValue);
        portfolioValueText.setText(NumberFormat.getCurrencyInstance().format(portfolioValue));

        TextView portfolioValuePerformanceText = (TextView) findViewById(R.id.portfolioValuePerformance);
        if (portfolioValue / 10000 - 1 >= 0) {
            portfolioValuePerformanceText.setText("+" + NumberFormat.getCurrencyInstance().format(portfolioValue - 10000) + " (+" + new DecimalFormat("0.00").format((portfolioValue / 10000 - 1) * 100) + "%)");
            portfolioValuePerformanceText.setTextColor(Color.parseColor("#33CC33"));
        }
        else {
            portfolioValuePerformanceText.setText(NumberFormat.getCurrencyInstance().format(portfolioValue - 10000) + " (" + new DecimalFormat("0.00").format((portfolioValue / 10000 - 1) * 100) + "%)");
            portfolioValuePerformanceText.setTextColor(Color.RED);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("portfolioValue", portfolioValue);
        editor.apply();
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.progressBarPositions).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) url.openConnection();
                }
                while (urlConnection.getResponseCode() >= 400);
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String stockInfo = "";
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stockInfo += line + "\n";
                    }
                    bufferedReader.close();
                    return stockInfo.replaceAll("// ", "");
                }
                finally {
                    urlConnection.disconnect();
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                result = "Error getting stock quote";
            }
            Log.i("INFO", result);
            try {
                JSONArray jsonArray = new JSONArray(result);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                Set<String> positionsSet = prefs.getStringSet("positions", new HashSet<String>());
                //If it is a Markit on Demand query
                if (!jsonObject.isNull("Exchange")) {
                    if (jsonObject.getString("Exchange").equals("NYSE") || jsonObject.getString("Exchange").equals("BATS Trading Inc")) {
                        new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NYSE%3A" + jsonObject.getString("Symbol"));
                    }
                    else if (jsonObject.getString("Exchange").equals("NASDAQ")) {
                        new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NASDAQ%3A" + jsonObject.getString("Symbol"));
                    }
                }
                //If it is a Google Finance query and ticker is a position
                else if (positionsSet.contains(jsonObject.getString("t"))) {
                    positionsRows.add(new CustomRow(jsonObject.getString("t"), jsonObject.getString("l"), jsonObject.getString("cp")));
                    positionsAdapter.notifyDataSetChanged();
                    portfolioValue += Float.valueOf(jsonObject.getString("l")) * prefs.getInt(jsonObject.getString("t"), 0);
                    positionsCount--;
                }
                //If it is a Google Finance query and ticker is in watchlist or not in positions nor watchlist
                else {
                    watchlistRows.add(new CustomRow(jsonObject.getString("t"), jsonObject.getString("l"), jsonObject.getString("cp")));
                    watchlistAdapter.notifyDataSetChanged();
                    SharedPreferences.Editor editor = prefs.edit();
                    Set<String> watchlistSet = new HashSet<>(prefs.getStringSet("watchlist", new HashSet<String>()));
                    watchlistSet.add(jsonObject.getString("t"));
                    editor.putStringSet("watchlist", watchlistSet);
                    editor.apply();
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }

            findViewById(R.id.progressBarPositions).setVisibility(View.GONE);
            findViewById(R.id.progressBarWatchlist).setVisibility(View.GONE);

            if (positionsCount == 0) {
                showPortfolioValue();
            }
        }
    }
}