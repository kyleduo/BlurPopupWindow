package com.kyleduo.blurpopupwindow;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kyleduo.blurpopupwindow.library.BlurPopupWindow;

/**
 * Created by kyle on 2017/3/14.
 */

public class FullScreenMenu extends BlurPopupWindow {
	public FullScreenMenu(@NonNull Context context) {
		super(context);
	}

	public static Builder builder(Context context) {
		return new Builder(context);
	}

	@Override
	protected View createContentView() {
		View menu = LayoutInflater.from(getContext()).inflate(R.layout.layout_full, this, false);
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		menu.setLayoutParams(lp);
		return menu;
	}

	public static class Builder extends BlurPopupWindow.Builder<FullScreenMenu> {
		protected Builder(Context context) {
			super(context);
			this.tintColor(0x60000000).scaleRatio(0.3f);
		}

		@Override
		protected FullScreenMenu createPopupWindow() {
			return new FullScreenMenu(mContext);
		}
	}

}
