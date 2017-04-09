package kesira.papertrader;

class CustomRow {

    private final String ticker;
    private final String quote;
    private final String percentChange;

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