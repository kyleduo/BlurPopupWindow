package com.kyleduo.blurpopupwindow;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by kyle on 2017/3/23.
 */

public class ShadowContainer extends LinearLayout {
	public static final int DEFAULT_SHADOW_COLOR = 0x20000000;
	public static final int DEFAULT_SHADOW_RADIUS_DP = 8;

	private int mShadowRadius;
	private int mShadowColor;
	private ShadowDrawable mShadowDrawable;
	private float mDensity;

	public ShadowContainer(Context context) {
		this(context, null);
	}

	public ShadowContainer(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ShadowContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	protected void init(AttributeSet attrs) {
		setLayerType(LAYER_TYPE_SOFTWARE, null);

		mDensity = getResources().getDisplayMetrics().density;

		mShadowRadius = dp2px(DEFAULT_SHADOW_RADIUS_DP);
		mShadowColor = 0xE0BFCDE6;

		if (attrs != null) {
			TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ShadowContainer);
            mShadowColor = ta.getColor(R.styleable.ShadowContainer_sc_shadowColor, mShadowColor);
			ta.recycle();
		}

		mShadowDrawable = new ShadowDrawable(mDensity);
		mShadowDrawable.setShadow(mShadowRadius, mShadowColor);
		mShadowDrawable.setInset(mShadowRadius, mShadowRadius);
		super.setBackgroundDrawable(mShadowDrawable);

		setPadding(
				getPaddingLeft(),
				getPaddingTop(),
				getPaddingRight(),
				getPaddingBottom());
	}

	private int dp2px(float dp) {
		return (int) (mDensity * dp);
	}

	@Override
	public void setBackgroundColor(@ColorInt int color) {
		mShadowDrawable.setBackgroundColor(color);
	}

	@Override
	public void setBackgroundDrawable(Drawable background) {
		// do nothing
	}

	public ShadowDrawable getShadowDrawable() {
		return mShadowDrawable;
	}

	@Override
	public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
		left += mShadowRadius;
		top += mShadowRadius;
		right += mShadowRadius;
		bottom += mShadowRadius;
		super.setPadding(left, top, right, bottom);
	}

	public void setShadowRadius(int shadowRadius) {
		int pl = getPaddingLeft() - mShadowRadius;
		int pt = getPaddingTop() - mShadowRadius;
		int pr = getPaddingRight() - mShadowRadius;
		int pb = getPaddingBottom() - mShadowRadius;
		mShadowRadius = shadowRadius;
		mShadowDrawable.setShadow(mShadowRadius, mShadowColor);
		mShadowDrawable.setInset(mShadowRadius, mShadowRadius);
		setPadding(pl, pt, pr, pb);
	}

	public void setShadowColor(int shadowColor) {
		mShadowColor = shadowColor;
		mShadowDrawable.setShadow(mShadowRadius, mShadowColor);
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(
					(int) Math.min(MeasureSpec.getSize(widthMeasureSpec), getResources().getDisplayMetrics().widthPixels * 0.8f),
					MeasureSpec.AT_MOST
			);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	public static class ShadowDrawable extends Drawable {
		private Paint mPaint;
		private RectF mRectF;
		private int mBackgroundColor;
		private int mInsetX, mInsetY;
		private float mDensity;
		private int[] mBackgroundColors;
		private int mCornerRadius;
		private LinearGradient mLinearGradient;

		public ShadowDrawable(float density) {
			mDensity = density;
			mBackgroundColor = 0xFFFFFFFF;

			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mRectF = new RectF();

			mPaint.setColor(mBackgroundColor);
			mPaint.setStyle(Paint.Style.FILL);

			mCornerRadius = dp2px(6);
		}

		public void setInset(int insetX, int insetY) {
			mInsetX = insetX;
			mInsetY = insetY;
		}

		private int dp2px(float dp) {
			return (int) (mDensity * dp);
		}


		@Override
		public void draw(@NonNull Canvas canvas) {
			mRectF.set(getBounds());
			mRectF.inset(mInsetX, mInsetY);

			if (mBackgroundColors != null) {
				mLinearGradient = new LinearGradient(mRectF.left, mRectF.top, mRectF.right, mRectF.bottom, mBackgroundColors, new float[]{0, 1.f}, Shader.TileMode.CLAMP);
				mPaint.setShader(mLinearGradient);
			}

			canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);
		}

		@Override
		public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
			mPaint.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter) {
			mPaint.setColorFilter(colorFilter);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.TRANSLUCENT;
		}

		public void setShadow(int radius, int color) {
			mPaint.setShadowLayer(radius, 0, 0, color);
			invalidateSelf();
		}

		public void setBackgroundColor(int backgroundColor) {
			mBackgroundColor = backgroundColor;
			invalidateSelf();
		}

		public LinearGradient getLinearGradient() {
			return mLinearGradient;
		}

		public void setColors(int[] colors) {
			mBackgroundColors = colors;
			invalidateSelf();
		}

		public void setCornerRadius(int cornerRadius) {
			mCornerRadius = cornerRadius;
			invalidateSelf();
		}
	}

}
