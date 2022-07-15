package kesira.papertrader;

import java.math.BigDecimal;

class Stock {
    private final String ticker;
    private int shares;
    private BigDecimal cost;
    private BigDecimal previousClose;
    private BigDecimal quote;
    private BigDecimal change;
    private BigDecimal percentChange;

    Stock(String ticker) {
        this.ticker = ticker;
        this.shares = 0;
        this.cost = new BigDecimal(0);
    }

    Stock(String ticker, int shares, BigDecimal cost) {
        this.ticker = ticker;
        this.shares = shares;
        this.cost = cost;
    }

    Stock(String ticker, int shares, BigDecimal cost, BigDecimal previousClose, BigDecimal quote, BigDecimal change, BigDecimal percentChange) {
        this.ticker = ticker;
        this.shares = shares;
        this.cost = cost;
        this.previousClose = previousClose;
        this.quote = quote;
        this.change = change;
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

    BigDecimal getPreviousClose() {
        return previousClose;
    }
    void setPreviousClose(BigDecimal previousClose) {
        this.previousClose = previousClose;
    }

    BigDecimal getChange() {
        return change;
    }
    void setChange(BigDecimal change) {
        this.change = change;
    }

    BigDecimal getPercentChange() {
        return percentChange;
    }
    void setPercentChange(BigDecimal percentChange) {
        this.percentChange = percentChange;
    }
}