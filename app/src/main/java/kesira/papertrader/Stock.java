package kesira.papertrader;

import java.math.BigDecimal;

class Stock {
    private final String ticker;
    private int shares;
    private BigDecimal cost;
    private BigDecimal quote;
    private BigDecimal percentChange;

    Stock(String ticker, int shares, BigDecimal cost) {
        this.ticker = ticker;
        this.shares = shares;
        this.cost = cost;
    }

    Stock(String ticker, int shares, BigDecimal cost, BigDecimal quote, BigDecimal percentChange) {
        this.ticker = ticker;
        this.shares = shares;
        this.cost = cost;
        this.quote = quote;
        this.percentChange = percentChange;
    }

    int getShares() {
        return shares;
    }
    void setShares(int shares) {
        this.shares = shares;
    }

    BigDecimal getCost() {
        return cost;
    }
    void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    String getTicker() {
        return ticker;
    }

    BigDecimal getQuote() {
        return quote;
    }
    void setQuote(BigDecimal quote) {
        this.quote = quote;
    }

    BigDecimal getPercentChange() {
        return percentChange;
    }
    void setPercentChange(BigDecimal percentChange) {
        this.percentChange = percentChange;
    }
}