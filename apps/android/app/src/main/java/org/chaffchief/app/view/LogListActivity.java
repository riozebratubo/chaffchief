package org.chaffchief.app.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.chaffchief.app.R;
import org.chaffchief.app.database.DatabaseHelper;
import org.chaffchief.app.database.model.Log;

import org.chaffchief.app.utils.MyDividerItemDecoration;
import org.chaffchief.app.utils.RecyclerTouchListener;

public class LogListActivity extends AppCompatActivity {
    private LogsAdapter mAdapter;
    private final List<Log> logsList = new ArrayList<>();
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private TextView noNotesView;

    public static final String INTENT_MESSAGE_PREFIX = "org.chaffchief.app.";

    private DatabaseHelper db;

    private LogListActivity instance;

    int editingPosition;

    public static void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper
                            .getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod(
                            "setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs_list);

        instance = this;

        getSupportActionBar().setTitle(getResources().getString(R.string.activity_title_logs, getResources().getString(R.string.app_name)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        coordinatorLayout = findViewById(R.id.coordinator_layout2);
        recyclerView = findViewById(R.id.logs_list_recycler_view);
        noNotesView = findViewById(R.id.empty_logs_list_view);

        db = new DatabaseHelper(this);

        logsList.addAll(db.getLogs());

        mAdapter = new LogsAdapter(this, logsList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recyclerView.setAdapter(mAdapter);

        toggleEmptyLogList();

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this,
                recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, final int position) {
                Log log;

                log = logsList.get(position);

                Intent intent = new Intent(getApplicationContext(), PastLogViewerActivity.class);

                intent.putExtra(INTENT_MESSAGE_PREFIX + "log_id", log.getId());
                intent.putExtra(INTENT_MESSAGE_PREFIX + "log_position", position);

                editingPosition = position;

                startActivityForResult(intent, 1);
            }

            @Override
            public void onLongClick(final View view, final int position) {
                PopupMenu popup = new PopupMenu(recyclerView.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.menu_logs, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        Log logToShare;

                        switch (item.getItemId()) {
                            case R.id.menu_log_popup_delete:

                                deleteLog(position);

                                return true;
                            case R.id.menu_log_popup_share_as_csv:

                                logToShare = logsList.get(position);
                                logToShare = db.getLog(logToShare.getId());

                                String logDateTime = "";

                                try {
                                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    Date date = fmt.parse(logToShare.getTimestamp());
                                    SimpleDateFormat fmtOut = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
                                    logDateTime = fmtOut.format(date);
                                } catch (ParseException e) {

                                }

                                String logFileTitle = logDateTime + "-" + logToShare.getTitle() + ".csv";

                                File file = new File(getFilesDir(),"logs");
                                if (!file.exists()){
                                    file.mkdir();
                                }
                                try {
                                    File f = new File(file, logFileTitle);
                                    FileWriter writer = new FileWriter(f);
                                    writer.append("\"Time in microseconds\",\"Measured temp\",\"Power state\",\"Fan state\",\"Target temp\",\"Target fan\",\"Event\"\n");
                                    writer.append(
                                            TextUtils.join("\n", logToShare.getLinesWithEvents(instance))
                                    );
                                    writer.flush();
                                    writer.close();

                                } catch (Exception e){

                                    Toast.makeText(getApplicationContext(), "Error creating file", Toast.LENGTH_SHORT).show();

                                }


                                File logsPath = new File(getFilesDir(), "logs");
                                File newFile = new File(logsPath, logFileTitle);
                                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "org.chaffchief.app.provider", newFile);

                                Intent shareIntent = new Intent();
                                shareIntent.setAction(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                                shareIntent.setType("text/csv");
                                startActivity(Intent.createChooser(shareIntent, "Share log via..."));

                                return true;

                            case R.id.menu_log_popup_share_as_text:

                                logToShare = logsList.get(position);
                                logToShare = db.getLog(logToShare.getId());

                                StringBuilder textToShare = new StringBuilder( );

                                textToShare.append("\"Time in microseconds\",\"Measured temp\",\"Power state\",\"Fan state\",\"Target temp\",\"Target fan\",\"Event\"\n");
                                textToShare.append(
                                    TextUtils.join("\n", logToShare.getLinesWithEvents(instance))
                                );

                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare.toString());
                                sendIntent.setType("text/plain");
                                startActivity(sendIntent);

                                return true;

                            default:
                                return false;
                        }
                    }
                });
                setForceShowIcon(popup);
                popup.show();
            }
        }));
    }

    private void deleteLog(int position) {
        db.deleteLog(logsList.get(position));
        logsList.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                return true;
            default:
        }
        return true;
    }

    private void toggleEmptyLogList() {
        if (db.getLogsCount() > 0) {
            noNotesView.setVisibility(View.GONE);
        } else {
            noNotesView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                logsList.set(editingPosition, db.getLog(logsList.get(editingPosition).getId()));
                mAdapter.notifyItemChanged(editingPosition);
            }
        }
    }
}
