package com.mapswithme.maps;

import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mapswithme.maps.widget.placepage.PlacePageController;

public class NavigationButtonsAnimationController
{
  @NonNull
  private final View mFrame;

  @Nullable
  private final OnTranslationChangedListener mTranslationListener;
  private final float mBottomMargin;
  private final PlacePageController mPlacePageController;
  private final int mButtonWidth;
  private float mContentHeight;
  private float mContentWidth;

  public NavigationButtonsAnimationController(@NonNull View frame,
                                              PlacePageController placePageController,
                                              int buttonWidth,
                                              @Nullable OnTranslationChangedListener translationListener)
  {
    mFrame = frame;
    // Used to get the maximum height the buttons will evolve in
    View contentView = (View) mFrame.getParent();
    contentView.addOnLayoutChangeListener(new ContentViewLayoutChangeListener(contentView));
    mTranslationListener = translationListener;
    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) frame.getLayoutParams();
    mBottomMargin = lp.bottomMargin;
    mButtonWidth = buttonWidth;
    mPlacePageController = placePageController;
  }

  private boolean isScreenWideEnough()
  {
    return mContentWidth > (mPlacePageController.getPlacePageWidth() + 2 * mButtonWidth);
  }

  public void move(float translationY)
  {
    if (mContentHeight == 0 || isScreenWideEnough())
      return;

    final float translation = mBottomMargin + translationY - mContentHeight;
    final float appliedTranslation = translation <= 0 ? translation : 0;
    mFrame.setTranslationY(appliedTranslation);
    update(appliedTranslation);
  }

  public void update()
  {
    update(mFrame.getTranslationY());
  }

  private void update(final float translation)
  {
    if (mTranslationListener != null)
      mTranslationListener.onTranslationChanged(translation);
  }

  public interface OnTranslationChangedListener
  {
    void onTranslationChanged(float translation);
  }

  private class ContentViewLayoutChangeListener implements View.OnLayoutChangeListener
  {
    @NonNull
    private final View mContentView;

    public ContentViewLayoutChangeListener(@NonNull View contentView)
    {
      mContentView = contentView;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                               int oldTop, int oldRight, int oldBottom)
    {
      mContentHeight = bottom - top;
      mContentWidth = right - left;
      mContentView.removeOnLayoutChangeListener(this);
    }
  }
}
