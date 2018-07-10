package eu.baboi.cristian.news;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//TODO make style schanges for old android version
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
    }

    // the settings fragment
    public static class NewsPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private static final String LOG = "Settings";

        private Preference rating;
        //other
        private Preference password;
        private Preference pageSize;

        //boolean
        private Preference topic;
        private Preference office;
        private Preference lang;

        //date range
        private Preference from;
        private Preference to;
        private Preference useDate;

        //order
        private Preference orderBy;
        private Preference orderDate;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            rating = findPreference(getString(R.string.rating));
            //boolean
            topic = findPreference(getString(R.string.topic));
            office = findPreference(getString(R.string.office));
            lang = findPreference(getString(R.string.lang));

            //date range
            from = findPreference(getString(R.string.from));
            to = findPreference(getString(R.string.to));
            useDate = findPreference(getString(R.string.useDate));

            //order
            orderBy = findPreference(getString(R.string.orderBy));
            orderDate = findPreference(getString(R.string.orderDate));

            //other
            password = findPreference(getString(R.string.password));
            pageSize = findPreference(getString(R.string.pageSize));

            bindPreferenceSummaryToValue(rating);

            bindPreferenceSummaryToValue(topic);
            bindPreferenceSummaryToValue(office);
            bindPreferenceSummaryToValue(lang);

            bindPreferenceSummaryToValue(from);
            bindPreferenceSummaryToValue(to);
            bindPreferenceSummaryToValue(useDate);

            bindPreferenceSummaryToValue(orderBy);
            bindPreferenceSummaryToValue(orderDate);

            bindPreferenceSummaryToValue(password);
            bindPreferenceSummaryToValue(pageSize);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString().trim();
            if (preference == password) return true;
            if (preference == rating) {//can be empty, 0, sau 1 - 5
                if (!stringValue.isEmpty())
                    try {
                        int stars = Integer.valueOf(stringValue);
                        if (stars < 0 || stars > 5) return false;
                        preference.setSummary("*****".substring(0, stars));
                        return true;
                    } catch (Exception e) {
                        Log.e(LOG, String.format("Wrong rating value: %s", stringValue), e);
                        return false;
                    }
            }
            if (preference == pageSize) {//can be 1-50
                try {
                    long size = Long.valueOf(stringValue);
                    if (size <= 0 || size > 50) return false;
                } catch (Exception e) {
                    Log.e(LOG, String.format("Wrong page size: %s", stringValue), e);
                    return false;
                }
            }
            if (preference == from || preference == to) {// can be empty or a valid date
                DateFormat source = SimpleDateFormat.getDateInstance(DateFormat.SHORT, Locale.FRANCE);
                DateFormat target = SimpleDateFormat.getDateInstance(DateFormat.FULL);
                Date date;
                if (!stringValue.isEmpty()) {
                    try {
                        date = source.parse(stringValue);
                    } catch (ParseException e) {
                        Log.e(LOG, String.format("Wrong date format: %s", stringValue), e);
                        return false;
                    }
                    stringValue = target.format(date);
                }
            }
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else preference.setSummary(stringValue);
            return true;
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(this);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            String preferenceString = preferences.getString(preference.getKey(), "");

            //update screen
            onPreferenceChange(preference, preferenceString);
        }
    }
}
