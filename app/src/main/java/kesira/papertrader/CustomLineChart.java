package kesira.papertrader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;

public class CustomLineChart extends LineChart {
    public CustomLineChart(Context context) {
        super(context);
    }
    public CustomLineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CustomLineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void initialize(int height) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = height;
        setLayoutParams(layoutParams);
        setNoDataText("Loading...");
        setDragYEnabled(false);
        setScaleYEnabled(false);
        setDrawGridBackground(true);
        getLegend().setEnabled(false);
        getAxisRight().setEnabled(false);
        getDescription().setEnabled(false);
        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(4, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        performClick();
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.onTouchEvent(event);
    }
    @Override
    public boolean performClick() {
        return super.performClick();
    }
}