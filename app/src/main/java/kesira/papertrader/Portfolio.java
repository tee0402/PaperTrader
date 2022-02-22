package kesira.papertrader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Executors;

class Portfolio {
    private final MainActivity mainActivity;
    private static SharedPreferences prefs;
    private static Cash cash;
    private static StockCollection positions;
    private static StockCollection watchlist;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DecimalFormat percentageFormat = new DecimalFormat("0.00%");

    Portfolio(MainActivity mainActivity, ListView positionsView, ListView watchlistView) {
        this.mainActivity = mainActivity;
        prefs = mainActivity.getSharedPreferences("Save", Context.MODE_PRIVATE);
        cash = new Cash();
        positions = new StockCollection(true, positionsView);
        watchlist = new StockCollection(false, watchlistView);
        if (!containsPositions()) {
            showPortfolioValueIfReady();
        }
    }

    static BigDecimal getCash() {
        return cash.get();
    }
    static String getCashString() {
        return cash.getString();
    }

    static boolean containsPositions() {
        return positions.isNonEmpty();
    }

    static boolean inPositions(String ticker) {
        return positions.contains(ticker);
    }

    static boolean inWatchlist(String ticker) {
        return watchlist.contains(ticker);
    }

    static int getShares(String ticker) {
        return positions.getShares(ticker);
    }

    static BigDecimal getCost(String ticker) {
        return positions.getCost(ticker);
    }

    static BigDecimal getPreviousClose(String ticker) {
        if (inPositions(ticker)) {
            return positions.getPreviousClose(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getPreviousClose(ticker);
        }
        return null;
    }

    static BigDecimal getQuote(String ticker) {
        if (inPositions(ticker)) {
            return positions.getQuote(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getQuote(ticker);
        }
        return null;
    }

    static BigDecimal getChange(String ticker) {
        if (inPositions(ticker)) {
            return positions.getChange(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getChange(ticker);
        }
        return null;
    }

    static BigDecimal getPercentChange(String ticker) {
        if (inPositions(ticker)) {
            return positions.getPercentChange(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getPercentChange(ticker);
        }
        return null;
    }

    static void addIfValid(String ticker) {
        if (!watchlist.contains(ticker) && !positions.contains(ticker)) {
            watchlist.addIfValid(ticker);
        }
    }

    static void add(String ticker) {
        if (!watchlist.contains(ticker) && !positions.contains(ticker)) {
            watchlist.add(ticker);
        }
    }

    static void remove(String ticker) {
        if (watchlist.contains(ticker)) {
            watchlist.remove(ticker);
        }
    }

    static void changePosition(boolean buy, String ticker, int shares, BigDecimal price) {
        BigDecimal total = new BigDecimal(shares).multiply(price);
        int sharesOwned = positions.getShares(ticker);
        if (buy && cash.has(total)) {
            Stock watchlistStock = watchlist.remove(ticker);
            positions.changePosition(true, ticker, shares, price, watchlistStock);
            cash.change(false, total);
        } else if (!buy && shares <= sharesOwned) {
            Stock stock = positions.changePosition(false, ticker, shares, price, null);
            if (shares == sharesOwned && stock != null) {
                watchlist.add(stock);
            }
            cash.change(true, total);
        }
    }

    private void showPortfolioValueIfReady() {
        if (isPortfolioValueReady()) {
            BigDecimal portfolioValue = getPortfolioValue();
            BigDecimal initialCash = cash.getInitial();
            BigDecimal portfolioValueChange = roundCurrency(portfolioValue.subtract(initialCash));
            ((TextView) mainActivity.findViewById(R.id.portfolioValue)).setText(formatCurrency(portfolioValue));
            TextView portfolioValuePerformanceText = (TextView) mainActivity.findViewById(R.id.portfolioValuePerformance);
            boolean positive = isPositive(portfolioValueChange);
            portfolioValuePerformanceText.setText((positive ? " +" : " ") + formatCurrency(portfolioValueChange) + (positive ? " (+" : " (") + createPercentage(portfolioValueChange, initialCash) + ")");
            portfolioValuePerformanceText.setTextColor(positive ? Color.parseColor("#33CC33") : Color.RED);
        }
    }

    static boolean isPortfolioValueReady() {
        return positions.isPositionsValueReady();
    }

    static BigDecimal getPortfolioValue() {
        return roundCurrency(cash.get().add(positions.getPositionsValue()));
    }

    static void refresh() {
        positions.refresh();
        watchlist.refresh();
    }

    static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, new MathContext(20, RoundingMode.HALF_EVEN));
    }

    static String createPercentage(BigDecimal dividend, BigDecimal divisor) {
        return percentageFormat.format(divide(dividend, divisor));
    }

    static String formatCurrency(BigDecimal value) {
        return currencyFormat.format(value);
    }

    static String formatPercentage(BigDecimal value) {
        return percentageFormat.format(value);
    }

    static BigDecimal roundCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal roundPercentage(BigDecimal percentage) {
        return percentage.setScale(4, RoundingMode.HALF_EVEN);
    }

    static boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }

    private class Cash {
        private final BigDecimal initialCash = new BigDecimal(10000);
        private BigDecimal cash = new BigDecimal(prefs.getString("cash", initialCash.toPlainString()));

        private Cash() {
            ((TextView) mainActivity.findViewById(R.id.cash)).setText(getString());
        }

        private BigDecimal getInitial() {
            return initialCash;
        }

        private BigDecimal get() {
            return cash;
        }
        private String getString() {
            return formatCurrency(cash);
        }

        private boolean has(BigDecimal amount) {
            return cash.compareTo(amount) >= 0;
        }

        private void change(boolean add, BigDecimal amount) {
            cash = roundCurrency(add ? cash.add(amount) : cash.subtract(amount));
            write();
            ((TextView) mainActivity.findViewById(R.id.cash)).setText(getString());
        }

        private void write() {
            prefs.edit().putString("cash", cash.toPlainString()).apply();
        }
    }

    private class StockCollection {
        private final boolean positions;
        private final StockArrayAdapter adapter;
        private final ArrayList<Stock> stocks = new ArrayList<>();
        private int quotesReady = 0;

        private StockCollection(boolean positions, ListView listView) {
            this.positions = positions;
            adapter = new StockArrayAdapter(positions);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(mainActivity, StockInfoActivity.class);
                intent.putExtra("ticker", stocks.get(position).getTicker());
                mainActivity.startActivity(intent);
            });
            Scanner scanner = new Scanner(prefs.getString(positions ? "positions" : "watchlist", ""));
            while (scanner.hasNext()) {
                String ticker = scanner.next();
                stocks.add(new Stock(ticker, prefs.getInt(ticker, 0), new BigDecimal(prefs.getString(ticker + "_cost", "0"))));
            }
            scanner.close();
            if (positions) {
                mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
            }
            if (isNonEmpty()) {
                mainActivity.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
            }
            for (Stock stock : stocks) {
                getData(stock);
            }
        }

        private int size() {
            return stocks.size();
        }

        private boolean isNonEmpty() {
            return size() > 0;
        }

        private Stock getStock(String ticker) {
            for (Stock stock : stocks) {
                if (stock.getTicker().equals(ticker)) {
                    return stock;
                }
            }
            return null;
        }

        private boolean contains(String ticker) {
            return getStock(ticker) != null;
        }

        private int getShares(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getShares();
            }
            return 0;
        }

        private BigDecimal getCost(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getCost();
            }
            return null;
        }

        private BigDecimal getPreviousClose(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getPreviousClose();
            }
            return null;
        }

        private BigDecimal getQuote(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getQuote();
            }
            return null;
        }

        private BigDecimal getChange(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getChange();
            }
            return null;
        }

        private BigDecimal getPercentChange(String ticker) {
            Stock stock = getStock(ticker);
            if (stock != null) {
                return stock.getPercentChange();
            }
            return null;
        }

        private void addIfValid(String ticker) {
            if (!contains(ticker)) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    String result = APIHelper.get("https://api.polygon.io/v3/reference/tickers?ticker=" + ticker + "&apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                    try {
                        if (new JSONObject(result).getJSONArray("results").length() == 1) {
                            add(ticker);
                        }
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                });
            }
        }

        private void add(String ticker) {
            if (!contains(ticker)) {
                Stock stock = new Stock(ticker, prefs.getInt(ticker, 0), new BigDecimal(prefs.getString(ticker + "_cost", "0")));
                stocks.add(stock);
                writeTickers();
                getData(stock);
            }
        }

        private void add(Stock stock) {
            String ticker = stock.getTicker();
            if (!contains(ticker)) {
                stocks.add(stock);
                writeTickers();
                quotesReady++;
                adapter.notifyDataSetChanged();
            }
        }

        private Stock remove(String ticker) {
            if (contains(ticker)) {
                int numStocks = stocks.size();
                for (int i = 0; i < numStocks; i++) {
                    Stock stock = stocks.get(i);
                    if (stock.getTicker().equals(ticker)) {
                        stocks.remove(i);
                        writeTickers();
                        quotesReady--;
                        adapter.notifyDataSetChanged();
                        return stock;
                    }
                }
            }
            return null;
        }

        private void writeTickers() {
            StringBuilder tickers = new StringBuilder();
            for (Stock stock : stocks) {
                tickers.append(stock.getTicker()).append(" ");
            }
            prefs.edit().putString(positions ? "positions" : "watchlist", tickers.toString()).apply();
        }

        private Stock changePosition(boolean buy, String ticker, int shares, BigDecimal price, Stock watchlistStock) {
            Stock stock = getStock(ticker);
            // New or existing position
            if (buy && stock == null) {
                stock = new Stock(ticker, shares, price, watchlistStock.getPreviousClose(), watchlistStock.getQuote(), watchlistStock.getChange(), watchlistStock.getPercentChange());
                stocks.add(stock);
                writeTickers();
                writePosition(stock);
                quotesReady++;
                adapter.notifyDataSetChanged();
                mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
            } else if (stock != null) {
                int sharesOwned = stock.getShares();
                int newSharesOwned = buy ? sharesOwned + shares : sharesOwned - shares;
                if (newSharesOwned >= 0) {
                    stock.setShares(newSharesOwned);
                    adapter.notifyDataSetChanged();
                    if (buy) {
                        stock.setCost(roundCurrency(divide(new BigDecimal(sharesOwned).multiply(stock.getCost()).add(new BigDecimal(shares).multiply(price)), new BigDecimal(newSharesOwned))));
                    } else if (newSharesOwned == 0) {
                        stock.setCost(BigDecimal.ZERO);
                    }
                    writePosition(stock);
                    if (!buy && newSharesOwned == 0) {
                        remove(ticker);
                        mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
                        return stock;
                    }
                }
            }
            return null;
        }

        private void writePosition(Stock stock) {
            String ticker = stock.getTicker();
            int shares = stock.getShares();
            if (shares == 0) {
                prefs.edit().remove(ticker).remove(ticker + "_cost").apply();
            } else {
                prefs.edit().putInt(ticker, shares).putString(ticker + "_cost", stock.getCost().toPlainString()).apply();
            }
        }

        private boolean isPositionsValueReady() {
            return quotesReady == size();
        }

        private BigDecimal getPositionsValue() {
            BigDecimal value = BigDecimal.ZERO;
            for (Stock stock : stocks) {
                value = value.add(stock.getQuote().multiply(new BigDecimal(stock.getShares())));
            }
            return value;
        }

        private void refresh() {
            if (isNonEmpty()) {
                mainActivity.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
                quotesReady = 0;
                for (Stock stock : stocks) {
                    stock.setQuote(null);
                    stock.setChange(null);
                    stock.setPercentChange(null);
                    adapter.notifyDataSetChanged();
                    getData(stock);
                }
            }
        }

        private void getData(Stock stock) {
            Executors.newSingleThreadExecutor().execute(() -> {
                String result = APIHelper.get("https://api.polygon.io/v1/open-close/" + stock.getTicker() + "/" + APIHelper.getPreviousDay() + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                try {
                    stock.setPreviousClose(new BigDecimal(new JSONObject(result).getString("close")));
                } catch (JSONException e) {
                    Log.e("Exception", e.getMessage());
                }
                result = APIHelper.get("https://api.polygon.io/v2/aggs/ticker/" + stock.getTicker() + "/prev?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                try {
                    JSONObject jsonObject = new JSONObject(result).getJSONArray("results").getJSONObject(0);
                    BigDecimal close = new BigDecimal(jsonObject.getString("c"));
                    stock.setQuote(close);
                    quotesReady++;
                    BigDecimal previousClose = stock.getPreviousClose();
                    if (previousClose != null) {
                        BigDecimal change = close.subtract(previousClose);
                        stock.setChange(roundCurrency(change));
                        stock.setPercentChange(roundPercentage(divide(change, previousClose)));
                    }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        adapter.notifyDataSetChanged();
                        mainActivity.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(View.GONE);
                        if (positions) {
                            showPortfolioValueIfReady();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("Exception", e.getMessage());
                }
            });
        }

        private class StockArrayAdapter extends ArrayAdapter<Stock> {
            private final boolean positions;
            private final LayoutInflater layoutInflater = LayoutInflater.from(mainActivity);

            StockArrayAdapter(boolean positions) {
                super(mainActivity, positions ? R.layout.position_row : R.layout.watchlist_row, stocks);
                this.positions = positions;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView[] textViews;
                if (convertView == null) {
                    convertView = layoutInflater.inflate(positions ? R.layout.position_row : R.layout.watchlist_row, null);
                    textViews = new TextView[positions ? 4 : 3];
                    textViews[0] = convertView.findViewById(R.id.ticker);
                    textViews[1] = convertView.findViewById(R.id.quote);
                    textViews[2] = convertView.findViewById(R.id.percentChange);
                    if (positions) {
                        textViews[3] = convertView.findViewById(R.id.shares);
                    }
                    convertView.setTag(textViews);
                } else {
                    textViews = (TextView[]) convertView.getTag();
                }
                Stock stock = getItem(position);
                textViews[0].setText(stock.getTicker());
                BigDecimal quote = stock.getQuote();
                textViews[1].setText(quote == null ? "" : formatCurrency(stock.getQuote()));
                BigDecimal percentChange = stock.getPercentChange();
                if (percentChange == null) {
                    textViews[2].setText("");
                } else {
                    boolean percentChangePositive = isPositive(percentChange);
                    textViews[2].setText((percentChangePositive ? "+" : "") + formatPercentage(percentChange));
                    textViews[2].setTextColor(percentChangePositive ? Color.parseColor("#33CC33") : Color.RED);
                }
                if (positions) {
                    textViews[3].setText(stock.getShares() + " shares");
                }
                return convertView;
            }
        }
    }
}