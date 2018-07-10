package eu.baboi.cristian.news;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<NewsResult>, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String URL = "https://content.guardianapis.com/search";
    private static final String KEY = "password";

    private long pageSize;
    private long page;

    private SharedPreferences sharedPreferences;
    private AlertDialog dialog = null;

    private ProgressBar mProgress;

    private RecyclerView recyclerView;
    private NewsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        pageSize = Long.valueOf(sharedPreferences.getString(getString(R.string.pageSize), getString(R.string.pageSizeDefault)));

        // setup views
        mProgress = findViewById(R.id.progress);

        adapter = new NewsAdapter(NewsResult.newResult(),this);

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        prepareLoading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareLoading(){
        String password = sharedPreferences.getString(KEY,null);
        if(password == null) showPasswordDialog();
        else loadNews();
    }

    // ask for the password, find the api_key and start loading the data
    private void showPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.password, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setView(dialogView);
        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                if (dialog != null) {
                    EditText text = dialog.findViewById(R.id.password);
                    String password = text.getText().toString().trim();
                    dialog.dismiss();
                    dialog = null;
                    sharedPreferences.edit().putString(KEY, password).apply();
                    loadNews();
                }
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    private void loadNews() {
        Bundle args = new Bundle(1);
        args.putLong("page",1);
        getLoaderManager().initLoader(-1, args, this);
    }


    // Check if there is network connectivity
    boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }



    // Loader  methods

    @Override
    public Loader<NewsResult> onCreateLoader(int id, Bundle args) {
        //TODO what if args = null

        // save the page
        page = args.getLong("page");

        //start loading new data
        mProgress.setVisibility(View.VISIBLE);
        return new NewsLoader(getApplicationContext(), page);
    }

    //TODO scroll to given offset
    private void scrollToTop(){
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        linearLayoutManager.scrollToPosition(0);
    }

    //TODO find offset to scroll to
    @Override
    public void onLoadFinished(Loader<NewsResult> loader, NewsResult data) {
        mProgress.setVisibility(View.GONE);
        adapter.update(data);
        scrollToTop();
    }

    @Override
    public void onLoaderReset(Loader<NewsResult> loader) {
        adapter.clear();
    }

    // calculate the new page
    private static long newPage(long oldPage, long oldPageSize, long pageSize) {
        long page = (oldPage - 1) * oldPageSize / pageSize + 1;
        return page;
    }

    ;

    //TODO compute offset in page to scroll to
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //called for each preference changed in the settings screen
        if (key.equals(getString(R.string.pageSize))) {
            //page size changed;
            long oldPageSize = pageSize;
            pageSize = Long.valueOf(sharedPreferences.getString(getString(R.string.pageSize), getString(R.string.pageSizeDefault)));
            page = newPage(page, oldPageSize, pageSize);
            Bundle args = new Bundle();
            args.putLong("page", page);
            getLoaderManager().restartLoader(-1, args, this);
            return;
        }
        //start loading new data
        mProgress.setVisibility(View.VISIBLE);
    }

    //TODO move this to Settings class with singleton pattern
    // make an Uri from settings variables
    private static Uri makeUri(long page, Settings settings) {

        //extract keys
        String ratingKey = settings.ratingKey;

        //boolean
        String topicKey = settings.topicKey;
        String officeKey = settings.officeKey;
        String langKey = settings.langKey;

        //range
        String fromKey = settings.fromKey;
        String toKey = settings.toKey;
        String useDateKey = settings.useDateKey;

        //order
        String orderByKey = settings.orderByKey;
        String orderDateKey = settings.orderDateKey;

        //other
        String pageSizeKey = settings.pageSizeKey;

        //extract settings
        String rating = settings.rating;

        //boolean
        String topic = settings.topic;
        String office = settings.office;
        String lang = settings.lang;

        //range
        String from = settings.from;
        String to = settings.to;
        String useDate = settings.useDate;

        //order
        String orderBy = settings.orderBy;
        String orderDate = settings.orderDate;

        //other
        String pageSize = settings.pageSize;
        String password = settings.password;

        String api_key = Key.getApiKey(password, settings.key);

        Uri.Builder builder = Uri.parse(URL).buildUpon();
        builder.appendQueryParameter("api-key", api_key);
        builder.appendQueryParameter("format", "json");
        builder.appendQueryParameter("page", String.valueOf(page));

        if (!rating.isEmpty() && !rating.equalsIgnoreCase("0"))//not empty or 0
            builder.appendQueryParameter(ratingKey, rating);

        //boolean
        if (!topic.isEmpty()) builder.appendQueryParameter(topicKey, topic);
        if (!office.isEmpty()) builder.appendQueryParameter(officeKey, office);
        if (!lang.isEmpty()) builder.appendQueryParameter(langKey, lang);

        //range
        if (!from.isEmpty()) builder.appendQueryParameter(fromKey, from);
        if (!to.isEmpty()) builder.appendQueryParameter(toKey, to);
        builder.appendQueryParameter(useDateKey, useDate);

        //order
        builder.appendQueryParameter(orderByKey, orderBy);
        builder.appendQueryParameter(orderDateKey, orderDate);

        //other
        builder.appendQueryParameter(pageSizeKey, pageSize);

        //data
        builder.appendQueryParameter("show-fields", "headline,byline,firstPublicationDate,thumbnail");
        builder.appendQueryParameter("show-tags", "contributor");
        return builder.build();
    }

    private static class NewsLoader extends AsyncTaskLoader<NewsResult> {
        final private File cacheDir;

        private long mPage;
        private String mUrl;

        NewsLoader(Context context, long page) {
            super(context);
            cacheDir = context.getCacheDir();
            mPage = page;
        }

        @Override
        protected void onStartLoading() {
            Settings settings = new Settings(getContext());
            mUrl = makeUri(mPage, settings).toString();
            Log.e("URL", mUrl);
            forceLoad();
        }

        @Override
        public NewsResult loadInBackground() {
            if (mUrl == null || mUrl.trim().length() == 0) return NewsResult.newResult();
            NewsResult result = QueryUtils.getNews(cacheDir, mUrl);
            if(result.code==NewsResult.ERROR) result.page = mPage; //save the page if error
            return result;
        }
    }
}
