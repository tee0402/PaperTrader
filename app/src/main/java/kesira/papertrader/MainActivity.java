package kesira.papertrader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
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
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<CustomRow> customRows = new ArrayList<>();
    private TextViewAdapter listAdapter;
    private String tickerSelected;
    private float portfolioValue;
    private ArrayList<String> positions = new ArrayList<>();

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setOnKeyListener(new View.OnKeyListener() {
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

        ListView listView = (ListView) findViewById(R.id.listview);
        registerForContextMenu(listView);
        listAdapter = new TextViewAdapter(getBaseContext(), customRows);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), StockInfoActivity.class);
                intent.putExtra("ticker", customRows.get(i).getTicker());
                startActivity(intent);
            }
        });

        try {
            InputStream inputStream = this.openFileInput("save.txt");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String str;
                if ((str = bufferedReader.readLine()) != null) {
                    portfolioValue = Float.valueOf(str);
                }
                while ((str = bufferedReader.readLine()) != null) {
                    positions.add(str);
                }
                inputStream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "Reading from save failed: " + e.toString());
            portfolioValue = 10000f;
        }

        ((TextView) findViewById(R.id.portfolioValue)).setText(String.format("$%.2f", portfolioValue));

        try {
            InputStream inputStream = this.openFileInput("watchlist.txt");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + str);
                }
                inputStream.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "Reading from saved watchlist failed: " + e.toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("watchlist.txt", Context.MODE_PRIVATE));
            for (int i = 0; i < customRows.size(); i++) {
                outputStreamWriter.write(customRows.get(i).getTicker() + "\n");
            }
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "Writing to saved watchlist failed: " + e.toString());
        }
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("save.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(portfolioValue + "\n");
            for (int i = 0; i < positions.size(); i++) {
                outputStreamWriter.write(positions.get(i) + "\n");
            }
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "Writing to saved watchlist failed: " + e.toString());
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
        removeTicker(tickerSelected);
        listAdapter.notifyDataSetChanged();
        return true;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void addToWatchlist(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String ticker = editText.getText().toString();
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
        editText.getText().clear();
        editText.clearFocus();
        view.requestFocus();
    }

    void removeTicker(String ticker) {
        for (int i = 0; i < customRows.size(); i++) {
            if (customRows.get(i).getTicker().equals(ticker)) {
                customRows.remove(i);
                break;
            }
        }
    }

    private boolean containsTicker(String ticker) {
        for (int i = 0; i < customRows.size(); i++) {
            if (customRows.get(i).getTicker().equals(ticker)) {
                return true;
            }
        }
        return false;
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            findViewById(R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
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
            catch(Exception e) {
                Log.e("Exception", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                result = "Error getting stock quote";
            }
            Log.i("INFO", result);
            try {
                JSONArray jsonArray = new JSONArray(result);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                if (!jsonObject.isNull("Exchange")) {
                    if (jsonObject.getString("Exchange").equals("NYSE")) {
                        new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NYSE%3A" + jsonObject.getString("Symbol"));
                    } else if (jsonObject.getString("Exchange").equals("NASDAQ")) {
                        new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NASDAQ%3A" + jsonObject.getString("Symbol"));
                    }
                }
                else {
                    if (!containsTicker(jsonObject.getString("t"))) {
                        customRows.add(new CustomRow(jsonObject.getString("t"), jsonObject.getString("l"), jsonObject.getString("cp")));
                    }
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            findViewById(R.id.progressBarWatchlist).setVisibility(View.GONE);
            listAdapter.notifyDataSetChanged();
        }
    }
}
