package eu.baboi.cristian.news;

import android.net.Uri;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


class News {
    private static final String LOG = "News";
    private static final Uri defaultPicture = Uri.parse("android.resource://"+R.class.getPackage().getName()+"/" +R.mipmap.ic_launcher);
    private URL mPictureURL; // the url of the picture
    private Uri mPicture; // the local copy of the picture
    private String mTitle;
    private String mSection;
    private Date mDate;
    private String mAuthor;
    private Uri mSource;

    News(){
        mTitle = "Breaking news!";
        mSection = "Sport";
        mDate = new Date();
        mAuthor = "John Doe";
        mPicture =  null;
        mPictureURL = null;
        mSource = Uri.parse("http://www.theguardian.com");
    }


    public boolean hasPicture(){
        return mPictureURL != null;
    }

    public boolean isCached(){
        return mPicture != null;
    }

    public Uri getPicture() {
        return mPicture==null? defaultPicture : mPicture;
    }

    public URL getPictureURL(){
        return mPictureURL;
    }

    public void setPicture(String url){
        if(url==null||url.trim().length()==0) return;
        try{
            mPictureURL = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(LOG,String.format("Wrong URL:%s",url),e);
        }
    }

    public void setPicture(Uri uri){
        mPicture = uri;
    }

    public String getTitle() {
        return mTitle;
    }
    public void setTitle(String title){
        mTitle = title;
    }

    public String getSection() {
        return mSection;
    }

    public void setSection(String section) {
        mSection = section;
    }

    public String getDate() {
        SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return dtf.format(mDate);
    }
    public void setDate(String date){
        SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dtf.setTimeZone(TimeZone.getTimeZone("Z"));
        try {
            mDate = dtf.parse(date);
        } catch (ParseException e) {
            Log.e(LOG,"Invalid date!",e);
        }
    }

    public String getAuthor() {
        return mAuthor;
    }
    public void setAuthor(String author){
        mAuthor = author;
    }
    public Uri getSource() {
        return mSource;
    }
    public void setSource(String url){
        mSource = Uri.parse(url);
    }
}
