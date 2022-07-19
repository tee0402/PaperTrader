package kesira.papertrader;

import android.content.Intent;
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
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class Portfolio {
    private static final Portfolio portfolio = new Portfolio();
    private MainActivity mainActivity;
    private final BigDecimal initialCash = new BigDecimal(10000);
    private Cash cash;
    private StockCollection positions;
    private StockCollection watchlist;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DecimalFormat simpleCurrencyFormat = new DecimalFormat("0.00");
    private final DecimalFormat percentageFormat = new DecimalFormat("0.00%");
    private DocumentReference userDoc;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy", Locale.ENGLISH);
    private List<Date> datesList;
    private List<Float> portfolioValuesList;
    private List<String> positionsList;
    private Map<String, String> positionsShares;
    private Map<String, String> positionsCost;
    private List<String> watchlistList;

    private Portfolio() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
    }

    static Portfolio getInstance() {
        return portfolio;
    }

    void initialize(MainActivity mainActivity, ListView positionsView, ListView watchlistView) {
        this.mainActivity = mainActivity;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously().addOnCompleteListener(mainActivity, task -> {
            if (task.isSuccessful()) {
                String uId = mAuth.getUid();
                if (uId != null) {
                    userDoc = FirebaseFirestore.getInstance().collection("users").document(uId);
                    userDoc.get().addOnCompleteListener(docTask -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = docTask.getResult();
                            if (document.exists()) {
                                getExistingFirestore(document);
                            } else {
                                setInitialFirestore();
                            }
                            positions = new StockCollection(true, positionsView);
                            watchlist = new StockCollection(false, watchlistView);
                            if (!containsPositions()) {
                                showPortfolioValueIfReady();
                            }
                            mainActivity.initializeChartData();
                        } else {
                            Toast.makeText(mainActivity, "Document get failed", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(mainActivity, "uid not found", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(mainActivity, "Anonymous sign-in failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void getExistingFirestore(DocumentSnapshot document) {
        cash = new Cash((String) document.get("cash"));
        List<String> dates = (List<String>) document.get("dates");
        assert dates != null;
        datesList = dates.stream().map(date -> {
            try {
                return dateFormat.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
        List<String> portfolioValues = (List<String>) document.get("portfolioValues");
        assert portfolioValues != null;
        portfolioValuesList = portfolioValues.stream().map(Float::parseFloat).collect(Collectors.toList());
        positionsList = (List<String>) document.get("positions");
        positionsShares = (Map<String, String>) document.get("positionsShares");
        positionsCost = (Map<String, String>) document.get("positionsCost");
        watchlistList = (List<String>) document.get("watchlist");
    }

    private void setInitialFirestore() {
        Map<String, Object> user = new HashMap<>();
        cash = new Cash(initialCash.toPlainString());
        user.put("cash", cash.getPlainString());
        datesList = new ArrayList<>();
        datesList.add(new Date());
        user.put("dates", datesList.stream().map(dateFormat::format).collect(Collectors.toList()));
        portfolioValuesList = new ArrayList<>();
        portfolioValuesList.add(initialCash.floatValue());
        user.put("portfolioValues", portfolioValuesList.stream().map(String::valueOf).collect(Collectors.toList()));
        positionsList = new ArrayList<>();
        user.put("positions", positionsList);
        positionsShares = new HashMap<>();
        user.put("positionsShares", positionsShares);
        positionsCost = new HashMap<>();
        user.put("positionsCost", positionsCost);
        watchlistList = new ArrayList<>();
        user.put("watchlist", watchlistList);
        userDoc.set(user);
    }

    BigDecimal getCash() {
        return cash.get();
    }
    String getCashString() {
        return cash.getString();
    }

    List<Date> getDatesList() {
        return datesList;
    }
    List<Float> getPortfolioValuesList() {
        return portfolioValuesList;
    }

    boolean containsPositions() {
        return positions.isNonEmpty();
    }

    boolean inPositions(String ticker) {
        return positions.contains(ticker);
    }

    boolean inWatchlist(String ticker) {
        return watchlist.contains(ticker);
    }

    int getShares(String ticker) {
        return positions.getShares(ticker);
    }

    BigDecimal getCost(String ticker) {
        return positions.getCost(ticker);
    }

    BigDecimal getPreviousClose(String ticker) {
        if (inPositions(ticker)) {
            return positions.getPreviousClose(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getPreviousClose(ticker);
        }
        return null;
    }

    BigDecimal getQuote(String ticker) {
        if (inPositions(ticker)) {
            return positions.getQuote(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getQuote(ticker);
        }
        return null;
    }

    BigDecimal getChange(String ticker) {
        if (inPositions(ticker)) {
            return positions.getChange(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getChange(ticker);
        }
        return null;
    }

    BigDecimal getPercentChange(String ticker) {
        if (inPositions(ticker)) {
            return positions.getPercentChange(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getPercentChange(ticker);
        }
        return null;
    }

    void addIfValid(String ticker) {
        if (!watchlist.contains(ticker) && !positions.contains(ticker)) {
            watchlist.addIfValid(ticker);
        }
    }

    void add(String ticker) {
        if (!watchlist.contains(ticker) && !positions.contains(ticker)) {
            watchlist.add(ticker);
        }
    }

    void remove(String ticker) {
        if (watchlist.contains(ticker)) {
            watchlist.remove(ticker);
        }
    }

    void changePosition(boolean buy, String ticker, int shares, BigDecimal price) {
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
            ((TextView) mainActivity.findViewById(R.id.portfolioValue)).setText(formatCurrency(portfolioValue));
            mainActivity.showPortfolioValuePerformance(null);
        }
    }

    boolean isPortfolioValueReady() {
        return positions.isPositionsValueReady();
    }

    BigDecimal getPortfolioValue() {
        return roundCurrency(cash.get().add(positions.getPositionsValue()));
    }

    BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, new MathContext(20, RoundingMode.HALF_EVEN));
    }

    String createPercentage(BigDecimal dividend, BigDecimal divisor) {
        return percentageFormat.format(divide(dividend, divisor));
    }

    String formatCurrency(BigDecimal value) {
        return currencyFormat.format(value);
    }

    String formatSimpleCurrency(BigDecimal value) {
        return simpleCurrencyFormat.format(value);
    }

    String formatPercentage(BigDecimal value) {
        return percentageFormat.format(value);
    }

    BigDecimal roundCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    BigDecimal roundPercentage(BigDecimal percentage) {
        return percentage.setScale(4, RoundingMode.HALF_EVEN);
    }

    boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }

    boolean isPositiveChange(BigDecimal currentValue, BigDecimal initialValue) {
        return currentValue.compareTo(initialValue) >= 0;
    }

    private class Cash {
        private BigDecimal cash;

        private Cash(String cash) {
            this.cash = new BigDecimal(cash);
            ((TextView) mainActivity.findViewById(R.id.cash)).setText(getString());
        }

        private BigDecimal get() {
            return cash;
        }
        private String getString() {
            return formatCurrency(cash);
        }
        private String getPlainString() {
            return cash.toPlainString();
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
            userDoc.update("cash", getPlainString());
        }
    }

    private class StockCollection {
        private final boolean positions;
        private final StockArrayAdapter adapter;
        private final List<Stock> stocks = new ArrayList<>();
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
            if (positions) {
                for (String ticker : positionsList) {
                    String shares = positionsShares.get(ticker);
                    assert shares != null;
                    String cost = positionsCost.get(ticker);
                    assert cost != null;
                    stocks.add(new Stock(ticker, Integer.parseInt(shares), new BigDecimal(cost)));
                }
                mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
            } else {
                for (String ticker : watchlistList) {
                    stocks.add(new Stock(ticker));
                }
            }
            if (isNonEmpty()) {
                mainActivity.findViewById(positions ? R.id.progressBarPositions : R.id.progressBarWatchlist).setVisibility(View.VISIBLE);
            }
            for (Stock stock : stocks) {
                getData(stock, null);
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

        // Only used by watchlist
        private void addIfValid(String ticker) {
            if (!contains(ticker)) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    String result = APIHelper.get("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + ticker + "&apikey=1275");
                    try {
                        JSONObject jsonObject = new JSONObject(result).getJSONObject("Global Quote");
                        if (jsonObject.length() > 0) {
                            if (!contains(ticker)) {
                                Stock stock = new Stock(ticker);
                                stocks.add(stock);
                                write(true, ticker, null);
                                getData(stock, result);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("Exception", e.getMessage());
                    }
                });
            }
        }

        // Only used by watchlist
        private void add(String ticker) {
            if (!contains(ticker)) {
                Stock stock = new Stock(ticker);
                stocks.add(stock);
                write(true, ticker, null);
                getData(stock, null);
            }
        }

        // Only used by watchlist
        private void add(Stock stock) {
            String ticker = stock.getTicker();
            if (!contains(ticker)) {
                stocks.add(stock);
                write(true, ticker, null);
                quotesReady++;
                adapter.notifyDataSetChanged();
            }
        }

        private Stock remove(String ticker) {
            if (contains(ticker)) {
                for (int i = 0; i < stocks.size(); i++) {
                    Stock stock = stocks.get(i);
                    if (stock.getTicker().equals(ticker)) {
                        stocks.remove(i);
                        write(false, ticker, null);
                        quotesReady--;
                        adapter.notifyDataSetChanged();
                        return stock;
                    }
                }
            }
            return null;
        }

        private void write(boolean add, String ticker, Stock stock) {
            if (positions) {
                if (add) {
                    if (!positionsList.contains(ticker)) {
                        positionsList.add(ticker);
                    }
                    positionsShares.put(ticker, String.valueOf(stock.getShares()));
                    positionsCost.put(ticker, String.valueOf(stock.getCost()));
                } else {
                    positionsList.remove(ticker);
                    positionsShares.remove(ticker);
                    positionsCost.remove(ticker);
                }
                userDoc.update("positions", positionsList, "positionsShares", positionsShares, "positionsCost", positionsCost);
            } else {
                if (add) {
                    watchlistList.add(ticker);
                } else {
                    watchlistList.remove(ticker);
                }
                userDoc.update("watchlist", watchlistList);
            }
        }

        // Only used by positions, returns stock if removing to watchlist
        private Stock changePosition(boolean buy, String ticker, int shares, BigDecimal price, Stock watchlistStock) {
            Stock stock = getStock(ticker);
            if (buy && stock == null) { // New position
                if (watchlistStock == null) { // Stock not in watchlist
                    stock = new Stock(ticker, shares, price);
                } else { // Stock in watchlist
                    stock = new Stock(ticker, shares, price, watchlistStock.getPreviousClose(), watchlistStock.getQuote(), watchlistStock.getChange(), watchlistStock.getPercentChange());
                }
                stocks.add(stock);
                write(true, ticker, stock);
                if (watchlistStock == null) {
                    getData(stock, null);
                } else {
                    quotesReady++;
                    adapter.notifyDataSetChanged();
                }
                mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
            } else if (stock != null) { // Overwriting or removing existing position
                int sharesOwned = stock.getShares();
                int newSharesOwned = buy ? sharesOwned + shares : sharesOwned - shares;
                if (newSharesOwned >= 0) {
                    stock.setShares(newSharesOwned);
                    adapter.notifyDataSetChanged();
                    if (buy) {
                        stock.setCost(roundCurrency(divide(new BigDecimal(sharesOwned).multiply(stock.getCost()).add(new BigDecimal(shares).multiply(price)), new BigDecimal(newSharesOwned))));
                        write(true, ticker, stock);
                    } else if (newSharesOwned == 0) {
                        stock.setCost(BigDecimal.ZERO);
                        remove(ticker);
                        mainActivity.findViewById(R.id.positions).setVisibility(isNonEmpty() ? View.VISIBLE : View.GONE);
                        return stock;
                    } else {
                        write(true, ticker, stock);
                    }
                }
            }
            return null;
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

        private void getData(Stock stock, String prevResult) {
            Executors.newSingleThreadExecutor().execute(() -> {
                String result = prevResult == null ? APIHelper.get("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + stock.getTicker() + "&apikey=1275") : prevResult;
                try {
                    JSONObject jsonObject = new JSONObject(result).getJSONObject("Global Quote");
                    stock.setPreviousClose(roundCurrency(new BigDecimal(jsonObject.getString("08. previous close"))));
                    stock.setQuote(roundCurrency(new BigDecimal(jsonObject.getString("05. price"))));
                    stock.setChange(roundCurrency(new BigDecimal(jsonObject.getString("09. change"))));
                    String percentChange = jsonObject.getString("10. change percent");
                    stock.setPercentChange(roundPercentage(divide(new BigDecimal(percentChange.substring(0, percentChange.length() - 1)), new BigDecimal(100))));
                    quotesReady++;
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