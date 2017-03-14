package com.kyleduo.blurpopupwindow;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.kyleduo.blurpopupwindow.library.BlurPopupWindow;

/**
 * Created by kyle on 2017/3/14.
 */

public class IOSMenu extends BlurPopupWindow {
	private static final String TAG = "IOSMenu";

	public IOSMenu(@NonNull Context context) {
		super(context);
	}

	@Override
	protected View createContentView() {
		View menu = LayoutInflater.from(getContext()).inflate(R.layout.layout_ios, this, false);
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.BOTTOM;
		menu.setLayoutParams(lp);
		menu.setVisibility(INVISIBLE);
		return menu;
	}

	@Override
	protected void onShow() {
		super.onShow();
		Log.d(TAG, "height: " + getContentView().getMeasuredHeight());
		getContentView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				getViewTreeObserver().removeGlobalOnLayoutListener(this);

				getContentView().setVisibility(VISIBLE);
				int height = getContentView().getMeasuredHeight();
				ObjectAnimator.ofFloat(getContentView(), "translationY", height, 0).setDuration(300).start();
			}
		});
	}

	@Override
	protected void onDismiss() {
		super.onDismiss();
		int height = getContentView().getMeasuredHeight();
		ObjectAnimator.ofFloat(getContentView(), "translationY", 0, height).setDuration(300).start();
	}
}
