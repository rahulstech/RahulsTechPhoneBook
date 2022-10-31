package rahulstech.android.phonebook.view;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

import rahulstech.android.phonebook.util.Check;

public class Animations {

    public static final long SHORT_DURATION = 800;

    public static void crossFade(View hide, View show, Runnable OnComplete) {
        Check.isNonNull(hide,"null == hide view");
        Check.isNonNull(show,"null == show view");

        Animation fade_in = new AlphaAnimation(0,1);
        fade_in.setDuration(SHORT_DURATION);
        fade_in.setStartOffset(200);
        fade_in.setInterpolator(new DecelerateInterpolator());
        fade_in.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                show.setVisibility(View.VISIBLE);
            }
        });

        Animation fade_out = new AlphaAnimation(1,0);
        fade_out.setDuration(SHORT_DURATION);
        fade_out.setInterpolator(new AccelerateInterpolator());
        fade_out.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                hide.setVisibility(View.GONE);
            }
        });

        hide.startAnimation(fade_out);
        show.startAnimation(fade_in);
    }

    public static void scaleAndFade(ViewGroup container, View hide, View show, Runnable OnComplete) {
        Check.isNonNull(hide,"null == hide view");
        Check.isNonNull(show,"null == show view");

        int from_height = hide.getMeasuredHeight();
        int from_width = hide.getMeasuredWidth();
        int to_height = show.getMeasuredHeight();
        int to_width = show.getMeasuredWidth();

        float toX = to_width/from_width;
        float toY = to_height/from_height;

        Animation scale = new ScaleAnimation(1,toX,1,toY);
        scale.setInterpolator(new DecelerateInterpolator());
        scale.setDuration(SHORT_DURATION);
        scale.setAnimationListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animation animation) {
                hide.setVisibility(View.GONE);
                show.setVisibility(View.VISIBLE);
            }
        });

        container.startAnimation(scale);
    }

    public static class SimpleAnimationListener implements Animation.AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }
}
