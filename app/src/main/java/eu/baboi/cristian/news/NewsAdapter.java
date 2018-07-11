package eu.baboi.cristian.news;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

// Type Article
class NewsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    final ImageView picture;
    final TextView title;
    final TextView section;
    final TextView date;
    final TextView author;

    final private Context mContext;
    private Uri mSource;

    NewsViewHolder(@NonNull View itemView, Context context) {
        super(itemView);
        mContext = context;
        mSource = null;

        picture = itemView.findViewById(R.id.picture);
        title = itemView.findViewById(R.id.title);
        section = itemView.findViewById(R.id.section);
        date = itemView.findViewById(R.id.date);
        author = itemView.findViewById(R.id.author);

        LinearLayout layout = itemView.findViewById(R.id.list_item);
        layout.setOnClickListener(this);
    }

    void hidePicture() {
        picture.setVisibility(View.GONE);
    }

    void showPicture() {
        picture.setVisibility(View.VISIBLE);
    }

    void setSource(Uri source) {
        mSource = source;
    }

    public void onClick(View v) {
        if (mSource != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, mSource);
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(intent);
            }
        }
    }
}

//previous & next buttons
class ButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    final private MainActivity mainActivity;
    final private Button button;
    private long page;

    ButtonViewHolder(@NonNull View itemView, MainActivity context) {
        super(itemView);
        mainActivity = context;
        page = 0;
        button = itemView.findViewById(R.id.button);
        button.setOnClickListener(this);
    }

    void setPage(long page) {
        this.page = page;
    }

    void setButton(int res) {
        button.setText(res);
    }

    // jump to page if not 0
    @Override
    public void onClick(View v) {
        if (page > 0)
            mainActivity.getLoaderManager().restartLoader(MainActivity.LOADER_KEY, mainActivity.loaderArgs(page), mainActivity);
    }
}

//Empty state
class EmptyViewHolder extends RecyclerView.ViewHolder  {
    EmptyViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}

//error button
class ErrorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    final private MainActivity mainActivity;
    private long page;

    ErrorViewHolder(@NonNull View itemView, MainActivity context) {
        super(itemView);
        page = 0;
        mainActivity = context;
        Button button = itemView.findViewById(R.id.try_again);
        button.setOnClickListener(this);
    }

    void setPage(long page){
        this.page = page;
    }

    // jump to page if not 0
    @Override
    public void onClick(View v) {
        if (page > 0)
            mainActivity.getLoaderManager().restartLoader(MainActivity.LOADER_KEY, mainActivity.loaderArgs(page), mainActivity);
    }
}

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {
    private final static int TYPE_PREVIOUS = 0;
    private final static int TYPE_NEXT = 1;
    private final static int TYPE_ARTICLE = 2;
    private final static int TYPE_EMPTY = 3;
    private final static int TYPE_ERROR = 4;

    final private NewsResult mNewsList; // holds ALL the items to be displayed
    final private MainActivity mainActivity;

    public NewsAdapter(NewsResult newsList, MainActivity context) {
        //here we get the initial list
        mNewsList = newsList;
        mainActivity = context;
    }

    @Override
    public int getItemViewType(int position) {
        switch (mNewsList.code){
            case NewsResult.OK:
                if (position == 0 && mNewsList.hasPrevious()) return TYPE_PREVIOUS;
                if (position == getItemCount() - 1 && mNewsList.hasNext()) return TYPE_NEXT;
                return TYPE_ARTICLE;
            case NewsResult.EMPTY:
                return TYPE_EMPTY;
            case NewsResult.ERROR:
                return TYPE_ERROR;
        }
        return TYPE_ERROR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        switch (type) {
            case TYPE_PREVIOUS:
            case TYPE_NEXT:
                View button = LayoutInflater.from(parent.getContext()).inflate(R.layout.button, parent, false);
                return new ButtonViewHolder(button, mainActivity);
            case TYPE_ARTICLE:
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_item, parent, false);
                return new NewsViewHolder(itemView, mainActivity);
            case TYPE_EMPTY:
                View empty = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty, parent, false);
                return new EmptyViewHolder(empty);
            case TYPE_ERROR:
                View error = LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false);
                return new ErrorViewHolder(error, mainActivity);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_PREVIOUS: // nothing to do - can use one type of button and change labels here
                ButtonViewHolder pvh = (ButtonViewHolder) holder;
                pvh.setPage(mNewsList.page - 1);
                pvh.setButton(R.string.prev_page);
                break;
            case TYPE_NEXT:
                ButtonViewHolder nvh = (ButtonViewHolder) holder;
                nvh.setPage(mNewsList.page + 1);
                nvh.setButton(R.string.next_page);
                break;
            case TYPE_ARTICLE:

                // adjust the position
                if (mNewsList.hasPrevious()) position--;

                NewsViewHolder newsViewHolder = (NewsViewHolder) holder;

                News news = mNewsList.get(position);

                if (news.hasPicture()) {
                    newsViewHolder.picture.setImageURI(news.getPicture());
                    newsViewHolder.showPicture();
                } else newsViewHolder.hidePicture();

                newsViewHolder.title.setText(news.getTitle());
                newsViewHolder.section.setText(news.getSection());
                newsViewHolder.date.setText(news.getDate());
                newsViewHolder.author.setText(news.getAuthor());
                newsViewHolder.setSource(news.getSource());

                break;
            case TYPE_EMPTY: // nothing to do
                break;
            case TYPE_ERROR:
                ErrorViewHolder evh = (ErrorViewHolder) holder;
                evh.setPage(mNewsList.page);
                break;
            default:
        }
    }

    @Override
    public int getItemCount() {
        switch (mNewsList.code){
            case NewsResult.OK:
                int count = mNewsList.news.size();
                if (mNewsList.hasPrevious()) count++;
                if (mNewsList.hasNext()) count++;
                return count;
            case NewsResult.EMPTY:
            case NewsResult.ERROR:
                return 1;
        }
        return 0;
    }

    // called from onLoaderReset
    public void clear() {
        mNewsList.clear();
        notifyDataSetChanged();
    }

    public void update(NewsResult data) {
        // here we change the mNewsList
        mNewsList.clear();
        mNewsList.copy(data); //copy new data
        notifyDataSetChanged();
    }

}
