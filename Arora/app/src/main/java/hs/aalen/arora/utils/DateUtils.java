package hs.aalen.arora.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for manipulating Date and Time formats
 *
 * @author Michael Schlosser
 */
public class DateUtils {
    /**
     * Parses the Timestamp from the DB into a better readable format
     *
     * @param dateTimeDB date and time retrieved from DB
     * @return well formatted DateTime String
     */
    public static String parseDateTime(String dateTimeDB) {
        SimpleDateFormat formatDB = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date newDate;
        String ret;
        try {
            newDate = formatDB.parse(dateTimeDB);
            SimpleDateFormat formatRet = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            assert newDate != null;
            ret = formatRet.format(newDate);
        } catch (ParseException | NullPointerException e) {
            e.printStackTrace();
            ret = dateTimeDB;
        }
        return ret;
    }
}
