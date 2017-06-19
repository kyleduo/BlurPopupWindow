package com.kyleduo.blurpopupwindow;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.kyleduo.blurpopupwindow.library.BlurPopupWindow;

/**
 * Created by kyle on 2017/3/14.
 */

public class BottomMenu extends BlurPopupWindow {
    private static final String TAG = "IOSMenu";

    public BottomMenu(@NonNull Context context) {
        super(context);
    }

    @Override
    protected View createContentView(ViewGroup parent) {
        View menu = LayoutInflater.from(getContext()).inflate(R.layout.layout_bottom_menu, parent, false);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        menu.setLayoutParams(lp);
        menu.setVisibility(INVISIBLE);

        menu.findViewById(R.id.cancel_action).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return menu;
    }

    @Override
    protected void onShow() {
        super.onShow();
        getContentView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);

                getContentView().setVisibility(VISIBLE);
                int height = getContentView().getMeasuredHeight();
                ObjectAnimator.ofFloat(getContentView(), "translationY", height, 0).setDuration(getAnimationDuration()).start();
            }
        });
    }

    @Override
    protected ObjectAnimator createDismissAnimator() {
        int height = getContentView().getMeasuredHeight();
        return ObjectAnimator.ofFloat(getContentView(), "translationY", 0, height).setDuration(getAnimationDuration());
    }

    @Override
    protected ObjectAnimator createShowAnimator() {
        return null;
    }

    public static class Builder extends BlurPopupWindow.Builder<BottomMenu> {
        public Builder(Context context) {
            super(context);
            this.setBlurRadius(0).setTintColor(0x70000000);
        }

        @Override
        protected BottomMenu createPopupWindow() {
            return new BottomMenu(mContext);
        }
    }
}
