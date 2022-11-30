package rahulstech.android.phonebook.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
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

public class ListPopups {

    private static float dpToPx(@NonNull Context context, float dp) {
        return context.getResources().getDisplayMetrics().density*dp;
    }

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

        // TODO: choose gravity accordingly
        PopupWindowCompat.showAsDropDown(popup,anchor,0,0, Gravity.NO_GRAVITY);

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
