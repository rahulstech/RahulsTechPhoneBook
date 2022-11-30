package rahulstech.android.phonebook.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

public class DrawableUtil {

    public static Drawable vectorDrawable(@NonNull Context context, @DrawableRes int resId) {
        Check.isNonNull(context,"null == context");
        return VectorDrawableCompat.create(context.getResources(),resId,context.getTheme());
    }

    public static int getMinimumDimension(@NonNull View view) {
        Check.isNonNull(view,"null == view");
        return Math.min(view.getMeasuredWidth(),view.getMeasuredHeight());
    }

    public static Drawable roundedTextDrawable(@Nullable String text, int start, int end, int radius, @Nullable Drawable ifNull) {
        if (Check.isEmptyString(text)) return ifNull;
        String label = text.substring(start,end);
        int color = ColorGenerator.MATERIAL.getRandomColor();
        return TextDrawable.builder().buildRoundRect(label,color,radius);
    }

    public static Drawable tintedDrawable(@NonNull Drawable drawable, @ColorInt int color) {
        DrawableCompat.setTint(drawable,color);
        return drawable;
    }
}
