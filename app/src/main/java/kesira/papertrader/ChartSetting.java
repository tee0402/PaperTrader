package kesira.papertrader;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.LineData;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.List;

class ChartSetting {
    private LineData lineData;
    private List<String> xAxisValues;
    private List<String> markerDates;
    private BigDecimal change;
    private BigDecimal percentChange;
    private LimitLine previousCloseLimitLine;
    private int initialIndex;
    private final BigDecimal open;
    private DateFormat xAxisFormat;

    ChartSetting(LineData lineData, List<String> xAxisValues, List<String> markerDates, BigDecimal change, BigDecimal percentChange, BigDecimal open) {
        this.lineData = lineData;
        this.xAxisValues = xAxisValues;
        this.markerDates = markerDates;
        this.change = change;
        this.percentChange = percentChange;
        this.open = open;
    }

    ChartSetting(int initialIndex, BigDecimal open, DateFormat xAxisFormat) {
        this.initialIndex = initialIndex;
        this.open = open;
        this.xAxisFormat = xAxisFormat;
    }

    LineData getLineData() {
        return lineData;
    }
    void setLineData(LineData lineData) {
        this.lineData = lineData;
    }

    List<String> getXAxisValues() {
        return xAxisValues;
    }

    List<String> getMarkerDates() {
        return markerDates;
    }

    BigDecimal getChange() {
        return change;
    }

    BigDecimal getPercentChange() {
        return percentChange;
    }

    LimitLine getPreviousCloseLimitLine() {
        return previousCloseLimitLine;
    }
    void setPreviousCloseLimitLine(LimitLine previousCloseLimitLine) {
        this.previousCloseLimitLine = previousCloseLimitLine;
    }

    int getInitialIndex() {
        return initialIndex;
    }

    BigDecimal getOpen() {
        return open;
    }

    DateFormat getXAxisFormat() {
        return xAxisFormat;
    }
}