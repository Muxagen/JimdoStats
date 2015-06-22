package de.goldenzweig.jimdostats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by mihahh on 22.06.2015.
 */
public class JimdoStatisticsMockDataProvider {

    private static final long MILLISECONDS_IN_DAY = 86400000;

    /**
     * Generates a list with random Jimdo Website statistics.
     * @param numberOfDays - Number of Jimdo day usage statistics
     * @return generated list of JimdoPerDayStatistics
     */
    public static List<JimdoPerDayStatistics> generateMockStats(int numberOfDays) {

        List<JimdoPerDayStatistics> mockStats = new ArrayList<>();

        String[] referer = {"www.google.com","www.yandex.ru","","www.altavista.com"};
        String[] pages = {"test1.jimdo.com","test2.jimdo.com","test3.jimdo.com"};
        String[] devices = {"android","iphone","pc"};
        String[] os = {"Windows","Linux","MacOS", "iOS", "Android"};

        Random rand = new Random();
        //go back in time for the "numberOfDays"
        long dayTime = Calendar.getInstance().getTimeInMillis() - (MILLISECONDS_IN_DAY * numberOfDays);
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
}