package com.kyleduo.blurpopupwindow.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import static android.graphics.Bitmap.createBitmap;

/**
 * PopupWindow with blurred below view.
 * Created by kyle on 2017/3/14.
 */

public class BlurPopupWindow extends FrameLayout {
	private static final String TAG = "BlurPopupWindow";

	private static final float DEFAULT_BLUR_RADIUS = 10;
	private static final float DEFAULT_SCALE_RATIO = 0.4f;
	private static final long DEFAULT_ANIMATING_DURATION = 300;

	private Activity mActivity;
	private View mContentView;
	protected ImageView mBlurView;
	private boolean mAnimating;
	private int mTintColor;
	private View mAnchorView;
	private float mBlurRadius;
	private float mScaleRatio;
	private long mAnimatingDuration;

	public BlurPopupWindow(@NonNull Context context) {
		super(context);
		init();
	}

	private void init() {
		if (!(getContext() instanceof Activity)) {
			throw new IllegalArgumentException("Context must be Activity");
		}
		mActivity = (Activity) getContext();

		mBlurRadius = DEFAULT_BLUR_RADIUS;
		mScaleRatio = DEFAULT_SCALE_RATIO;
		mAnimatingDuration = DEFAULT_ANIMATING_DURATION;

		setFocusable(true);
		setFocusableInTouchMode(true);

		mBlurView = new ImageView(mActivity);
		mBlurView.setScaleType(ImageView.ScaleType.FIT_XY);
		mBlurView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		addView(mBlurView);

		mContentView = createContentView();
		if (mContentView != null) {
			addView(mContentView);
		}
	}

	protected View createContentView() {
		return null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mAnimating) {
			return super.onTouchEvent(event);
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			dismiss();
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mAnimating) {
			return super.onKeyUp(keyCode, event);
		}
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			dismiss();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	public void setContentView(View contentView) {
		if (contentView == null) {
			throw new IllegalArgumentException("contentView can not be null");
		}
		if (mContentView != null) {
			if (mContentView.getParent() != null) {
				((ViewGroup) mContentView.getParent()).removeView(mContentView);
			}
			mContentView = null;
		}
		mContentView = contentView;
		addView(mContentView);
	}

	public View getContentView() {
		return mContentView;
	}

	public void show() {
		if (mAnimating) {
			return;
		}
		ViewGroup.LayoutParams layoutParams = mActivity.getWindow().getDecorView().getLayoutParams();
		new BlurTask(mActivity.getWindow().getDecorView(), this, new BlurTask.BlurTaskCallback() {
			@Override
			public void onBlurFinish(Bitmap bitmap) {
				onBlurredImageGot(bitmap);
			}
		}).execute();

		WindowManager.LayoutParams params = new WindowManager.LayoutParams();
		params.width = WindowManager.LayoutParams.MATCH_PARENT;
		params.height = WindowManager.LayoutParams.MATCH_PARENT;
		params.format = PixelFormat.RGBA_8888;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			params.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		}
		WindowManager windowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
		windowManager.addView(BlurPopupWindow.this, params);
		onShow();
		ObjectAnimator showAnimator = createOnShowAnimator();
		if (showAnimator != null) {
			mAnimating = true;
			showAnimator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationCancel(Animator animation) {
					mAnimating = false;
					requestFocus();
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					mAnimating = false;
					requestFocus();
				}
			});
			showAnimator.start();
		}
	}

	public void dismiss() {
		if (mAnimating) {
			return;
		}
		onDismiss();
		ObjectAnimator animator = createOnDismissAnimator();
		if (animator == null) {
			WindowManager windowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
			windowManager.removeView(this);
		} else {
			mAnimating = true;
			animator.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					removeSelf();
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					removeSelf();
				}

				private void removeSelf() {
					WindowManager windowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
					windowManager.removeView(BlurPopupWindow.this);
					mAnimating = false;
				}
			});
			animator.start();
		}
	}

	protected void onBlurredImageGot(Bitmap bitmap) {
		mBlurView.setImageBitmap(bitmap);
	}

	protected void onShow() {

	}

	protected void onDismiss() {

	}

	protected ObjectAnimator createOnShowAnimator() {
		setAlpha(0);
		return ObjectAnimator.ofFloat(this, "alpha", getAlpha(), 1).setDuration(mAnimatingDuration);
	}

	protected ObjectAnimator createOnDismissAnimator() {
		return ObjectAnimator.ofFloat(this, "alpha", getAlpha(), 0).setDuration(mAnimatingDuration);
	}

	public int getTintColor() {
		return mTintColor;
	}

	public void setTintColor(int tintColor) {
		mTintColor = tintColor;
	}

	public View getAnchorView() {
		return mAnchorView;
	}

	public void setAnchorView(View anchorView) {
		mAnchorView = anchorView;
	}

	public float getBlurRadius() {
		return mBlurRadius;
	}

	public void setBlurRadius(float blurRadius) {
		mBlurRadius = blurRadius;
	}

	public float getScaleRatio() {
		return mScaleRatio;
	}

	public void setScaleRatio(float scaleRatio) {
		mScaleRatio = scaleRatio;
	}

	public long getAnimatingDuration() {
		return mAnimatingDuration;
	}

	public void setAnimatingDuration(long animatingDuration) {
		mAnimatingDuration = animatingDuration;
	}

	public static Builder builder(Context context) {
		return new Builder(context);
	}

	public static class Builder<T extends BlurPopupWindow> {
		protected Context mContext;
		protected View mContentView;

		public Builder(Context context) {
			mContext = context;
		}

		public Builder contentView(View contentView) {
			mContentView = contentView;
			return this;
		}

		public Builder tintColor(int tintColor) {
			return this;
		}

		protected BlurPopupWindow createPopupWindow() {
			return new BlurPopupWindow(mContext);
		}

		public T build() {
			BlurPopupWindow popupWindow = createPopupWindow();
			if (mContentView != null) {
				popupWindow.setContentView(mContentView);
			}
			//noinspection unchecked
			return (T) popupWindow;
		}

	}

	private final static class BlurTask extends AsyncTask<Void, Void, Bitmap> {

		private WeakReference<Context> mContextRef;
		private WeakReference<BlurPopupWindow> mPopupWindowRef;
		private Bitmap mSourceBitmap;
		private BlurTaskCallback mBlurTaskCallback;

		interface BlurTaskCallback {
			void onBlurFinish(Bitmap bitmap);
		}

		BlurTask(View sourceView, BlurPopupWindow popupWindow, BlurTaskCallback blurTaskCallback) {
			mContextRef = new WeakReference<>(sourceView.getContext());
			mPopupWindowRef = new WeakReference<>(popupWindow);
			mBlurTaskCallback = blurTaskCallback;

			Drawable background = sourceView.getBackground();
			mSourceBitmap = createBitmap(sourceView.getWidth(), sourceView.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(mSourceBitmap);
			if (background == null) {
				canvas.drawColor(0xffffffff);
			}
			sourceView.draw(canvas);
			if (popupWindow.getTintColor() != 0) {
				canvas.drawColor(popupWindow.getTintColor());
			}
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Context context = mContextRef.get();
			BlurPopupWindow popupWindow = mPopupWindowRef.get();
			if (context == null || popupWindow == null) {
				return null;
			}
			@SuppressWarnings("WrongThread") float scaleRatio = popupWindow.getScaleRatio();
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(mSourceBitmap, (int) (mSourceBitmap.getWidth() * scaleRatio), (int) (mSourceBitmap.getHeight() * scaleRatio), false);
			@SuppressWarnings("WrongThread") float radius = popupWindow.getBlurRadius();
			Bitmap blurred = BlurUtils.blur(context, scaledBitmap, radius);
			return Bitmap.createScaledBitmap(blurred, mSourceBitmap.getWidth(), mSourceBitmap.getHeight(), true);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			BlurPopupWindow popupWindow = mPopupWindowRef.get();
			if (popupWindow != null && popupWindow.getAnchorView() != null) {
				Canvas canvas = new Canvas(bitmap);
				View anchorView = popupWindow.getAnchorView();
				int[] location = new int[2];
				anchorView.getLocationInWindow(location);
				canvas.save();
				canvas.translate(location[0], location[1]);
				popupWindow.getAnchorView().draw(canvas);
				canvas.restore();
			}
			if (mBlurTaskCallback != null) {
				mBlurTaskCallback.onBlurFinish(bitmap);
			}
		}
	}

}
