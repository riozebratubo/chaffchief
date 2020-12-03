package org.chaffchief.app.view;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.chaffchief.app.R;

public class TooltipWindow {

    private static final int MSG_DISMISS_TOOLTIP = 100;
    private final Context ctx;
    private final PopupWindow tipWindow;
    private final View contentView;
    private final LayoutInflater inflater;

    public TooltipWindow(Context ctx) {
        this.ctx = ctx;
        tipWindow = new PopupWindow(ctx);

        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        contentView = inflater.inflate(R.layout.tooltip_dialog, null);
    }

    public void setText(String text) {
        TextView textViewText = contentView.findViewById(R.id.tooltip_text);
        textViewText.setText(text);
    }

    void showToolTip(View anchor) {

        tipWindow.setHeight(LayoutParams.WRAP_CONTENT);
        tipWindow.setWidth(LayoutParams.WRAP_CONTENT);

        tipWindow.setOutsideTouchable(false);
        tipWindow.setFocusable(false);
        tipWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        tipWindow.setContentView(contentView);

        int[] screen_pos = new int[2];
        anchor.getLocationOnScreen(screen_pos);

        Rect anchor_rect = new Rect(screen_pos[0], screen_pos[1], screen_pos[0]
                + anchor.getWidth(), screen_pos[1] + anchor.getHeight());

        contentView.measure(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        int contentViewHeight = contentView.getMeasuredHeight();
        int contentViewWidth = contentView.getMeasuredWidth();
        int position_x = anchor_rect.centerX() - (contentViewWidth / 2);
        int position_y = anchor_rect.bottom - (anchor_rect.height() / 2);

        tipWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, position_x,position_y);

        handler.sendEmptyMessageDelayed(MSG_DISMISS_TOOLTIP, 4000);
    }

    boolean isTooltipShown() {
        return tipWindow != null && tipWindow.isShowing();
    }

    void dismissTooltip() {
        if (tipWindow != null && tipWindow.isShowing())
            tipWindow.dismiss();
    }

    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DISMISS_TOOLTIP:
                    if (tipWindow != null && tipWindow.isShowing())
                        tipWindow.dismiss();
                    break;
            }
        }
    };

}