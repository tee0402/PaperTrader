package kesira.papertrader;

class CustomRow {

    private String ticker;
    private String quote;
    private String percentChange;

    CustomRow(String ticker, String quote, String percentChange) {
        this.ticker = ticker;
        this.quote = quote;
        this.percentChange = percentChange;
    }

    String getTicker() {
        return ticker;
    }

    String getQuote() {
        return quote;
    }

    String getPercentChange() {
        return percentChange;
    }
}