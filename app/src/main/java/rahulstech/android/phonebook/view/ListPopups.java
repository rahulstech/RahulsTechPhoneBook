package rahulstech.android.phonebook.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.PopupWindowCompat;
import rahulstech.android.phonebook.R;
import rahulstech.android.phonebook.util.Check;

import static rahulstech.android.phonebook.util.Helpers.dpToPx;
import static rahulstech.android.phonebook.util.Helpers.logDebug;

public class ListPopups {

    private static final String TAG = "ListPopup";

    public static PopupWindow showContextMenu(@NonNull Context context, @NonNull View anchor, @Nullable String title,
                                              @NonNull ListAdapter adapter, @Nullable OnMenuItemClickListener itemClickListener) {
        Check.isNonNull(context,"null == context");
        Check.isNonNull(anchor,"null == anchor");
        Check.isNonNull(adapter,"null == adapter");

        View view = View.inflate(context, R.layout.context_menu_with_title_layout,null);
        TextView titleView = view.findViewById(R.id.title);
        ListView menu = view.findViewById(R.id.menu);
        View divider = view.findViewById(R.id.divider);

        if (!Check.isEmptyString(title)) {
            titleView.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            titleView.setText(title);
        }

        PopupWindow popup = new PopupWindow(view, (int) dpToPx(context,150), WindowManager.LayoutParams.WRAP_CONTENT,true);
        popup.setFocusable(true);
        popup.setBackgroundDrawable(ResourcesCompat.getDrawable(context.getResources(),R.drawable.popup_window_background,context.getTheme()));

        menu.setAdapter(adapter);
        menu.setOnItemClickListener((list,item,position,id)->{
            if (null != itemClickListener) itemClickListener.onItemClick(popup,position);
            popup.dismiss();
        });

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int menuHeight = view.getMeasuredHeight();

        int anchorHeight = anchor.getMeasuredHeight();
        ViewGroup.MarginLayoutParams anchorParams = (ViewGroup.MarginLayoutParams) anchor.getLayoutParams();
        int[] anchorLocation = new int[2];
        anchor.getLocationOnScreen(anchorLocation);
        int anchorX = anchorLocation[0];
        int anchorTopY = anchorLocation[1];
        int anchorBottomY = anchorLocation[1]+anchorHeight;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int screenBelowAnchor = screenHeight-anchorBottomY;
        int menuX = anchorX;
        int menuY = screenBelowAnchor > menuHeight ? anchorBottomY : anchorTopY-menuHeight;

        logDebug(TAG, "screenBelowAnchor: "+screenBelowAnchor+" menuHeight: "+menuHeight+" (menuX,menuY): ("+menuX+","+menuY+")");

        popup.showAtLocation(anchor,Gravity.NO_GRAVITY,menuX,menuY);

        return popup;
    }

    public static PopupWindow showContextMenu(@NonNull Context context, @NonNull View anchor, @Nullable String title,
                                              @NonNull String[] items, @Nullable OnMenuItemClickListener itemClickListener) {
        Check.isNonNull(context,"null == context");
        if (null == items || items.length==0) throw new IllegalArgumentException("no menu items");
        return showContextMenu(context,anchor,title,
                new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line,items),
                itemClickListener);
    }

    public interface OnMenuItemClickListener {
        void onItemClick(PopupWindow popup, int position);
    }
}
