package kesira.papertrader;

import android.graphics.Color;
import android.os.Bundle;
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

import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class Portfolio {
    private static final Portfolio portfolio = new Portfolio();
    private MainFragment mainFragment;
    private MainActivity activity;
    private final BigDecimal initialCash = new BigDecimal(10000);
    private Cash cash;
    private StockCollection positions;
    private StockCollection watchlist;
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DecimalFormat simpleCurrencyFormat = new DecimalFormat("0.00");
    private final DecimalFormat percentageFormat = new DecimalFormat("0.00%");
    private final DecimalFormat simplePercentageFormat = new DecimalFormat("0.0000");
    private DocumentReference userDocRef;
    private CollectionReference historyCollectionRef;

    private Portfolio() {}
    static Portfolio getInstance() {
        return portfolio;
    }
    @SuppressWarnings("unchecked")
    void initialize(MainFragment mainFragment, ListView positionsView, ListView watchlistView) {
        this.mainFragment = mainFragment;
        this.activity = (MainActivity) mainFragment.requireActivity();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInAnonymously().addOnCompleteListener(signInTask -> {
            if (signInTask.isSuccessful()) {
                String uId = mAuth.getUid();
                if (uId != null) {
                    userDocRef = FirebaseFirestore.getInstance().collection("users").document(uId);
                    historyCollectionRef = userDocRef.collection("history");
                    userDocRef.get().addOnCompleteListener(docTask -> {
                        if (docTask.isSuccessful()) {
                            DocumentSnapshot userDoc = docTask.getResult();
                            DateFormat isoDateFormat = APIHelper.getISODateFormat();
                            boolean exists = userDoc.exists();
                            cash = new Cash(exists ? userDoc.getString("cash") : initialCash.toPlainString());
                            List<Date> datesList = exists ?
                                    ((List<String>) Objects.requireNonNull(userDoc.get("dates"))).stream().map(date -> {
                                        try {
                                            return isoDateFormat.parse(date);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                            return null;
                                        }
                                    }).collect(Collectors.toList()) :
                                    new ArrayList<>();
                            List<Float> portfolioValuesList = exists ?
                                    ((List<String>) Objects.requireNonNull(userDoc.get("portfolioValues"))).stream().map(Float::parseFloat).collect(Collectors.toList()) :
                                    new ArrayList<>();
                            List<String> positionsList = exists ? (List<String>) userDoc.get("positions") : new ArrayList<>();
                            Map<String, String> positionsShares = exists ? (Map<String, String>) userDoc.get("positionsShares") : new HashMap<>();
                            Map<String, String> positionsCost = exists ? (Map<String, String>) userDoc.get("positionsCost") : new HashMap<>();
                            List<String> watchlistList = exists ? (List<String>) userDoc.get("watchlist") : new ArrayList<>();
                            if (!exists) {
                                Map<String, Object> user = new HashMap<>();
                                user.put("cash", cash.getPlainString());
                                datesList.add(new Date());
                                user.put("dates", datesList.stream().map(isoDateFormat::format).collect(Collectors.toList()));
                                portfolioValuesList.add(initialCash.floatValue());
                                user.put("portfolioValues", portfolioValuesList.stream().map(String::valueOf).collect(Collectors.toList()));
                                user.put("positions", positionsList);
                                user.put("positionsShares", positionsShares);
                                user.put("positionsCost", positionsCost);
                                user.put("watchlist", watchlistList);
                                userDocRef.set(user);
                            }
                            positions = new StockCollection(true, positionsView, positionsList, positionsShares, positionsCost);
                            if (positions.isEmpty()) {
                                mainFragment.showPortfolioValueIfReady();
                            }
                            watchlist = new StockCollection(false, watchlistView, watchlistList, null, null);
                            mainFragment.initializeChartData(datesList, portfolioValuesList);
                        } else {
                            Toast.makeText(activity, "Document get failed", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(activity, "uid not found", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(activity, "Anonymous sign-in failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    void queryHistory(List<QueryDocumentSnapshot> result, ArrayAdapter<QueryDocumentSnapshot> adapter, String ticker, View view, View showAll) {
        Query query = ticker == null ? historyCollectionRef : historyCollectionRef.whereEqualTo("ticker", ticker);
        boolean limit = view != null;
        query = limit ? query.limit(6) : query;
        query.orderBy("date", Query.Direction.DESCENDING).get().addOnCompleteListener(historyTask -> {
            if (historyTask.isSuccessful()) {
                QuerySnapshot history = historyTask.getResult();
                for (QueryDocumentSnapshot event : history) {
                    result.add(event);
                    if (limit && result.size() == 5) {
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
                if (limit) {
                    view.setVisibility(history.isEmpty() ? View.GONE : View.VISIBLE);
                    showAll.setVisibility(history.size() > 5 ? View.VISIBLE : View.GONE);
                }
            } else {
                Toast.makeText(activity, "History query failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    void startStockInfoFragment(Bundle bundle) {
        if (bundle.containsKey("quote") || getStock(bundle.getString("ticker")).getQuote() != null) {
            activity.getSupportFragmentManager().beginTransaction()
                    .hide(mainFragment)
                    .add(R.id.fragmentContainerView, StockInfoFragment.class, bundle)
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
    }

    boolean inPositions(String ticker) {
        return positions.contains(ticker);
    }
    boolean inWatchlist(String ticker) {
        return watchlist.contains(ticker);
    }
    boolean inPortfolio(String ticker) {
        return positions.contains(ticker) || watchlist.contains(ticker);
    }

    BigDecimal getCash() {
        return cash.get();
    }
    String getCashString() {
        return cash.getString();
    }
    Stock getStock(String ticker) {
        if (inPositions(ticker)) {
            return positions.getStock(ticker);
        } else if (inWatchlist(ticker)) {
            return watchlist.getStock(ticker);
        }
        return null;
    }

    void addIfValid(String ticker) {
        if (!inPortfolio(ticker)) {
            watchlist.addIfValid(ticker);
        }
    }
    void add(String ticker, BigDecimal previousClose, BigDecimal quote, BigDecimal change, BigDecimal percentChange) {
        if (!inPortfolio(ticker)) {
            watchlist.add(ticker, previousClose, quote, change, percentChange);
        }
    }
    void remove(String ticker) {
        if (inWatchlist(ticker)) {
            watchlist.remove(ticker);
        }
    }

    // Stock must be in portfolio
    void changePosition(boolean buy, String ticker, int shares, BigDecimal price, StockInfoFragment stockInfoFragment) {
        BigDecimal total = new BigDecimal(shares).multiply(price);
        int sharesOwned = getStock(ticker).getShares();
        if (buy && cash.has(total)) {
            Stock watchlistStock = watchlist.remove(ticker);
            positions.changePosition(true, ticker, shares, price, watchlistStock, stockInfoFragment);
            cash.change(false, total);
        } else if (!buy && shares <= sharesOwned) {
            Stock stock = positions.changePosition(false, ticker, shares, price, null, stockInfoFragment);
            if (stock != null) {
                watchlist.add(stock);
            }
            cash.change(true, total);
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
    BigDecimal percentageToDecimal(BigDecimal percentage) {
        return divide(percentage, new BigDecimal(100));
    }
    BigDecimal roundCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
    BigDecimal roundPercentage(BigDecimal percentage) {
        return percentage.setScale(4, RoundingMode.HALF_EVEN);
    }

    String formatNumber(int value) {
        return numberFormat.format(value);
    }
    String formatNumber(BigDecimal value) {
        return numberFormat.format(value);
    }
    String formatCurrency(BigDecimal value) {
        return currencyFormat.format(value);
    }
    String formatSimpleCurrency(BigDecimal value) {
        return simpleCurrencyFormat.format(value);
    }
    String formatCurrencyWithoutRounding(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 2 ? formatCurrency(value) : "$" + stripped.toPlainString();
    }
    String formatPercentage(BigDecimal value) {
        return percentageFormat.format(value);
    }
    String formatSimplePercentage(BigDecimal value) {
        return simplePercentageFormat.format(value);
    }
    String createPercentage(BigDecimal dividend, BigDecimal divisor) {
        return percentageFormat.format(divide(dividend, divisor));
    }

    boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }
    boolean isPositiveChange(BigDecimal initialValue, BigDecimal currentValue) {
        return currentValue.compareTo(initialValue) >= 0;
    }

    private class Cash {
        private BigDecimal cash;

        private Cash(String cash) {
            this.cash = new BigDecimal(cash);
            mainFragment.showCash(getString());
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
            mainFragment.showCash(getString());
        }

        private void write() {
            userDocRef.update("cash", getPlainString());
        }
    }

    private class StockCollection {
        private final boolean positions;
        private final StockArrayAdapter adapter;
        private final List<String> tickerList;
        private final Map<String, String> positionsShares;
        private final Map<String, String> positionsCost;
        private final List<Stock> stocks = new ArrayList<>();
        private final Map<String, Stock> tickerToStock = new HashMap<>();
        private int quotesReady = 0;

        private StockCollection(boolean positions, ListView listView, List<String> tickerList, Map<String, String> positionsShares, Map<String, String> positionsCost) {
            this.positions = positions;
            adapter = new StockArrayAdapter();
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                Bundle bundle = new Bundle();
                bundle.putString("ticker", stocks.get(position).getTicker());
                startStockInfoFragment(bundle);
            });
            this.tickerList = tickerList;
            this.positionsShares = positionsShares;
            this.positionsCost = positionsCost;
            if (positions) {
                for (String ticker : tickerList) {
                    Stock stock = new Stock(ticker, Integer.parseInt(Objects.requireNonNull(positionsShares.get(ticker))), new BigDecimal(positionsCost.get(ticker)));
                    stocks.add(stock);
                    tickerToStock.put(ticker, stock);
                }
                mainFragment.setPositionsVisibility(!isEmpty());
            } else {
                for (String ticker : tickerList) {
                    Stock stock = new Stock(ticker);
                    stocks.add(stock);
                    tickerToStock.put(ticker, stock);
                }
            }
            mainFragment.setProgressBarVisibility(positions, !isEmpty());
            for (Stock stock : stocks) {
                getData(stock, null);
            }
        }

        private boolean isEmpty() {
            return stocks.size() == 0;
        }

        private Stock getStock(String ticker) {
            return tickerToStock.get(ticker);
        }

        private boolean contains(String ticker) {
            return tickerToStock.containsKey(ticker);
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
                                tickerToStock.put(ticker, stock);
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
        private void add(String ticker, BigDecimal previousClose, BigDecimal quote, BigDecimal change, BigDecimal percentChange) {
            Stock stock = new Stock(ticker, previousClose, quote, change, percentChange);
            add(stock);
        }
        private void add(Stock stock) {
            String ticker = stock.getTicker();
            if (!contains(ticker)) {
                stocks.add(stock);
                tickerToStock.put(ticker, stock);
                write(true, ticker, null);
                quotesReady++;
                adapter.notifyDataSetChanged();
            }
        }

        private Stock remove(String ticker) {
            if (contains(ticker)) {
                Stock stock = tickerToStock.get(ticker);
                stocks.remove(stock);
                tickerToStock.remove(ticker);
                write(false, ticker, null);
                quotesReady--;
                adapter.notifyDataSetChanged();
                return stock;
            }
            return null;
        }

        private void write(boolean addOrOverwrite, String ticker, Stock stock) {
            if (positions) {
                if (addOrOverwrite) {
                    if (!tickerList.contains(ticker)) {
                        tickerList.add(ticker);
                    }
                    positionsShares.put(ticker, String.valueOf(stock.getShares()));
                    positionsCost.put(ticker, String.valueOf(stock.getCost()));
                } else {
                    tickerList.remove(ticker);
                    positionsShares.remove(ticker);
                    positionsCost.remove(ticker);
                }
                userDocRef.update("positions", tickerList, "positionsShares", positionsShares, "positionsCost", positionsCost);
            } else {
                if (addOrOverwrite) {
                    tickerList.add(ticker);
                } else {
                    tickerList.remove(ticker);
                }
                userDocRef.update("watchlist", tickerList);
            }
        }

        // Only used by positions, returns stock if removing to watchlist
        private Stock changePosition(boolean buy, String ticker, int shares, BigDecimal price, Stock watchlistStock, StockInfoFragment stockInfoFragment) {
            Stock stock = getStock(ticker);
            if (buy && stock == null) { // New position
                stock = new Stock(ticker, shares, price, watchlistStock.getPreviousClose(), watchlistStock.getQuote(), watchlistStock.getChange(), watchlistStock.getPercentChange());
                stocks.add(stock);
                tickerToStock.put(ticker, stock);
                write(true, ticker, stock);
                writeTrade(true, ticker, shares, price, stockInfoFragment);
                quotesReady++;
                adapter.notifyDataSetChanged();
                mainFragment.setPositionsVisibility(!isEmpty());
            } else if (stock != null) { // Removing to watchlist or overwriting position
                int sharesOwned = stock.getShares();
                int newSharesOwned = buy ? sharesOwned + shares : sharesOwned - shares;
                if (newSharesOwned >= 0) {
                    stock.setShares(newSharesOwned);
                    adapter.notifyDataSetChanged();
                    writeTrade(buy, ticker, shares, price, stockInfoFragment);
                    if (!buy && newSharesOwned == 0) { // Removing to watchlist
                        stock.setCost(BigDecimal.ZERO);
                        remove(ticker);
                        mainFragment.setPositionsVisibility(!isEmpty());
                        return stock;
                    } else { // Overwrite position
                        if (buy) {
                            stock.setCost(roundCurrency(divide(new BigDecimal(sharesOwned).multiply(stock.getCost()).add(new BigDecimal(shares).multiply(price)), new BigDecimal(newSharesOwned))));
                        }
                        write(true, ticker, stock);
                    }
                }
            }
            return null;
        }

        private void writeTrade(boolean buy, String ticker, int shares, BigDecimal price, StockInfoFragment stockInfoFragment) {
            Map<String, Object> trade = new HashMap<>();
            trade.put("buy", buy);
            trade.put("date", FieldValue.serverTimestamp());
            trade.put("price", String.valueOf(price));
            trade.put("shares", String.valueOf(shares));
            trade.put("ticker", ticker);
            historyCollectionRef.add(trade).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    stockInfoFragment.updateHistory();
                }
            });
        }

        private boolean isPositionsValueReady() {
            return quotesReady == stocks.size();
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
                    stock.setPercentChange(roundPercentage(percentageToDecimal(new BigDecimal(percentChange.substring(0, percentChange.length() - 1)))));
                    quotesReady++;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        adapter.notifyDataSetChanged();
                        mainFragment.setProgressBarVisibility(positions, false);
                        if (positions) {
                            mainFragment.showPortfolioValueIfReady();
                        }
                    });
                } catch (JSONException e) {
                    Log.e("Exception", e.getMessage());
                }
            });
        }

        private class StockArrayAdapter extends ArrayAdapter<Stock> {
            private final LayoutInflater layoutInflater = LayoutInflater.from(activity);

            StockArrayAdapter() {
                super(activity, positions ? R.layout.position_row : R.layout.watchlist_row, stocks);
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
                textViews[1].setText(quote == null ? "" : formatCurrency(quote));
                BigDecimal percentChange = stock.getPercentChange();
                if (percentChange == null) {
                    textViews[2].setText("");
                } else {
                    boolean percentChangePositive = isPositive(percentChange);
                    textViews[2].setText((percentChangePositive ? "+" : "") + formatPercentage(percentChange));
                    textViews[2].setTextColor(percentChangePositive ? Color.parseColor("#33CC33") : Color.RED);
                }
                if (positions) {
                    textViews[3].setText(portfolio.formatNumber(stock.getShares()) + " shares");
                }
                return convertView;
            }
        }
    }
}