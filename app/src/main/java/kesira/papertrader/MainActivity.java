package kesira.papertrader;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<CustomRow> rows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String ticker = "MSFT";
        new RetrieveFeedTask().execute("https://www.google.com/finance/info?q=NASDAQ:" + ticker);

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(new TextViewAdapter(this, rows));

        new SlidingRootNavBuilder(this).withMenuLayout(R.layout.menu_drawer_navigation).inject();
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
                Log.e("Exception", e.getMessage());
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
                JSONObject jsonObject = new JSONObject(result);
                rows.add(new CustomRow(jsonObject.getString("t"), jsonObject.getString("l"), jsonObject.getString("cp")));
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }
    }
}
