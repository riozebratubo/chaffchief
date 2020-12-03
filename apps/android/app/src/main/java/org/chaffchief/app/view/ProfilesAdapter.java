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

import org.chaffchief.app.database.model.Profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.chaffchief.app.R;

public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.MyViewHolder> {

    private final Context context;
    private final List<Profile> profilesList;

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, PopupMenu.OnMenuItemClickListener {
        public TextView profileTitle;
        public TextView dot;
        public TextView timestamp;
        public TextView menuOpener;

        public MyViewHolder(View view) {
            super(view);
            profileTitle = view.findViewById(R.id.profile_title);
            dot = view.findViewById(R.id.dot);
            timestamp = view.findViewById(R.id.timestamp);
            menuOpener = view.findViewById(R.id.profile_options_menu_opener);
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


    public ProfilesAdapter(Context context, List<Profile> profilesList) {
        this.context = context;
        this.profilesList = profilesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Profile profile = profilesList.get(position);

        holder.profileTitle.setText(profile.getTitle());

        holder.dot.setText(Html.fromHtml("&#8226;"));

        holder.timestamp.setText(formatDate(profile.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return profilesList.size();
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = fmt.parse(dateStr);
            SimpleDateFormat fmtOut = new SimpleDateFormat("dd/MM/yyyy");
            return fmtOut.format(date);
        } catch (ParseException e) {

        }

        return "";
    }
}
