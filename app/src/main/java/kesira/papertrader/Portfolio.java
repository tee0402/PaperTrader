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
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Portfolio {
    private final BigDecimal initialCash = new BigDecimal(10000);
    private static SharedPreferences prefs;
    private static BigDecimal cash;
    private static StockCollection positions;
    private static StockCollection watchlist;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DecimalFormat percentageFormat = new DecimalFormat("0.00%");

    Portfolio(MainActivity mainActivity, ListView positionsView, ListView watchlistView) {
        prefs = mainActivity.getSharedPreferences("Save", Context.MODE_PRIVATE);
        cash = new BigDecimal(prefs.getString("cash", initialCash.toPlainString()));
        positions = new StockCollection(true, mainActivity, positionsView);
        watchlist = new StockCollection(false, mainActivity, watchlistView);
    }

    static BigDecimal getCash() {
        return cash;
    }
    static String getCashString() {
        return formatCurrency(cash);
    }

    static boolean containsPositions() {
        return positions.size() > 0;
    }

    static boolean containsWatchlist() {
        return watchlist.size() > 0;
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

    static void add(String ticker) {
        if (!watchlist.contains(ticker) && !positions.contains(ticker)) {
            watchlist.addIfValid(ticker);
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
        if (buy && cash.compareTo(total) >= 0) {
            Stock stock = watchlist.remove(ticker);
            positions.changePosition(true, ticker, shares, price, stock);
            cash = roundCurrency(cash.subtract(total));
            writeCash();
        } else if (!buy && shares <= sharesOwned) {
            Stock stock = positions.changePosition(false, ticker, shares, price, null);
            if (shares == sharesOwned) {
                if (stock != null) {
                    watchlist.add(stock);
                }
            }
            cash = roundCurrency(cash.add(total));
            writeCash();
        }
    }

    private static void writeCash() {
        prefs.edit().putString("cash", cash.toPlainString()).apply();
    }

    static void refresh() {
        positions.refresh();
        watchlist.refresh();
    }

    static boolean valueReady() {
        return positions.ready();
    }

    static BigDecimal getValue() {
        return roundCurrency(cash.add(positions.getPositionsValue()));
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

    private class StockCollection {
        private final boolean positions;
        private final MainActivity mainActivity;
        private final StockArrayAdapter adapter;
        private final Set<String> stockSet;
        private final ArrayList<Stock> stocks = new ArrayList<>();
        private int quotesReady = 0;

        private StockCollection(boolean positions, MainActivity mainActivity, ListView listView) {
            this.positions = positions;
            this.mainActivity = mainActivity;
            adapter = new StockArrayAdapter(positions);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((adapterView, view, i, l) -> {
                Intent intent = new Intent(mainActivity, StockInfoActivity.class);
                intent.putExtra("ticker", stocks.get(i).getTicker());
                mainActivity.startActivity(intent);
            });
            stockSet = new LinkedHashSet<>(prefs.getStringSet(positions ? "positions" : "watchlist", new LinkedHashSet<>()));
            showPortfolioValueIfReady();
            for (String ticker : stockSet) {
                Stock stock = new Stock(ticker, prefs.getInt(ticker, 0), new BigDecimal(prefs.getString(ticker + "_cost", "0")));
                stocks.add(stock);
                getData(stock);
            }
        }

        private int size() {
            return stockSet.size();
        }

        private boolean contains(String ticker) {
            return stockSet.contains(ticker);
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
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    String result = APIHelper.get("https://api.polygon.io/v3/reference/tickers?ticker=" + ticker + "&apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
                    try {
                        if (new JSONObject(result).getJSONArray("results").length() == 1) {
                            stockSet.add(ticker);
                            writeTickers();
                            Stock stock = new Stock(ticker, prefs.getInt(ticker, 0), new BigDecimal(prefs.getString(ticker + "_cost", "0")));
                            stocks.add(stock);
                            getData(stock);
                        }
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                });
            }
        }

        private void add(Stock stock) {
            String ticker = stock.getTicker();
            if (!contains(ticker)) {
                stockSet.add(ticker);
                writeTickers();
                stocks.add(stock);
                quotesReady++;
            }
        }

        private Stock remove(String ticker) {
            if (contains(ticker)) {
                stockSet.remove(ticker);
                writeTickers();
                int numStocks = stocks.size();
                for (int i = 0; i < numStocks; i++) {
                    Stock stock = stocks.get(i);
                    if (stock.getTicker().equals(ticker)) {
                        stocks.remove(i);
                        quotesReady--;
                        adapter.notifyDataSetChanged();
                        return stock;
                    }
                }
            }
            return null;
        }

        private void writeTickers() {
            prefs.edit().putStringSet(positions ? "positions" : "watchlist", stockSet).apply();
        }

        private Stock getStock(String ticker) {
            for (Stock stock : stocks) {
                if (stock.getTicker().equals(ticker)) {
                    return stock;
                }
            }
            return null;
        }

        private Stock changePosition(boolean buy, String ticker, int shares, BigDecimal price, Stock watchlistStock) {
            Stock stock = getStock(ticker);
            // New or existing position
            if (buy && stock == null) {
                stockSet.add(ticker);
                writeTickers();
                stock = new Stock(ticker, shares, price, watchlistStock.getPreviousClose(), watchlistStock.getQuote(), watchlistStock.getChange(), watchlistStock.getPercentChange());
                stocks.add(stock);
                writePosition(stock);
                quotesReady++;
            } else if (stock != null) {
                int sharesOwned = stock.getShares();
                int newSharesOwned = buy ? sharesOwned + shares : sharesOwned - shares;
                if (newSharesOwned >= 0) {
                    stock.setShares(newSharesOwned);
                    if (buy) {
                        stock.setCost(roundCurrency(divide(new BigDecimal(sharesOwned).multiply(stock.getCost()).add(new BigDecimal(shares).multiply(price)), new BigDecimal(newSharesOwned))));
                    } else if (newSharesOwned == 0) {
                        stock.setCost(BigDecimal.ZERO);
                    }
                    writePosition(stock);
                    if (!buy && newSharesOwned == 0) {
                        remove(ticker);
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

        private boolean ready() {
            return quotesReady == stockSet.size();
        }

        private BigDecimal getPositionsValue() {
            BigDecimal value = BigDecimal.ZERO;
            for (Stock stock : stocks) {
                value = value.add(stock.getQuote().multiply(new BigDecimal(stock.getShares())));
            }
            return value;
        }

        private void showPortfolioValueIfReady() {
            if (positions && ready()) {
                BigDecimal portfolioValue = roundCurrency(cash.add(getPositionsValue()));
                BigDecimal portfolioValueChange = roundCurrency(portfolioValue.subtract(initialCash));
                mainActivity.showPortfolioValue(formatCurrency(portfolioValue), formatCurrency(portfolioValueChange), createPercentage(portfolioValueChange, initialCash), portfolioValueChange.compareTo(BigDecimal.ZERO) >= 0);
            }
        }

        private void refresh() {
            if (size() > 0) {
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
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                String result = APIHelper.get("https://api.polygon.io/v1/open-close/" + stock.getTicker() + "/" + APIHelper.subToday(Calendar.DAY_OF_WEEK, 1) + "?apiKey=lTkAIOnwJ9vpjDvqYAF0RWt9yMkhD0up");
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
                    handler.post(() -> {
                        adapter.notifyDataSetChanged();
                        mainActivity.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(View.GONE);
                        showPortfolioValueIfReady();
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
                    boolean percentChangePositive = percentChange.compareTo(BigDecimal.ZERO) >= 0;
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