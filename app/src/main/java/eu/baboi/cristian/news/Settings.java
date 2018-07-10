package eu.baboi.cristian.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Settings {

    // Keys
    public final String ratingKey;

    //boolean
    public final String topicKey;
    public final String officeKey;
    public final String langKey;

    //range
    public final String fromKey;
    public final String toKey;
    public final String useDateKey;

    //order
    public final String orderByKey;
    public final String orderDateKey;

    //other
    public final String pageSizeKey;
    public final String passwordKey;
    public final String key;

    // Defaults
    public final String ratingDefault;

    //boolean
    public final String topicDefault;
    public final String officeDefault;
    public final String langDefault;

    //range
    public final String fromDefault;
    public final String toDefault;
    public final String dateDefault;

    //order
    public final String orderDefault;

    //other
    public final String pageSizeDefault;
    public final String passwordDefault;

    // State
    public String rating;

    //boolean
    public String topic;
    public String office;
    public String lang;

    //range
    public String from;
    public String to;
    public String useDate;

    //order
    public String orderBy;
    public String orderDate;

    //other
    public String pageSize;
    public String password;


    Settings(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //Keys
        ratingKey = context.getString(R.string.rating);

        //boolean
        topicKey = context.getString(R.string.topic);
        officeKey = context.getString(R.string.office);
        langKey = context.getString(R.string.lang);

        //range
        fromKey = context.getString(R.string.from);
        toKey = context.getString(R.string.to);
        useDateKey = context.getString(R.string.useDate);

        //order
        orderByKey = context.getString(R.string.orderBy);
        orderDateKey = context.getString(R.string.orderDate);

        //other
        pageSizeKey = context.getString(R.string.pageSize);
        passwordKey = context.getString(R.string.password);
        key = context.getString(R.string.key);

        //Defaults
        ratingDefault = context.getString(R.string.ratingDefault);

        //boolean
        topicDefault = context.getString(R.string.topicDefault);
        officeDefault = context.getString(R.string.officeDefault);
        langDefault = context.getString(R.string.langDefault);

        //range
        fromDefault = context.getString(R.string.fromDefault);
        toDefault = context.getString(R.string.toDefault);
        dateDefault = context.getString(R.string.dateDefault);

        //order
        orderDefault = context.getString(R.string.orderDefault);

        //other
        pageSizeDefault = context.getString(R.string.pageSizeDefault);
        passwordDefault = context.getString(R.string.passwordDefault);

        rating = sharedPreferences.getString(ratingKey, ratingDefault);

        //boolean
        topic = sharedPreferences.getString(topicKey, topicDefault);
        office = sharedPreferences.getString(officeKey, officeDefault);
        lang = sharedPreferences.getString(langKey, langDefault);

        //range
        from = apiDate(sharedPreferences.getString(fromKey, fromDefault));
        to = apiDate(sharedPreferences.getString(toKey, toDefault));
        useDate = sharedPreferences.getString(useDateKey, dateDefault);

        //order
        orderBy = sharedPreferences.getString(orderByKey, orderDefault);
        orderDate = sharedPreferences.getString(orderDateKey, dateDefault);

        //other
        pageSize = sharedPreferences.getString(pageSizeKey, pageSizeDefault);
        password = sharedPreferences.getString(passwordKey, passwordDefault);
    }

    // convert a french date to a format accepted by the Guardian API
    public static String apiDate(String date) {
        if (date == null || date.isEmpty()) return "";
        DateFormat source = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);
        SimpleDateFormat target = new SimpleDateFormat("yyyy-MM-dd");
        Date d;
        try {
            d = source.parse(date);
        } catch (ParseException e) {
            return "";
        }
        return target.format(d);
    }
}
