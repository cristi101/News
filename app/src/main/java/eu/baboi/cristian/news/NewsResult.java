package eu.baboi.cristian.news;

import java.util.ArrayList;
import java.util.List;

public class NewsResult {
    public static final int OK = 0;
    public static final int EMPTY = 1;
    public static final int ERROR = 2;
    public int code; //error code see above

    public long count ;
    public long pageSize ;
    public long page ;
    public long pages ;
    public List<News> news;

    // no one can create new instances directly
    private NewsResult(){

    }

    public static boolean isEmpty(NewsResult result){
        return result==null||result.news==null||result.news.isEmpty();
    }

    public static NewsResult newResult(){
        NewsResult result;
        result = new NewsResult();
        result.code = EMPTY;
        result.count=0;
        result.pageSize = 0;
        result.page = 0;
        result.pages = 0;
        result.news = new ArrayList<>();
        return result;
    }
    public void clear(){
        code = EMPTY;
        count=0;
        pageSize = 0;
        page = 0;
        pages = 0;
        news.clear();
    }
    public void copy(NewsResult data){
        if (data != null) {
            code = data.code;
            count = data.count;
            pageSize = data.pageSize;
            page = data.page;
            pages = data.pages;
            news = data.news;
        } else clear();
    }

    // check if we need to display the two buttons
    public boolean hasPrevious(){
        return page!=0 && page!=1 && pages!=1;
    }
    public boolean hasNext(){
        return pages !=1 && page!=pages;
    }

    public News get(int position) {
        return news.get(position);
    }
}
