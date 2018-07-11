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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<NewsResult>, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int LOADER_KEY = -1;

    private static final String PASSWORD_KEY = "password";
    private static final String PAGE_KEY = "page";

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

        // setup views
        mProgress = findViewById(R.id.progress);

        adapter = new NewsAdapter(NewsResult.newResult(),this);

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        prepareLoading();
    }

    // settings menu handling
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

    // ask for password and start loading news articles
    private void prepareLoading(){
        String password = sharedPreferences.getString(PASSWORD_KEY, null);
        if(password == null) showPasswordDialog();
        else loadNews();
    }

    // ask for the password, save the password and start loading the data
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

                    //save the password into preferences
                    sharedPreferences.edit().putString(PASSWORD_KEY, password).apply();

                    loadNews();
                }
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    // The entry point - first loading of the news article list
    private void loadNews() {
        Settings.load(this); //load the settings
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        getLoaderManager().initLoader(LOADER_KEY, loaderArgs(1), this);
    }


    // Check if there is network connectivity
    boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Loader  methods

    // build loader arguments
    public Bundle loaderArgs(long page) {
        Bundle args = new Bundle(1);
        args.putLong(PAGE_KEY, page);
        return args;
    }

    @Override
    public Loader<NewsResult> onCreateLoader(int id, Bundle args) {
        if (args == null) throw new IllegalArgumentException("You must provide the page number!");

        // retrieve the page number
        long page = args.getLong(PAGE_KEY);
        if (page == 0) throw new IllegalArgumentException("You must provide the page number!");

        //start loading new data
        mProgress.setVisibility(View.VISIBLE);
        return new NewsLoader(getApplicationContext(), page);
    }


    private void scrollToTop(){
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        linearLayoutManager.scrollToPosition(0);
    }

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

    //called for each preference changed in the settings screen
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mProgress.setVisibility(View.VISIBLE);// make progress indicator visible
    }

    // see https://medium.com/google-developers/making-loading-data-on-android-lifecycle-aware-897e12760832
    // The AsyncTaskLoader news loader
    private static class NewsLoader extends AsyncTaskLoader<NewsResult> implements SharedPreferences.OnSharedPreferenceChangeListener {
        final private File cacheDir; // the folder to load news pictures

        private long mPage; // the page to display
        private String mUrl; // the url of the page to display

        private NewsResult mData = null;
        private boolean hasCallback = false;

        NewsLoader(Context context, long page) {
            super(context);
            cacheDir = context.getDir("cachedPictures", 0);
            mPage = page;
        }

        @Override
        protected void onStartLoading() {//called only on new loader or new activity

            if (mData != null) {// already have data
                if (takeContentChanged()) mData = null;
                else deliverResult(mData); // use cached data
            }

            if (!hasCallback) { // register for preference changes
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                sharedPreferences.registerOnSharedPreferenceChangeListener(this);
                hasCallback = true;
            }

            if (mData == null) {
                mUrl = Settings.makeUri(mPage).toString();//build URL from cached preferences
                forceLoad();
            }
        }


        @Override
        public void deliverResult(NewsResult data) {
            mData = data; // save the data for later
            super.deliverResult(data);
        }

        protected void onReset() {
            if (hasCallback) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                hasCallback = false;
            }
        }

        // perform data loading on background thread - must have mPage, mUrl and cacheDir set!
        @Override
        public NewsResult loadInBackground() {
            if (mUrl == null || mUrl.trim().length() == 0) return NewsResult.newResult();

            NewsResult result = QueryUtils.getNews(cacheDir, mUrl);
            if(result.code==NewsResult.ERROR) result.page = mPage; //save the page if error

            return result;
        }

        // watch for changes in preferences and trigger loading of new data
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Settings.update(sharedPreferences, key);//update the cached values

            mPage = 1;//restart on page one
            mUrl = Settings.makeUri(mPage).toString();//build new URL from cached preferences

            // trigger loading of new data
            onContentChanged();
        }
    }
}
