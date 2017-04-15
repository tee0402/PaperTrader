package kesira.papertrader;

import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class SearchResultsActivity extends AppCompatActivity {

    private ArrayList<SearchRow> searchRows = new ArrayList<>();
    private SearchAdapter searchAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        ListView searchResults = (ListView) findViewById(R.id.searchResults);
        searchAdapter = new SearchAdapter(getBaseContext(), searchRows);
        searchResults.setAdapter(searchAdapter);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        searchRows = new ArrayList<>();
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + query);
        }
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            getLayoutInflater().inflate(R.layout.activity_search_results, null);
            findViewById(R.id.progressBarSearch).setVisibility(View.VISIBLE);
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
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    searchRows.add(new SearchRow(jsonObject.getString("Symbol"), jsonObject.getString("Name")));
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
            searchAdapter.notifyDataSetChanged();
            findViewById(R.id.progressBarSearch).setVisibility(View.GONE);
        }
    }
}
