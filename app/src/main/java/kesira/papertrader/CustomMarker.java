package kesira.papertrader;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.math.BigDecimal;
import java.util.ArrayList;

class CustomMarker extends MarkerView {
    private final TextView textView;
    private ArrayList<String> markerDates;
    private final Portfolio portfolio = Portfolio.getInstance();

    CustomMarker(Context context) {
        this(context, R.layout.chart_marker);
    }

    private CustomMarker(Context context, int layoutResource) {
        super(context, layoutResource);
        textView = findViewById(R.id.marker);
    }

    void setMarkerDates(ArrayList<String> markerDates) {
        this.markerDates = markerDates;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        textView.setText(portfolio.formatSimpleCurrency(BigDecimal.valueOf(e.getY())) + "  " + markerDates.get((int) e.getX()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -(2 * getHeight()));
    }
}