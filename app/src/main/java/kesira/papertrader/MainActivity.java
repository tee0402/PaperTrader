package kesira.papertrader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

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

public class MainActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private final ArrayList<CustomRow> positionsRows = new ArrayList<>();
    private final ArrayList<CustomRow> watchlistRows = new ArrayList<>();
    private PositionsAdapter positionsAdapter;
    private WatchlistAdapter watchlistAdapter;
    private String tickerSelected;
    private TextView cash;
    private float portfolioValue;
    private SharedPreferences prefs;
    private int positionsCount;
    private MenuItem searchMenuItem;
    private RewardedVideoAd mAd;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAd = MobileAds.getRewardedVideoAdInstance(this);
        mAd.setRewardedVideoAdListener(this);
        mAd.loadAd("ca-app-pub-4071292763824495/4372652960", new AdRequest.Builder().addTestDevice("4A9CA16A6BD94883A6FAB491F8FA22E9").build());

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

        NonScrollListView positions = (NonScrollListView) findViewById(R.id.positions);
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

        NonScrollListView watchlist = (NonScrollListView) findViewById(R.id.watchlist);
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
        else {
            findViewById(R.id.positionsText).setVisibility(View.VISIBLE);
            findViewById(R.id.positionsList).setVisibility(View.VISIBLE);
        }

        setupUI(findViewById(R.id.mainActivity));
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (prefs.getStringSet("positions", new HashSet<String>()).size() == 0) {
            findViewById(R.id.positionsText).setVisibility(View.GONE);
            findViewById(R.id.positionsList).setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.positionsText).setVisibility(View.VISIBLE);
            findViewById(R.id.positionsList).setVisibility(View.VISIBLE);
        }

        cash.setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)));

        positionsAdapter.notifyDataSetChanged();

        Set<String> watchlistSet = prefs.getStringSet("watchlist", new HashSet<String>());
        Iterator<String> watchlistIterator = watchlistSet.iterator();

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
            else if (!watchlistSet.contains(watchlistRows.get(i).getTicker())) {
                watchlistRows.remove(i);
                watchlistAdapter.notifyDataSetChanged();
            }
        }

        for (int i = 0; i < watchlistSet.size(); i++) {
            boolean displayedInWatchlist = false;
            String ticker = watchlistIterator.next();
            for (int j = 0; j < watchlistRows.size(); j++) {
                if (watchlistRows.get(j).getTicker().equals(ticker)) {
                    displayedInWatchlist = true;
                    break;
                }
            }
            if (!displayedInWatchlist) {
                new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
            }
        }
    }

    @Override
    public void onResume() {
        mAd.resume(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        mAd.pause(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mAd.destroy(this);
        super.onDestroy();
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
        inflater.inflate(R.menu.refresh_menu, menu);
        inflater.inflate(R.menu.search_menu, menu);

        searchMenuItem = menu.findItem(R.id.search);
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRewardedVideoAdLoaded() {

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {
        if (Constants.ad1Clicked) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat("cash", prefs.getFloat("cash", -1) + 5000);
            editor.putFloat("startingAmount", prefs.getFloat("startingAmount", 10000) + 5000);
            editor.apply();
            cash.setText(NumberFormat.getCurrencyInstance().format(prefs.getFloat("cash", -1)));
            portfolioValue += 5000;
            Toast.makeText(this, "Enjoy your extra $5000!", Toast.LENGTH_SHORT).show();
        }
        else {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "Enjoy!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }

    public void showAd1(View view) {
        Constants.ad1Clicked = true;
        if (mAd.isLoaded()) {
            mAd.show();
        }
    }

    public void showAd2(View view) {
        Constants.ad1Clicked = false;
        if (mAd.isLoaded()) {
            mAd.show();
        }
    }

    public void setupUI(View view) {
        if(!(view instanceof SearchView)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    searchMenuItem.collapseActionView();
                    return false;
                }
            });
        }
        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
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
        float startingAmount = prefs.getFloat("startingAmount", 10000);

        portfolioValueText.setText(NumberFormat.getCurrencyInstance().format(portfolioValue));

        TextView portfolioValuePerformanceText = (TextView) findViewById(R.id.portfolioValuePerformance);
        if (portfolioValue / startingAmount - 1 >= 0) {
            portfolioValuePerformanceText.setText(" +" + NumberFormat.getCurrencyInstance().format(portfolioValue - startingAmount) + " (+" + new DecimalFormat("0.00").format((portfolioValue / startingAmount - 1) * 100) + "%)");
            portfolioValuePerformanceText.setTextColor(Color.parseColor("#33CC33"));
        }
        else {
            portfolioValuePerformanceText.setText(" " + NumberFormat.getCurrencyInstance().format(portfolioValue - startingAmount) + " (" + new DecimalFormat("0.00").format((portfolioValue / startingAmount - 1) * 100) + "%)");
            portfolioValuePerformanceText.setTextColor(Color.RED);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("portfolioValue", portfolioValue);
        editor.apply();
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            if (positionsCount > 0) {
                findViewById(R.id.progressBarPositions).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection;
                do {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(0);
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