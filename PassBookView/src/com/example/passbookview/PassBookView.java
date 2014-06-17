package com.example.passbookview;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

public class PassBookView extends ViewGroup {

	private static final float COLLAPSED_TIP_FACTOR = 0.7f;
	private static final float CHILD_HEIGHT_FACTOR = 0.87f;
	private static final DecelerateInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
	private static final long SINGLE_TAP_INTERVAL = 800;
	private static final long SINGLE_TOUCH_SLOPE = 20;
	private static final float MAX_ZPOSITION_FACTOR = 0.03f;
	private volatile Mode mode = Mode.LISTED;
	private int width;
	private int height;
	private final SparseIntArray childOffsets = new SparseIntArray();
	private int childHeight;
	private int focusedIndex = -1;
	private final List<Animator> animators = new ArrayList<Animator>();
	private float xDistance, yDistance, lastX, lastY;
	private float startY, startX, beforeY;
	private long startTime;
	private int itemHeight;
	private static final float TOUCH_FEEDBACK_FACTOR = 4f;
	private float currentFeedbackFactor = 0f;
	private final AnimatorSet toListModeSet = new AnimatorSet();
	private final AnimatorSet toPickModeSet = new AnimatorSet();
	private final AnimatorSet revertSet = new AnimatorSet();
	private float bottomSpace;
	private float collapsedChildsShownTip;
	private DataListener<Boolean> onCardSelectListener;
	private float centerX;

	public PassBookView(Context context, AttributeSet attrs, int defStyle) {
		super( context, attrs, defStyle );
		init( context );
	}

	public PassBookView(Context context, AttributeSet attrs) {
		super( context, attrs );
		init( context );
	}

	public PassBookView(Context context) {
		super( context );
		init( context );
	}

	private void init( Context context ) {
		if ( !isInEditMode() ) {
			(( MainActivity ) context).addOnBackPressedListener( new OnBackPressedListener() {

				@Override
				public boolean onBackPressed() {
					if ( mode != Mode.LISTED ) {
						applyListMode();
						return true;
					}
					return false;
				}
			} );
		}
		setClipToPadding( false );
		toListModeSet.addListener( new AnimatorListener() {

			@Override
			public void onAnimationStart( Animator animation ) {
				if ( onCardSelectListener != null ) {
					onCardSelectListener.onFinish( false );
				}
			}

			@Override
			public void onAnimationRepeat( Animator animation ) {
			}

			@Override
			public void onAnimationEnd( Animator animation ) {
				PassBookView.this.mode = Mode.LISTED;
			}

			@Override
			public void onAnimationCancel( Animator animation ) {
			}
		} );
		toListModeSet.setDuration( 300 );
		toListModeSet.setInterpolator( DECELERATE_INTERPOLATOR );

		toPickModeSet.addListener( new AnimatorListener() {

			@Override
			public void onAnimationStart( Animator animation ) {
				if ( onCardSelectListener != null ) {
					onCardSelectListener.onFinish( true );
				}
			}

			@Override
			public void onAnimationRepeat( Animator animation ) {
			}

			@Override
			public void onAnimationEnd( Animator animation ) {
				PassBookView.this.mode = Mode.PICKED;
			}

			@Override
			public void onAnimationCancel( Animator animation ) {
			}
		} );
		toPickModeSet.setDuration( 300 );
		toPickModeSet.setInterpolator( DECELERATE_INTERPOLATOR );

		revertSet.addListener( new AnimatorListener() {

			@Override
			public void onAnimationStart( Animator animation ) {
			}

			@Override
			public void onAnimationRepeat( Animator animation ) {
			}

			@Override
			public void onAnimationEnd( Animator animation ) {
				PassBookView.this.mode = Mode.LISTED;
			}

			@Override
			public void onAnimationCancel( Animator animation ) {
			}
		} );
		revertSet.setDuration( 150 );
		revertSet.setInterpolator( DECELERATE_INTERPOLATOR );
	}

	private static enum Mode {
		LISTED, PICKED, ANIMATING
	}

	private void changeMode( Mode mode ) {
		switch ( mode ) {
		case LISTED :
			if ( this.mode == Mode.PICKED ) {
				this.mode = Mode.ANIMATING;
				applyListMode();
			}
			break;

		case PICKED :
			if ( this.mode == Mode.LISTED ) {
				this.mode = Mode.ANIMATING;
				applyPickMode();
			}
			break;
		}
	}

	public void applyListMode() {
		toListModeSet.end();
		animators.clear();
		final int childCount = getChildCount();
		for ( int i = 0 ; i < childCount ; i++ ) {
			View child = getChildAt( i );
			animators.add( ObjectAnimator.ofFloat( child, "translationY", child.getTranslationY(), 0 ) );
			child.setPivotX( centerX );
			child.setPivotY( 0 );
			animators.add( ObjectAnimator.ofFloat( child, "scaleX", child.getScaleX(), 1 ) );
			animators.add( ObjectAnimator.ofFloat( child, "scaleY", child.getScaleY(), 1 ) );
		}
		toListModeSet.playTogether( animators );
		toListModeSet.start();
	}

	private void applyPickMode() {
		toPickModeSet.end();
		animators.clear();
		final int childCount = getChildCount();
		int reverseIndex = childCount - 1;
		int forwardIndex = 0;
		for ( int i = 0 ; i < childCount ; i++ ) {
			View child = getChildAt( i );
			if ( i == focusedIndex ) {
				animators.add( ObjectAnimator.ofFloat( child, "translationY", child.getTranslationY(), -childOffsets.get( i ) ) );
				child.setPivotX( centerX );
				child.setPivotY( 0 );
				animators.add( ObjectAnimator.ofFloat( child, "scaleX", child.getScaleX(), 1 ) );
				animators.add( ObjectAnimator.ofFloat( child, "scaleY", child.getScaleY(), 1 ) );
			} else {
				animators.add( ObjectAnimator.ofFloat( child, "translationY", child.getTranslationY(), childHeight - childOffsets.get( i ) + collapsedChildsShownTip * forwardIndex ) );
				child.setPivotX( centerX );
				child.setPivotY( 0 );
				float targetScale = 1 - (MAX_ZPOSITION_FACTOR * reverseIndex);
				animators.add( ObjectAnimator.ofFloat( child, "scaleX", child.getScaleX(), targetScale ) );
				animators.add( ObjectAnimator.ofFloat( child, "scaleY", child.getScaleY(), targetScale ) );
				reverseIndex--;
				forwardIndex++;
			}
		}
		toPickModeSet.playTogether( animators );
		toPickModeSet.start();
	}

	private void recoverChildsPosition() {
		revertSet.end();
		animators.clear();
		if ( mode == Mode.LISTED ) {
			mode = Mode.ANIMATING;
			currentFeedbackFactor = 1f;
			final int childCount = getChildCount();
			for ( int i = 0 ; i < childCount ; i++ ) {
				View child = getChildAt( i );
				animators.add( ObjectAnimator.ofFloat( child, "translationY", child.getTranslationY(), 0 ) );
			}
			revertSet.playTogether( animators );
			revertSet.start();
		}
	}

	@Override
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged( w, h, oldw, oldh );
		this.width = w;
		this.height = h;
		this.childHeight = Math.round( h * CHILD_HEIGHT_FACTOR );
		this.bottomSpace = height - childHeight;
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		final int childCount = getChildCount();
		centerX = ( float ) (right - left) / 2f;
		if ( childCount > 0 ) {
			int paddingTop = getPaddingTop();
			itemHeight = Math.round( (height - paddingTop) / (childCount + 1) );
			for ( int i = 0 ; i < childCount ; i++ ) {
				View child = getChildAt( i );
				child.setDrawingCacheEnabled( true );
				child.setDrawingCacheQuality( View.DRAWING_CACHE_QUALITY_HIGH );
				int itemTop = i * itemHeight + paddingTop;
				child.layout( 0, itemTop, width, itemTop + childHeight );
				childOffsets.put( i, itemTop );
			}
			this.collapsedChildsShownTip = bottomSpace / ( float ) childCount * COLLAPSED_TIP_FACTOR;
		}
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		super.onMeasure( widthMeasureSpec, heightMeasureSpec );
		final int childCount = getChildCount();
		if ( childCount > 0 ) {
			int childHeightSpec = MeasureSpec.makeMeasureSpec( childHeight, MeasureSpec.EXACTLY );
			for ( int i = 0 ; i < childCount ; i++ ) {
				View child = getChildAt( i );
				child.measure( widthMeasureSpec, childHeightSpec );
			}
		}
	}

	/*
	 * bypass event when horizontal swipe action arrived here
	 */
	@Override
	public boolean onInterceptTouchEvent( MotionEvent ev ) {
		if ( mode != Mode.LISTED ) {
			return childHeight < ev.getY();
		}
		switch ( ev.getAction() & MotionEvent.ACTION_MASK ) {
		case MotionEvent.ACTION_DOWN :
			xDistance = yDistance = 0f;
			lastX = ev.getX();
			lastY = ev.getY();
			break;
		case MotionEvent.ACTION_MOVE :
			final float curX = ev.getX();
			final float curY = ev.getY();
			xDistance += Math.abs( curX - lastX );
			yDistance += Math.abs( curY - lastY );
			lastX = curX;
			lastY = curY;
			if ( xDistance > yDistance )
				return false;
		}
		return true;
	}

	@Override
	public boolean onTouchEvent( MotionEvent ev ) {
		int action = ev.getAction() & MotionEvent.ACTION_MASK;
		float y = ev.getY();
		float x = ev.getX();
		switch ( action ) {
		case MotionEvent.ACTION_DOWN :
			startY = y;
			beforeY = y;
			startX = x;
			startTime = ev.getEventTime();
			break;

		case MotionEvent.ACTION_MOVE :
			float moveY = y - beforeY;
			beforeY = y;
			if ( mode == Mode.LISTED ) {
				applyChildScroll( Math.round( moveY ) );
			}
			break;

		case MotionEvent.ACTION_CANCEL :
			break;

		case MotionEvent.ACTION_OUTSIDE :
			break;

		case MotionEvent.ACTION_UP :
			if ( ev.getEventTime() - startTime < SINGLE_TAP_INTERVAL && Math.abs( startX - x ) < SINGLE_TOUCH_SLOPE && Math.abs( startY - y ) < SINGLE_TOUCH_SLOPE ) {
				final int childCount = getChildCount();
				if ( mode == Mode.LISTED ) {
					for ( int i = childCount - 1 ; i > -1 ; i-- ) {
						if ( childOffsets.get( i ) < y ) {
							focusedIndex = i;
							changeMode( Mode.PICKED );
							break;
						}
					}
				} else if ( mode == Mode.PICKED ) {
					if ( childHeight < y ) {
						applyListMode();
					}
				}
			} else {
				recoverChildsPosition();
			}
			break;

		default :
		}
		return true;
	}

	private void applyChildScroll( int moveY ) {
		final int childCount = getChildCount();
		if ( childCount > 0 ) {
			if ( moveY > 0 ) {
				currentFeedbackFactor += TOUCH_FEEDBACK_FACTOR;
				currentFeedbackFactor = Math.min( currentFeedbackFactor, 50 * getResources().getDisplayMetrics().density );
			} else {
				currentFeedbackFactor -= TOUCH_FEEDBACK_FACTOR;
				currentFeedbackFactor = Math.max( currentFeedbackFactor, -50 * getResources().getDisplayMetrics().density );
			}
			for ( int i = 0 ; i < childCount ; i++ ) {
				float translationY = ( float ) Math.sqrt( i ) * currentFeedbackFactor;
				getChildAt( i ).setTranslationY( translationY );
			}
		}
	}

	public void setOnCardSelectListener( DataListener<Boolean> dataListener ) {
		this.onCardSelectListener = dataListener;
	}
}