package org.chaffchief.app.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.chaffchief.app.database.model.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.chaffchief.app.R;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.MyViewHolder> {

    private final Context context;
    private final List<Log> logsList;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, PopupMenu.OnMenuItemClickListener {
        public TextView logTitle;
        public TextView logComments;
        public TextView dot;
        public TextView timestamp;

        public MyViewHolder(View view) {
            super(view);
            logTitle = view.findViewById(R.id.log_title);
            logComments = view.findViewById(R.id.log_comments);
            dot = view.findViewById(R.id.dot);
            timestamp = view.findViewById(R.id.timestamp);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            //
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return true;
        }
    }


    public LogsAdapter(Context context, List<Log> logsList) {
        this.context = context;
        this.logsList = logsList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.log_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Log log = logsList.get(position);

        holder.logTitle.setText(log.getTitle());

        holder.logComments.setText(log.getComments());
        if (log.getComments().isEmpty()) {
            holder.logComments.setVisibility(View.GONE);
        }
        else {
            holder.logComments.setVisibility(View.VISIBLE);
        }

        // Displaying dot from HTML character code
        holder.dot.setText(Html.fromHtml("&#8226;"));

        // Formatting and displaying timestamp
        String logTimestampLine = formatDate(log.getTimestamp());
        if (log.getRoasterName() != null && !log.getRoasterName().isEmpty()) {
            logTimestampLine = logTimestampLine.concat(" - ").concat(log.getRoasterName());
        }
        else {
            if (log.getRoasterAddress() != null && !log.getRoasterAddress().isEmpty()) {
                logTimestampLine = logTimestampLine.concat(" - ").concat(log.getRoasterAddress());
            }
        }

        holder.timestamp.setText(logTimestampLine);
    }

    @Override
    public int getItemCount() {
        return logsList.size();
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "";

        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = fmt.parse(dateStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            return fmtOut.format(date);
        } catch (ParseException e) {

        }

        return "";
    }
}
