package rahulstech.android.phonebook.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import rahulstech.android.phonebook.util.Check;

public class Animations {

    public static final long DURATION_FAST = 200;

    public static final long DURATION_NORMAL = 400;

    public static final long DURATION_SLOW = 550;

    public static Animator translateInLeft(@NonNull View view, float translationX, long duration) {
        return translateX(view,translationX,0,duration,new DecelerateInterpolator());
    }

    public static Animator translateOutRight(@NonNull View view, float translationX, long duration) {
        return translateX(view,0,translationX,duration,new AccelerateInterpolator());
    }

    public static Animator translateX(@NonNull View view, float from, float to, long duration, Interpolator interpolator) {
        Check.isNonNull(view,"null == view");
        ObjectAnimator animator = ObjectAnimator.ofFloat(view,"translationX",from,to);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setTranslationX(from);
            }
        });
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    public static Animator expandXY(@NonNull View child, long duration) {
        return scaleXY(child,0,1,true,true,duration,new DecelerateInterpolator());
    }

    public static Animator shrinkXY(@NonNull View child, long duration) {
        return scaleXY(child,1,0,true,true,duration,new AccelerateInterpolator());
    }

    public static Animator expandHeight(@NonNull View view, long duration) {
        int finalHeight = measureHeight(view);
        return animateHeight(view,0,finalHeight,duration,new DecelerateInterpolator());
    }

    public static Animator shrinkHeight(@NonNull View view, long duration) {
        int initialHeight = measureHeight(view);
        return animateHeight(view,initialHeight,0,duration,new AccelerateInterpolator());
    }

    public static Animator animateHeight(@NonNull View view, int from, int to, long duration, Interpolator interpolator) {
        ValueAnimator animator = ValueAnimator.ofInt(from,to);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = from;
                view.setLayoutParams(params);
            }
        });
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = value;
            view.setLayoutParams(params);
        });
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    public static Animator animateWidth(@NonNull View view, int from, int to, long duration, Interpolator interpolator) {
        ValueAnimator animator = ValueAnimator.ofInt(from,to);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = from;
                view.setLayoutParams(params);
            }
        });
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = value;
            view.setLayoutParams(params);
        });
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    public static Animator scaleXY(@NonNull View view, float from, float to, boolean animateX, boolean animateY, long duration, Interpolator interpolator) {
        Check.isNonNull(view,"null == view");
        ValueAnimator animator = ValueAnimator.ofFloat(from,to);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (animateX) view.setScaleX(from);
                if (animateY) view.setScaleY(from);
            }
        });
        animator.addUpdateListener(anim -> {
            float value = (float) anim.getAnimatedValue();
            if (animateX) view.setScaleX(value);
            if (animateY) view.setScaleY(value);
        });
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    public static Animator fadeIn(@NonNull View view, long duration) {
        return fade(view,0,1,duration,new LinearInterpolator());
    }

    public static Animator fadeOut(@NonNull View view, long duration) {
        return fade(view,1,0,duration,new LinearInterpolator());
    }

    public static Animator fade(@NonNull View view, float from, float to, long duration, @NonNull Interpolator interpolator) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view,"alpha",from,to);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setAlpha(from);
            }
        });
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        return animator;
    }

    public static Animator blink(@NonNull View view, long duration) {
        Check.isNonNull(view,"null == view");
        ObjectAnimator animator = ObjectAnimator.ofFloat(view,"alpha",1,0);
        animator.setDuration(duration);
        animator.setRepeatCount(3);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        return animator;
    }

    public static int measureHeight(@NonNull View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredHeight();
    }

    public static int measureWidth(@NonNull View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return view.getMeasuredWidth();
    }
}
