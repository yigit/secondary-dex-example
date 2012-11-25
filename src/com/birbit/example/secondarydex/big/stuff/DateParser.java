package com.birbit.example.secondarydex.big.stuff;

import com.birbit.example.secondarydex.big.base.IDateParser;
import com.birbit.example.secondarydex.big.base.exception.MyIllegalArgumentException;
import com.birbit.example.secondarydex.big.base.exception.MyUnsupportedOperationException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/*
 * the actual class that wraps calls to Joda
 */
public class DateParser implements IDateParser {
    static final DateTimeFormatter jodaTimeFormatter = DateTimeFormat.forPattern("y-M-d'T'H:m:s.SSSSSSSSSS'Z'").withZoneUTC();
    @Override
    public Date parseDate(String timstamp) {
        try {
            //parseDate throws UnsupportedOperationException and IllegalArgumentException
            //both of which are in java.lang package. normally, we can just throw them
            //but for the demo purposes, we'll assume that they belong to org.joda package and
            //since org.joda is not included in our app, we need to wrap them into known exceptions
            return jodaTimeFormatter.parseDateTime(timstamp).toDate();
        } catch (UnsupportedOperationException e) {
            throw new MyUnsupportedOperationException(e);
        } catch (IllegalArgumentException e) {
            throw new MyIllegalArgumentException(e);
        }
    }

    @Override
    public String format(Date date) {
        return jodaTimeFormatter.print(new DateTime(date));
    }
}
