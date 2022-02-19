package kesira.papertrader;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

class APIHelper {
    static String get(String url) {
        StringBuilder result = new StringBuilder();
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            if (urlConnection.getResponseCode() == 200) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                bufferedReader.close();
            }
            urlConnection.disconnect();
        } catch (IOException e) {
            Log.e("Exception", e.getMessage());
        }
        return result.toString();
    }

    private static Calendar getTodayCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        if (calendar.get(Calendar.HOUR_OF_DAY) < 17) {
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        }
        toFridayIfWeekend(calendar);
        return calendar;
    }

    private static void toFridayIfWeekend(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -1);
        } else if (dayOfWeek == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_WEEK, -2);
        }
    }

    static String getToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return dateFormat.format(getTodayCalendar().getTime());
    }

    static String getPreviousDay() {
        Calendar calendar = getTodayCalendar();
        calendar.add(Calendar.DAY_OF_WEEK, -1);
        toFridayIfWeekend(calendar);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return dateFormat.format(calendar.getTime());
    }

    static String getRangeStart(int field, int amount) {
        Calendar calendar = getTodayCalendar();
        calendar.add(field, -amount);
        calendar.add(Calendar.DAY_OF_WEEK, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        return dateFormat.format(calendar.getTime());
    }
}
