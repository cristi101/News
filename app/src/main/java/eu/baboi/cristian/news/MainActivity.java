package eu.baboi.cristian.news;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<NewsResult>{
    private static final String URL = "http://content.guardianapis.com/search";
    private static final String KEY = "password";

    private String api_key = null;

    SharedPreferences sharedPreferences = null;
    private AlertDialog dialog = null;

    private ProgressBar mProgress;

    RecyclerView recyclerView ;
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

    private void prepareLoading(){
        String password = sharedPreferences.getString(KEY,null);
        if(password == null) showPasswordDialog();
        else loadNews(password);
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
                    loadNews(password);
                    sharedPreferences.edit().putString(KEY, password).apply();
                }
            }
        });

        dialog = builder.create();
        dialog.show();
    }

    private void loadNews(String password) {
        api_key = Key.getApiKey(password, getString(R.string.key));
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

        long page = args.getLong("page");
        Uri.Builder builder= Uri.parse(URL).buildUpon();
        builder.appendQueryParameter("api-key", api_key);
        builder.appendQueryParameter("format", "json");

        builder.appendQueryParameter("q", "Queen");
        //builder.appendQueryParameter("section","");
        //builder.appendQueryParameter("tag","");

        builder.appendQueryParameter("use-date", "first-publication");
        //builder.appendQueryParameter("from-date","");
        //builder.appendQueryParameter("to-date","");

        builder.appendQueryParameter("page", String.valueOf(page));
        builder.appendQueryParameter("page-size", "10");

        builder.appendQueryParameter("order-date", "last-modified");
        builder.appendQueryParameter("order-by", "newest");

        builder.appendQueryParameter("show-fields", "headline,byline,firstPublicationDate,thumbnail");
        builder.appendQueryParameter("show-tags", "contributor");

        mProgress.setVisibility(View.VISIBLE);
        return new NewsLoader(getApplicationContext(), page, builder.toString());
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


    private static class NewsLoader extends AsyncTaskLoader<NewsResult> {

        private String mUrl;
        private long   mPage;
        private File cacheDir;

        NewsLoader(Context context, long page, String url) {
            super(context);
            mUrl = url;
            mPage = page;
            cacheDir = context.getCacheDir();
        }

        @Override
        public NewsResult loadInBackground() {
            if(mUrl==null||mUrl.trim().length()==0) return null;
            NewsResult result = QueryUtils.getNews(cacheDir, mUrl);
            if(result.code==NewsResult.ERROR) result.page = mPage; //save the page if error
            return result;
        }


        @Override
        protected void onStartLoading() {
            forceLoad();
        }

    }

}
