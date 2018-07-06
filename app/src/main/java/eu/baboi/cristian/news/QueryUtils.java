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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public final class QueryUtils {

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

            if(connection.getResponseCode()==200){
                // connection successful
                inputStream = connection.getInputStream();
                response = readData(inputStream);
            } else {
                Log.e(LOG, String.format("Error connecting to news code: %d",connection.getResponseCode()) );
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
            JSONObject response = root.getJSONObject("response");
            String status = response.getString("status");
            if(!status.equalsIgnoreCase("ok")) {
                result.code = NewsResult.ERROR;
                return result;
            }
            //code is EMPTY by default
            result.count = response.getLong("total");// number of items
            result.pageSize = response.getLong("pageSize"); // size of one page
            result.page = response.getLong("currentPage");
            result.pages = response.getLong("pages");// number of pages in total

            JSONArray results = response.getJSONArray("results"); // can be empty
            int nr = results.length();

            for(int i=0; i<nr; i++){ //for each news article
                JSONObject article = results.getJSONObject(i);

                String section = article.getString("sectionName");
                String url = article.getString("webUrl");
                String webDate = article.getString("webPublicationDate");
                String webTitle = article.getString("webTitle");
                JSONObject fields = article.getJSONObject("fields");

                // optional
                String title = fields.optString("headline", webTitle);
                String authors = fields.optString("byline","");
                String date = fields.optString("firstPublicationDate", webDate);
                String thumbnail = fields.optString("thumbnail",null);

                JSONArray tags = article.getJSONArray("tags");// can be empty
                int len = tags.length();
                for(int j = 0; j< len; j++){
                    String author = tags.getJSONObject(j).getString("webTitle");
                    authors = author;
                }
                News art = new News();

                art.setPicture(thumbnail);
                art.setTitle(title);
                art.setSection(section);
                art.setDate(date);
                art.setAuthor(authors);
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
        HttpsURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            File file = new File(dir, url.getPath());
            if(file.exists()) return Uri.fromFile(file); // if the file exists return it
            file.getParentFile().mkdirs();
            if(!file.createNewFile()) {
                Log.e(LOG, "Cannot create cache file.");
                return null;
            };

            file.deleteOnExit();
            outputStream = new FileOutputStream(file);

            connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();

            if(connection.getResponseCode()==200) {

                inputStream = connection.getInputStream();
                byte[] buffer = new byte[4096];// the transfer buffer
                int n = 0; // the byte count

                while ((n = inputStream.read(buffer, 0, 4096)) != -1)
                    outputStream.write(buffer, 0, n);

                return Uri.fromFile(file);
            } else {
                Log.e(LOG, String.format("Error connecting to news code: %d", connection.getResponseCode()) );
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
