/*
 * Copyright (C) 2015 Mikhail Goldenzweig
 * MIT Licence
 */
package de.goldenzweig.jimdostats;

import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.db.chart.view.XController;
import com.db.chart.view.YController;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.BaseEasingMethod;
import com.db.chart.view.animation.easing.quint.QuintEaseOut;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private static final int WEEK_DAYS = 7;
    private static final int MONTH_DAYS = 30;
    private static final long MILLISECONDS_IN_DAY = 86400000;

    private List<JimdoPerDayStatistics> mockSatats;
    private LineChartPresentation weekLineChartPresentation;
    private LineChartPresentation monthLineChartPresentation;
    private LineChartPresentation currentChartPresentation;

    //Animation Style
    private static BaseEasingMethod mCurrEasing = new QuintEaseOut();

    private LineChartView mLineChart;
    private RadioGroup mRadioGroup;
    private Handler mHandler;


    /**
     * inflate and redraw chart in separate thread
     */
    private final Runnable mInflateChartThread = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    inflateChart(currentChartPresentation);
                }
            }, 50);
        }
    };

    /**
     * enable radioGroup after chart animation is finished
     */
    private final Runnable mEnterEndAction = new Runnable() {
        @Override
        public void run() {
            setRadioGroupEnabled(true);
        }
    };

    /**
     * enables or disables all radioButtons in the group
     */
    private void setRadioGroupEnabled(boolean enabled) {
        if (mRadioGroup != null) {
            for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
                mRadioGroup.getChildAt(i).setEnabled(enabled);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLineChart = (LineChartView) findViewById(R.id.linechart);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);

        mHandler = new Handler();

        //prepare the data here to avoid unnecessary overhead while switching views
        mockSatats = generateMockStats(MONTH_DAYS);
        weekLineChartPresentation = prepareData(WEEK_DAYS);
        monthLineChartPresentation = prepareData(MONTH_DAYS);

        inflateChart(weekLineChartPresentation);
    }

    /**
     * RadioButton Group onClick listener.
     * Inflates the Line chart respectfully to the clicked RadioButton
     * @param view - clicked RadioButton
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_week:
                if (checked)
                    currentChartPresentation = weekLineChartPresentation;
                    break;
            case R.id.radio_month:
                if (checked)
                    currentChartPresentation = monthLineChartPresentation;
                break;
        }
        setRadioGroupEnabled(false);
        mInflateChartThread.run();
    }

    /**
     * Prepare data for the Line Chart
     * @param days - Number of Jimdo day usage statistics
     * @return LineChartPresentation object for Line Chart initialization
     */
    private LineChartPresentation prepareData(int days) {

        LineChartPresentation lcp = new LineChartPresentation();
        //Max value of visits or page views in the stats
        int maxValue = 0;

        //initialize the LineChartPresentation instance
        lcp.visitsArray = new float[days+1];
        lcp.pageViewsArray = new float[days+1];
        lcp.datesArray = new String[days+1];

        //first row is empty to avoid points being drawn directly on the y-axis
        lcp.visitsArray[0] = 0f;
        lcp.pageViewsArray[0] = 0f;
        lcp.datesArray[0] = "";

        for (int i = 0; i < days; i++) {

            JimdoPerDayStatistics stat = mockSatats.get(i);
            int visits = stat.getVisitCount();
            int pageViews = stat.getPageViewCount();

            if (visits > maxValue || pageViews > maxValue) {
                maxValue = Math.max(visits, pageViews);
            }

            lcp.visitsArray[i+1] = visits;
            lcp.pageViewsArray[i+1] = pageViews;

            // in month view add only every 4th date to avoid overfilling thy x-axis
            if (days == WEEK_DAYS) {
                lcp.datesArray[i+1] = stat.getShortDate();
            } else if ((i % 4) == 0) {
                lcp.datesArray[i+1] = stat.getShortDate();
            } else {
                lcp.datesArray[i+1] = "";
            }
        }

        //Round maxValue up to the closest 10
        lcp.maxValue = (int) Math.ceil(maxValue / 10d) * 10;
        //Calculate step so that we always have exactly 10 segments on the y-axis
        lcp.step = (int) Math.floor(lcp.maxValue / 10);

        return lcp;
    }


    /**
     * Inflates Line Chart with data, initializes the look and feel, shows the Line Chart.
     * @param lcp - LineChartPresentation instance.
     */
    private void inflateChart(LineChartPresentation lcp) {

        mLineChart.reset();

        //init and add "visits" line to the chart
        LineSet dataSet = new LineSet();
        dataSet.addPoints(lcp.datesArray, lcp.visitsArray);
        dataSet.setDots(true)
                .setDotsColor(this.getResources().getColor(R.color.line_visits_bg))
                .setDotsRadius(Tools.fromDpToPx(5))
                .setDotsStrokeThickness(Tools.fromDpToPx(2))
                .setDotsStrokeColor(this.getResources().getColor(R.color.line_visits))
                .setLineColor(this.getResources().getColor(R.color.line_visits))
                .setLineThickness(Tools.fromDpToPx(3))
                .beginAt(1).endAt(lcp.datesArray.length);

        mLineChart.addData(dataSet);

        //init and add "page views" line to the chart
        dataSet = new LineSet();
        dataSet.addPoints(lcp.datesArray, lcp.pageViewsArray);
        dataSet.setDots(true)
                .setDotsColor(this.getResources().getColor(R.color.line_pageviews_bg))
                .setDotsRadius(Tools.fromDpToPx(5))
                .setDotsStrokeThickness(Tools.fromDpToPx(2))
                .setDotsStrokeColor(this.getResources().getColor(R.color.line_pageviews))
                .setLineColor(this.getResources().getColor(R.color.line_pageviews))
                .setLineThickness(Tools.fromDpToPx(3))
                .beginAt(1).endAt(lcp.datesArray.length);

        mLineChart.addData(dataSet);

        //general initialization
        mLineChart.setAxisBorderValues(0, lcp.maxValue, lcp.step);

        Paint mLineGridPaint = new Paint();
        mLineGridPaint.setColor(this.getResources().getColor(R.color.line_grid));
        mLineGridPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
        mLineGridPaint.setStyle(Paint.Style.STROKE);
        mLineGridPaint.setAntiAlias(true);
        mLineGridPaint.setStrokeWidth(Tools.fromDpToPx(.75f));

        mLineChart.setBorderSpacing(Tools.fromDpToPx(4))
                .setGrid(LineChartView.GridType.HORIZONTAL, mLineGridPaint)
                .setXAxis(false)
                .setXLabels(XController.LabelPosition.OUTSIDE)
                .setYAxis(false)
                .setYLabels(YController.LabelPosition.OUTSIDE)
                .setLabelsFormat(new DecimalFormat("##"));

        Animation animation = new Animation().setEasing(mCurrEasing);
        mLineChart.show(animation.setEndAction(mEnterEndAction));
    }


    /**
     * Generates a list with random Jimdo Website statistics.
     * @param numberOfDays - Number of Jimdo day usage statistics
     * @return generated list of JimdoPerDayStatistics
     */
    private List<JimdoPerDayStatistics> generateMockStats(int numberOfDays) {

        List<JimdoPerDayStatistics> mockStats = new ArrayList<>();

        String[] referer = {"www.google.com","www.yandex.ru","","www.altavista.com"};
        String[] pages = {"test1.jimdo.com","test2.jimdo.com","test3.jimdo.com"};
        String[] devices = {"android","iphone","pc"};
        String[] os = {"Windows","Linux","MacOS", "iOS", "Android"};

        Random rand = new Random();
        //go back in time for the "numberOfDays"
        long dayTime = Calendar.getInstance().getTimeInMillis() - (MILLISECONDS_IN_DAY*numberOfDays);
        for (int day = 0; day < numberOfDays; day++) {
            JimdoPerDayStatistics dayStat = new JimdoPerDayStatistics();
            int uniqueVisits = rand.nextInt(20);
            int pageViews = rand.nextInt(3)+1; //1 to 4 pageviews per visit

            for (int v = 0; v < uniqueVisits; v++) {
                Visit visit = new Visit();
                for (int p = 0; p < pageViews; p++) {
                    PageView pageView = new PageView();
                    pageView.setPage(pages[rand.nextInt(2)]);
                    pageView.setTimeSpentOnPage(rand.nextInt(100000));
                    visit.addPageView(pageView);
                }
                visit.setReferer(referer[rand.nextInt(2)]);
                visit.setDevice(devices[rand.nextInt(2)]);
                visit.setOS(os[rand.nextInt(4)]);
                dayStat.addVisit(visit);
            }
            dayStat.setDate(new Date(dayTime));
            //back to the future
            dayTime = dayTime + MILLISECONDS_IN_DAY;

            mockStats.add(dayStat);
        }

        return mockStats;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
