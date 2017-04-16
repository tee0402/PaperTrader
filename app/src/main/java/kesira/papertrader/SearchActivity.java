package kesira.papertrader;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Intent stockInfoIntent = new Intent(getApplicationContext(), StockInfoActivity.class);
            stockInfoIntent.putExtra("ticker", query.toUpperCase());
            startActivity(stockInfoIntent);
        }
        else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String ticker = intent.getDataString();
            Intent stockInfoIntent = new Intent(getApplicationContext(), StockInfoActivity.class);
            stockInfoIntent.putExtra("ticker", ticker);
            startActivity(stockInfoIntent);
        }
        finish();
    }
}
