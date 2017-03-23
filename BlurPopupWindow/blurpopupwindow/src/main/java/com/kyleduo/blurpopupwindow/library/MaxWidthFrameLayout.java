package com.kyleduo.blurpopupwindow.library;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by kyle on 2017/3/23.
 */

class MaxWidthFrameLayout extends FrameLayout {
	private float mMaxWidthRatio;
	private int mMaxWidth;

	public MaxWidthFrameLayout(@NonNull Context context) {
		super(context);
	}

	public void setMaxWidthRatio(float maxWidthRatio) {
		mMaxWidthRatio = maxWidthRatio;
	}

	public void setMaxWidth(int maxWidth) {
		mMaxWidth = maxWidth;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int max = 0;
		if (mMaxWidth != 0) {
			max = mMaxWidth;
		} else if (mMaxWidthRatio != 0) {
			max = (int) (getResources().getDisplayMetrics().widthPixels * mMaxWidthRatio);
		}
		if (max > 0) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(
					Math.min(MeasureSpec.getSize(widthMeasureSpec), max),
					MeasureSpec.getMode(widthMeasureSpec));
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec) {
		super.measureChild(child, parentWidthMeasureSpec, parentHeightMeasureSpec);
	}
}
