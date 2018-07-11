package eu.baboi.cristian.news;


import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public final class QueryUtils {
    public static final String RESPONSE = "response";
    public static final String STATUS = "status";
    public static final String OK = "ok";

    public static final String COUNT = "total";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE = "currentPage";
    public static final String PAGES = "pages";

    public static final String RESULTS = "results";
    public static final String SECTION = "sectionName";
    public static final String URL = "webUrl";
    public static final String WEB_DATE = "webPublicationDate";
    public static final String WEB_TITLE = "webTitle";

    public static final String FIELDS = "fields";
    public static final String TITLE = "headline";
    public static final String AUTHORS = "byline";
    public static final String DATE = "firstPublicationDate";
    public static final String PICTURE = "thumbnail";

    public static final String TAGS = "tags";
    public static final String AUTHOR = "webTitle";

    private static final String LOG = "QueryUtils";

    private QueryUtils(){}

    private static NewsResult fetchNewsData(String url){
        URL lUrl = createUrl(url);
        String response = null;
        try{
            response = makeHttpRequest(lUrl);
        } catch (IOException e){
            Log.e(LOG,"Error closing news HTTP connection!",e);
        }
        return extractNews(response);
    }

    // transform a string to an URL
    private static URL createUrl(String url){
        if(url==null||url.trim().length()==0) return null;
        URL lUrl = null;
        try {
            lUrl = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(LOG, String.format("Error creating URL:%s",url),e);
        }
        return lUrl;
    }

    // get the response of a HTTP request
    private static String makeHttpRequest(URL url) throws IOException{
        String response = "";
        if(url==null) return response;

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            // setup the connection
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // connection successful
                inputStream = connection.getInputStream();
                response = readData(inputStream);
            } else {
                Log.e(LOG, String.format("Error connecting to news code: %d url: %s", connection.getResponseCode(), url.toString()));
            }

        } catch (IOException e){
            Log.e(LOG,"Problem retrieving JSON results",e);
        } finally {
            if(connection!=null)
                connection.disconnect();
            if(inputStream!=null)
                inputStream.close();
        }
        return response;
    }

    // read data from an InputStream
    private static String readData(InputStream stream) throws IOException {
        StringBuilder output = new StringBuilder();
        if(stream!=null){
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));

            // read line by line
            String line = reader.readLine();
            while(line!=null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    // extract a list of News from a JSON string
    private static NewsResult extractNews(String json){
        NewsResult result = NewsResult.newResult();//EMPTY by default

        if(json==null||json.trim().length()==0) {
            result.code=NewsResult.ERROR;//ERROR because empty responses have valid JSON data
            return result;
        }

        try {
            JSONObject root = new JSONObject(json);
            JSONObject response = root.getJSONObject(RESPONSE);
            String status = response.getString(STATUS);
            if (!status.equalsIgnoreCase(OK)) {
                result.code = NewsResult.ERROR;
                return result;
            }
            //code is EMPTY by default
            result.count = response.getLong(COUNT);// number of items
            result.pageSize = response.getLong(PAGE_SIZE); // size of one page
            result.page = response.getLong(PAGE);
            result.pages = response.getLong(PAGES);// number of pages in total

            JSONArray results = response.getJSONArray(RESULTS); // can be empty
            int nr = results.length();

            for(int i=0; i<nr; i++){ //for each news article
                JSONObject article = results.getJSONObject(i);

                String section = article.getString(SECTION);
                String url = article.getString(URL);
                String webDate = article.getString(WEB_DATE);
                String webTitle = article.getString(WEB_TITLE);
                JSONObject fields = article.getJSONObject(FIELDS);

                // optional
                String title = fields.optString(TITLE, webTitle);
                String authors = fields.optString(AUTHORS, "");
                String date = fields.optString(DATE, webDate);
                String thumbnail = fields.optString(PICTURE, null);

                JSONArray tags = article.getJSONArray(TAGS);// can be empty
                int len = tags.length();
                for(int j = 0; j< len; j++){
                    authors = tags.getJSONObject(j).getString(AUTHOR);
                }
                News art = new News();

                art.setPicture(thumbnail);
                art.setTitle(title.trim());
                art.setSection(section.trim());
                art.setDate(date);
                art.setAuthor(authors.trim());
                art.setSource(url);

                result.news.add(art);
                result.code = NewsResult.OK;//not EMPTY anymore
            }
        } catch (JSONException e){
            Log.e(LOG,"Problem parsing JSON results", e);
            result.code=NewsResult.ERROR;
        }
        return result;
    }


    // extract a list of News from a given URL
    public static NewsResult getNews(File dir, String url) {
        NewsResult result = fetchNewsData(url);
        if(result.code == NewsResult.OK){
            //download the pictures for all results
            for(News news:result.news){
                if(news.hasPicture()) {
                    Uri uri = getPicture(dir, news.getPictureURL());
                    // can be null
                    news.setPicture(uri);
                }
            }
        }
        return result;
    }

    // save a local copy of the picture at url and return the Uri of it
    public static Uri getPicture(File dir, URL url){
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            File file = new File(dir, url.getPath());
            if(file.exists()) return Uri.fromFile(file); // if the file exists return it
            file.getParentFile().mkdirs();
            if(!file.createNewFile()) {
                Log.e(LOG, "Cannot create cache file.");
                return null;
            }

            file.deleteOnExit();
            outputStream = new FileOutputStream(file);

            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                inputStream = connection.getInputStream();
                byte[] buffer = new byte[4096];// the transfer buffer
                int n; // the byte count

                while ((n = inputStream.read(buffer, 0, 4096)) != -1)
                    outputStream.write(buffer, 0, n);

                return Uri.fromFile(file);
            } else {
                Log.e(LOG, String.format("Error connecting to news code: %d url: %s", connection.getResponseCode(), url.toString()));
            }

        } catch (IOException e) {
            Log.e(LOG,"Error saving thumbnail.", e);
        } finally {

            if(connection!=null) connection.disconnect();

            if(inputStream!=null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(LOG, "Error closing picture connection.", e);
                }
            }

            if(outputStream!=null)
                try {
                outputStream.close();
                } catch (IOException e) {
                    Log.e(LOG, "Error closing cache file.", e);
                }
        }
        return null;
    }
}
