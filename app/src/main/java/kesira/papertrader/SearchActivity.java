package kesira.papertrader;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends AppCompatActivity {
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
            Portfolio portfolio = Portfolio.getInstance();
            if (actionView || ticker.matches("^[A-Z]+$|^[A-Z]+[.][A-Z]+$")) {
                if (portfolio.inPositions(ticker) || portfolio.inWatchlist(ticker)) {
                    Intent stockInfoIntent = new Intent(SearchActivity.this, StockInfoActivity.class);
                    stockInfoIntent.putExtra("ticker", ticker);
                    startActivity(stockInfoIntent);
                } else {
                    if (actionView) {
                        portfolio.add(ticker);
                    } else {
                        portfolio.addIfValid(ticker);
                    }
                }
            } else {
                Toast.makeText(this, "Invalid ticker", Toast.LENGTH_LONG).show();
            }
            SearchMenuItemHelper.getInstance().collapseSearch();
        }
        finish();
    }
}
