package kesira.papertrader;

import android.content.Intent;
import android.os.Bundle;

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
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            String ticker = intent.getDataString();
            Portfolio portfolio = Portfolio.getInstance();
            if (portfolio.inPositions(ticker) || portfolio.inWatchlist(ticker)) {
                Intent stockInfoIntent = new Intent(SearchActivity.this, StockInfoActivity.class);
                stockInfoIntent.putExtra("ticker", ticker);
                startActivity(stockInfoIntent);
            } else {
                portfolio.add(ticker);
            }
        }
        finish();
    }
}
