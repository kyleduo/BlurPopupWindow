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
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * PopupWindow with blurred below view.
 * Created by kyle on 2017/3/14.
 */

@SuppressWarnings("ALL")
public class BlurPopupWindow extends FrameLayout {
	private static final String TAG = "BlurPopupWindow";

	private static final float DEFAULT_BLUR_RADIUS = 10;
	private static final float DEFAULT_SCALE_RATIO = 0.4f;
	private static final long DEFAULT_ANIMATION_DURATION = 300;

	private Activity mActivity;
	protected ImageView mBlurView;
	protected FrameLayout mContentLayout;
	private boolean mAnimating;

	private WindowManager mWindowManager;

	private View mContentView;
	private int mTintColor;
	private View mAnchorView;
	private float mBlurRadius;
	private float mScaleRatio;
	private long mAnimationDuration;
	private boolean mDismissOnTouchBackground;
	private boolean mDismissOnClickBack;

	public BlurPopupWindow(@NonNull Context context) {
		super(context);
		init();
	}

	private void init() {
		if (!(getContext() instanceof Activity)) {
			throw new IllegalArgumentException("Context must be Activity");
		}
		mActivity = (Activity) getContext();
		mWindowManager = mActivity.getWindowManager();

		mBlurRadius = DEFAULT_BLUR_RADIUS;
		mScaleRatio = DEFAULT_SCALE_RATIO;
		mAnimationDuration = DEFAULT_ANIMATION_DURATION;

		setFocusable(true);
		setFocusableInTouchMode(true);

		mContentLayout = new FrameLayout(getContext());
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		addView(mContentLayout, lp);

		mBlurView = new ImageView(mActivity);
		mBlurView.setScaleType(ImageView.ScaleType.FIT_XY);
		lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.gravity = Gravity.BOTTOM;
		mBlurView.setLayoutParams(lp);
		mContentLayout.addView(mBlurView);

		mContentView = createContentView(mContentLayout);
		if (mContentView != null) {
			mContentLayout.addView(mContentView);
		}
	}

	/**
	 * Override this to create custom content.
	 *
	 * @param parent the parent where content view would be.
	 * @return
	 */
	protected View createContentView(ViewGroup parent) {
		return null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mAnimating || !mDismissOnTouchBackground) {
			return super.onTouchEvent(event);
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			dismiss();
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mAnimating || !mDismissOnClickBack) {
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
		mContentLayout.addView(mContentView);
	}

	public View getContentView() {
		return mContentView;
	}

	public void show() {
		if (mAnimating) {
			return;
		}

		WindowManager.LayoutParams params = new WindowManager.LayoutParams();
		params.width = WindowManager.LayoutParams.MATCH_PARENT;
		params.height = WindowManager.LayoutParams.MATCH_PARENT;
		params.format = PixelFormat.RGBA_8888;

		int statusBarHeight = 0;
		int navigationBarHeight = BlurPopupWindow.getNaviHeight(mActivity);
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}

		int trimTopHeight = statusBarHeight;
		int trimBottomHeight = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

			// No need to trim status bar height in SDK > 21.
			trimTopHeight = 0;

			WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
			if ((lp.flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				trimBottomHeight = navigationBarHeight;
			}

			// This line will cause decor view fill all the screen, even if FLAG_TRANSLUCENT_NAVIGATION
			// was not set.
			params.flags = lp.flags;

			if (trimBottomHeight > 0) {

				// If trimBottomHeight > 0, it means that we cut navigation bar off and we need shrink
				// popup windows' content height by increase bottom padding.
				setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom() + navigationBarHeight);
			} else {

				// If navigation is showing on the screen, whether translucent or not, we should move contentView
				// on top of it.
				boolean moveContent = false;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					moveContent = true;
				} else if (navigationBarHeight > 0 && (lp.flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) != 0) {
					// Navigation feature diffs from v19 to v21.
					moveContent = true;
				}
				if (navigationBarHeight > 0 && moveContent) {
					if (mContentView != null) {
						MarginLayoutParams layoutParams = (MarginLayoutParams) mContentView.getLayoutParams();
						layoutParams.bottomMargin += navigationBarHeight;
					}
				}
			}
		}

		new BlurTask(mActivity.getWindow().getDecorView(), trimTopHeight, trimBottomHeight, this, new BlurTask.BlurTaskCallback() {
			@Override
			public void onBlurFinish(Bitmap bitmap) {
				onBlurredImageGot(bitmap);
			}
		}).execute();

		mWindowManager.addView(this, params);

		ObjectAnimator showAnimator = createShowAnimator();
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
		onShow();
	}

	public void dismiss() {
		if (mAnimating) {
			return;
		}
		onDismiss();
		ObjectAnimator animator = createDismissAnimator();
		if (animator == null) {
			mWindowManager.removeView(this);
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
					try {
						mWindowManager.removeView(BlurPopupWindow.this);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						mAnimating = false;
					}
				}
			});
			animator.start();
		}
	}

	protected void onBlurredImageGot(Bitmap bitmap) {
		mBlurView.setImageBitmap(bitmap);
		if (!mAnimating) {
			ObjectAnimator.ofFloat(mBlurView, "alpha", 0, 1f).setDuration(mAnimationDuration).start();
		}
	}

	protected void onShow() {
	}

	protected void onDismiss() {
	}

	protected ObjectAnimator createShowAnimator() {
		return ObjectAnimator.ofFloat(mContentLayout, "alpha", 0, 1.f).setDuration(mAnimationDuration);
	}

	protected ObjectAnimator createDismissAnimator() {
		return ObjectAnimator.ofFloat(mContentLayout, "alpha", mContentLayout.getAlpha(), 0).setDuration(mAnimationDuration);
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

	@AnyThread
	public float getBlurRadius() {
		return mBlurRadius;
	}

	public void setBlurRadius(float blurRadius) {
		mBlurRadius = blurRadius;
	}

	@AnyThread
	public float getScaleRatio() {
		return mScaleRatio;
	}

	public void setScaleRatio(float scaleRatio) {
		mScaleRatio = scaleRatio;
	}

	public long getAnimationDuration() {
		return mAnimationDuration;
	}

	public void setAnimationDuration(long animationDuration) {
		mAnimationDuration = animationDuration;
	}

	public boolean isDismissOnTouchBackground() {
		return mDismissOnTouchBackground;
	}

	public void setDismissOnTouchBackground(boolean dismissOnTouchBackground) {
		mDismissOnTouchBackground = dismissOnTouchBackground;
	}

	public boolean isDismissOnClickBack() {
		return mDismissOnClickBack;
	}

	public void setDismissOnClickBack(boolean dismissOnClickBack) {
		mDismissOnClickBack = dismissOnClickBack;
	}

	public static class Builder<T extends BlurPopupWindow> {
		private static final String TAG = "BlurPopupWindow.Builder";
		protected Context mContext;
		private View mContentView;
		private int mTintColor;
		private float mBlurRadius;
		private float mScaleRatio;
		private long mAnimationDuration;
		private boolean mDismissOnTouchBackground = true;
		private boolean mDismissOnClickBack = true;
		private int mGravity = -1;

		public Builder(Context context) {
			mContext = context;

			mBlurRadius = BlurPopupWindow.DEFAULT_BLUR_RADIUS;
			mScaleRatio = BlurPopupWindow.DEFAULT_SCALE_RATIO;
			mAnimationDuration = BlurPopupWindow.DEFAULT_ANIMATION_DURATION;
		}

		public Builder setContentView(View contentView) {
			mContentView = contentView;
			return this;
		}

		public Builder setContentView(int resId) {
			View view = LayoutInflater.from(mContext).inflate(resId, new FrameLayout(mContext), false);
			mContentView = view;
			return this;
		}

		public Builder bindContentViewClickListener(View.OnClickListener listener) {
			if (mContentView != null) {
				mContentView.setClickable(true);
				mContentView.setOnClickListener(listener);
			}
			return this;
		}

		public Builder bindClickListener(View.OnClickListener listener, int... views) {
			if (mContentView != null) {
				for (int viewId : views) {
					View view = mContentView.findViewById(viewId);
					if (view != null) {
						view.setOnClickListener(listener);
					}
				}
			}
			return this;
		}

		public Builder setGravity(int gravity) {
			mGravity = gravity;
			return this;
		}

		public Builder setTintColor(int tintColor) {
			mTintColor = tintColor;
			return this;
		}

		public Builder setScaleRatio(float scaleRatio) {
			if (scaleRatio <= 0 || scaleRatio > 1) {
				Log.w(TAG, "scaleRatio invalid: " + scaleRatio + ". It can only be (0, 1]");
				return this;
			}
			mScaleRatio = scaleRatio;
			return this;
		}

		public Builder setBlurRadius(float blurRadius) {
			if (blurRadius < 0 || blurRadius > 25) {
				Log.w(TAG, "blurRadius invalid: " + blurRadius + ". It can only be [0, 25]");
				return this;
			}
			mBlurRadius = blurRadius;
			return this;
		}

		public Builder setAnimationDuration(long animatingDuration) {
			if (animatingDuration < 0) {
				Log.w(TAG, "animatingDuration invalid: " + animatingDuration + ". It can only be (0, ..)");
				return this;
			}
			mAnimationDuration = animatingDuration;
			return this;
		}

		public Builder setDismissOnTouchBackground(boolean dismissOnTouchBackground) {
			mDismissOnTouchBackground = dismissOnTouchBackground;
			return this;
		}

		public Builder setDismissOnClickBack(boolean dismissOnClickBack) {
			mDismissOnClickBack = dismissOnClickBack;
			return this;
		}

		protected T createPopupWindow() {
			//noinspection unchecked
			return (T) new BlurPopupWindow(mContext);
		}

		public T build() {
			T popupWindow = createPopupWindow();
			if (mContentView != null) {
				ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
				if (layoutParams == null || !(layoutParams instanceof FrameLayout.LayoutParams)) {
					layoutParams = new FrameLayout.LayoutParams(layoutParams.width, layoutParams.height);
				}
				if (mGravity != -1) {
					((LayoutParams) layoutParams).gravity = mGravity;
				}
				mContentView.setLayoutParams(layoutParams);
				popupWindow.setContentView(mContentView);
			}
			popupWindow.setTintColor(mTintColor);
			popupWindow.setAnimationDuration(mAnimationDuration);
			popupWindow.setBlurRadius(mBlurRadius);
			popupWindow.setScaleRatio(mScaleRatio);
			popupWindow.setDismissOnTouchBackground(mDismissOnTouchBackground);
			popupWindow.setDismissOnClickBack(mDismissOnClickBack);
			return popupWindow;
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

		BlurTask(View sourceView, int statusBarHeight, int navigationBarheight, BlurPopupWindow popupWindow, BlurTaskCallback blurTaskCallback) {
			mContextRef = new WeakReference<>(sourceView.getContext());
			mPopupWindowRef = new WeakReference<>(popupWindow);
			mBlurTaskCallback = blurTaskCallback;

			int height = sourceView.getHeight() - statusBarHeight - navigationBarheight;
			if (height < 0) {
				height = sourceView.getHeight();
			}

			Drawable background = sourceView.getBackground();
			mSourceBitmap = Bitmap.createBitmap(sourceView.getWidth(), height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(mSourceBitmap);
			int saveCount = 0;
			if (statusBarHeight != 0) {
				saveCount = canvas.save();
				canvas.translate(0, -statusBarHeight);
			}
			if (background == null) {
				canvas.drawColor(0xffffffff);
			}
			sourceView.draw(canvas);
			if (popupWindow.getTintColor() != 0) {
				canvas.drawColor(popupWindow.getTintColor());
			}
			if (statusBarHeight != 0 && saveCount != 0) {
				canvas.restoreToCount(saveCount);
			}
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Context context = mContextRef.get();
			BlurPopupWindow popupWindow = mPopupWindowRef.get();
			if (context == null || popupWindow == null) {
				return null;
			}
			float scaleRatio = popupWindow.getScaleRatio();
			if (popupWindow.getBlurRadius() == 0) {
				return mSourceBitmap;
			}
			Bitmap scaledBitmap = Bitmap.createScaledBitmap(mSourceBitmap, (int) (mSourceBitmap.getWidth() * scaleRatio), (int) (mSourceBitmap.getHeight() * scaleRatio), false);
			float radius = popupWindow.getBlurRadius();
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

	private static int getNaviHeight(Activity activity) {
		if (activity == null) {
			return 0;
		}
		Display display = activity.getWindowManager().getDefaultDisplay();
		int contentHeight = activity.getResources().getDisplayMetrics().heightPixels;
		int realHeight = 0;
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			final DisplayMetrics metrics = new DisplayMetrics();
			display.getRealMetrics(metrics);
			realHeight = metrics.heightPixels;
		} else {
			try {
				Method mGetRawH = Display.class.getMethod("getRawHeight");
				realHeight = (Integer) mGetRawH.invoke(display);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return realHeight - contentHeight;
	}

}
