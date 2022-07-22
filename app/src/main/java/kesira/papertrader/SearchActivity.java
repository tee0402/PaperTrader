package kesira.papertrader;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {
    private final Portfolio portfolio = Portfolio.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        boolean actionView = action.equals(Intent.ACTION_VIEW);
        boolean actionSearch = action.equals(Intent.ACTION_SEARCH);
        if (actionView || actionSearch) {
            String ticker = actionView ? intent.getDataString() : intent.getStringExtra(SearchManager.QUERY).toUpperCase().replaceAll("[^A-Z.]", "");
            if (actionView || ticker.matches("^[A-Z]+$|^[A-Z]+[.][A-Z]+$")) {
                if (portfolio.inPortfolio(ticker)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("ticker", ticker);
                    portfolio.startStockInfoFragment(bundle);
                } else {
                    startStockInfoActivityIfValid(ticker);
                }
            } else {
                Toast.makeText(this, "Invalid ticker", Toast.LENGTH_LONG).show();
            }
            SearchMenuItemHelper.getInstance().collapseSearch();
        }
        finish();
    }

    private void startStockInfoActivityIfValid(String ticker) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String result = APIHelper.get("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + ticker + "&apikey=1275");
            try {
                JSONObject jsonObject = new JSONObject(result).getJSONObject("Global Quote");
                if (jsonObject.length() > 0) {
                    Bundle bundle = new Bundle();
                    bundle.putString("ticker", ticker);
                    bundle.putString("previousClose", portfolio.formatSimpleCurrency(new BigDecimal(jsonObject.getString("08. previous close"))));
                    bundle.putString("quote", portfolio.formatSimpleCurrency(new BigDecimal(jsonObject.getString("05. price"))));
                    bundle.putString("change", portfolio.formatSimpleCurrency(new BigDecimal(jsonObject.getString("09. change"))));
                    String changePercent = jsonObject.getString("10. change percent");
                    bundle.putString("percentChange", portfolio.formatSimplePercentage(portfolio.percentageToDecimal(new BigDecimal(changePercent.substring(0, changePercent.length() - 1)))));
                    portfolio.startStockInfoFragment(bundle);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(this, "Invalid ticker", Toast.LENGTH_LONG).show());
                }
            } catch (JSONException e) {
                Log.e("Exception", e.getMessage());
            }
        });
    }
}
