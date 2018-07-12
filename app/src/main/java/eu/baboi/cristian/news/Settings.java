package eu.baboi.cristian.news;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

// Keep a cache of preferences and provide the URL for loading news articles
public class Settings {
    private static final String URL = "https://content.guardianapis.com/search";

    // the parameters
    public enum Parameter {
        RATING(R.string.rating, R.string.ratingDefault) {
            @Override
            public boolean isEmpty() {
                return val == null || val.isEmpty() || val.equalsIgnoreCase("0");
            }
        },
        TOPIC(R.string.topic, R.string.topicDefault),
        OFFICE(R.string.office, R.string.officeDefault),
        LANG(R.string.lang, R.string.langDefault),
        FROM(R.string.from, R.string.fromDefault) {
            @Override
            public String value() {
                return apiDate(val);
            }
        },
        TO(R.string.to, R.string.toDefault) {
            @Override
            public String value() {
                return apiDate(val);
            }
        },
        USE_DATE(R.string.useDate, R.string.dateDefault),
        ORDER_BY(R.string.orderBy, R.string.orderDefault),
        ORDER_DATE(R.string.orderDate, R.string.dateDefault),
        PAGE_SIZE(R.string.pageSize, R.string.pageSizeDefault),
        PASSWORD(R.string.password, R.string.passwordDefault) {
            @Override
            public boolean isEmpty() {
                return true;
            }
        };

        //the resource IDs
        public final int keyId;
        public final int defaultId;


        public String key;//key name
        public String val;//key value as stored in the preferences
        public String def;//default value

        Parameter(int key, int def) {
            this.keyId = key;
            this.defaultId = def;
            this.key = null;
            this.def = null;
            this.val = null;
        }

        public boolean isEmpty() {
            return val == null || val.isEmpty();
        }

        //key value as needed for the Guardian API
        public String value() {
            return val;
        }

        // convert a French date to a format accepted by the Guardian API
        private static String apiDate(String date) {
            if (date == null || date.isEmpty()) return null;
            DateFormat source = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);
            SimpleDateFormat target = new SimpleDateFormat("yyyy-MM-dd");
            Date d;
            try {
                d = source.parse(date);
            } catch (ParseException e) {
                return null;
            }
            return target.format(d);
        }
    }

    public static String api_key = null;

    private static HashMap<String, Parameter> map = null;// map from key to Parameter

    private Settings() {
    }

    public static boolean isLoaded() {
        return map != null;
    }

    // load the preferences into memory
    public static void load(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Parameter parameters[] = Parameter.values();

        map = new HashMap<>(parameters.length);
        for (Parameter p : parameters) {
            p.key = context.getString(p.keyId);
            p.def = context.getString(p.defaultId).trim();
            p.val = sharedPreferences.getString(p.key, p.def).trim();//trim spaces
            map.put(p.key, p);
        }

        String key = context.getString(R.string.key);
        api_key = Key.getApiKey(Parameter.PASSWORD.val, key);
    }

    // update the memory copy of a given preference
    public static void update(SharedPreferences sharedPreferences, String key) {
        Parameter parameter = map != null ? map.get(key) : null;
        if (parameter != null)
            parameter.val = sharedPreferences.getString(key, parameter.def).trim();
    }

    // make an Uri from preferences given a page
    public static Uri makeUri(long page) {
        Uri.Builder builder = Uri.parse(URL).buildUpon();

        if (api_key == null) api_key = "test";

        builder.appendQueryParameter("api-key", api_key);
        builder.appendQueryParameter("format", "json");
        builder.appendQueryParameter("page", String.valueOf(page));

        for (Parameter p : Parameter.values())
            if (!p.isEmpty()) builder.appendQueryParameter(p.key, p.value());

        //data
        builder.appendQueryParameter("show-fields", "headline,byline,firstPublicationDate,thumbnail");
        builder.appendQueryParameter("show-tags", "contributor");
        return builder.build();
    }
}
