package com.example.jarvis6;

import android.content.Context;
import android.provider.CalendarContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class CalenderHelper {
    record Event(long id, String title, String description, String location, String startTime, String endTime) {};

    private final Context context;

    public CalenderHelper(Context context) {
        this.context = context;
    }

    public List<Event> getEvents() {
        var events = new ArrayList<Event>();
        var cr = context.getContentResolver();

        var uri = CalendarContract.Events.CONTENT_URI;

        var projection = new String[] {
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };

        var startCalender = Calendar.getInstance();
        startCalender.set(Calendar.HOUR_OF_DAY, 0);
        startCalender.set(Calendar.MINUTE, 0);
        startCalender.set(Calendar.SECOND, 0);
        startCalender.set(Calendar.MILLISECOND, 0);

        var endCalendar = (Calendar) startCalender.clone();
        endCalendar.add(Calendar.DAY_OF_MONTH, 5);

        var selection = CalendarContract.Events.DTSTART + " >= ? AND " + CalendarContract.Events.DTSTART + " <= ?";
        var selectionArgs = new String[] {
                String.valueOf(startCalender.getTimeInMillis()),
                String.valueOf(endCalendar.getTimeInMillis())
        };

        var cursor = cr.query(uri, projection, selection, selectionArgs, CalendarContract.Events.DTSTART + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                var sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                var formattedStartDate = sdf.format(cursor.getLong(4));
                var formattedEndDate = sdf.format(cursor.getLong(5));

                var event = new Event(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        formattedStartDate,
                        formattedEndDate
                );

                events.add(event);
            } while (cursor.moveToNext());

            cursor.close();
        }

        System.out.println("These are the collected events: ");
        events.forEach(System.out::println);
        return events;
    }

    public static String getCurrentDay() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
    }
}
