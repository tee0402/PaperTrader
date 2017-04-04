package kesira.papertrader;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class StockInfoActivity extends AppCompatActivity {

    private String ticker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_info);

        Intent intent = getIntent();
        ticker = intent.getStringExtra("ticker");
        new RetrieveFeedTask().execute("http://dev.markitondemand.com/MODApis/Api/v2/Lookup/json?input=" + ticker);
        new RetrieveFeedTask().execute("http://finance.google.com/finance/info?client=ig&q=NASDAQ%3A" + ticker);
    }

    public void buy(View view) {
        DialogFragment newFragment = new BuyDialogFragment();
        newFragment.show(getSupportFragmentManager(), "buy");
    }

    public void sell(View view) {
        DialogFragment newFragment = new SellDialogFragment();
        newFragment.show(getSupportFragmentManager(), "sell");
    }

    private class RetrieveFeedTask extends AsyncTask<String, String, String> {

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
                result = "Error getting stock info";
            }
            Log.i("INFO", result);
            try {
                JSONArray jsonArray = new JSONArray(result);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                if (!jsonObject.isNull("Name")) {
                    ((TextView) findViewById(R.id.stockName)).setText(jsonObject.getString("Name"));
                }
                else {
                    ((TextView) findViewById(R.id.stockPrice)).setText(jsonObject.getString("l"));
                    if (Float.valueOf(jsonObject.getString("cp")) >= 0) {
                        ((TextView) findViewById(R.id.stockPercentChange)).setText("+" + jsonObject.getString("cp") + "%");
                        ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.GREEN);
                    }
                    else {
                        ((TextView) findViewById(R.id.stockPercentChange)).setText(String.format("%s%%", jsonObject.getString("cp")));
                        ((TextView) findViewById(R.id.stockPercentChange)).setTextColor(Color.RED);
                    }
                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }
    }
}
