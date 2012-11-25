package com.birbit.example.secondarydex.big.base;

import java.util.Date;

/**
 * interface to wrap calls that needs to go to joda package
 */
public interface IDateParser {
    public Date parseDate(String timestamp);
    public String format(Date date);
}
