package com.mapswithme.maps;

import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.mapswithme.maps.widget.placepage.PlacePageController;
import com.mapswithme.util.log.Logger;

public class NavigationButtonsAnimationController
{
  @NonNull
  private final View mFrame;
  @NonNull
  private final View mZoomFrame;
  @NonNull
  private final View mLayersButton;

  @Nullable
  private final OnTranslationChangedListener mTranslationListener;
  private final float mBottomMargin;
  private final PlacePageController mPlacePageController;
  private final int mButtonWidth;
  private float mContentHeight;
  private float mContentWidth;
  private float mInitialButtonMargin;

  public NavigationButtonsAnimationController(@NonNull View frame,
                                              @NonNull View zoomFrame,
                                              @NonNull View layersButton,
                                              PlacePageController placePageController,
                                              @Nullable OnTranslationChangedListener translationListener)
  {
    mFrame = frame;
    mZoomFrame = zoomFrame;
    mLayersButton = layersButton;
    // Used to get the maximum height the buttons will evolve in
    View contentView = (View) mFrame.getParent();
    contentView.addOnLayoutChangeListener(new ContentViewLayoutChangeListener(contentView));
    mTranslationListener = translationListener;
    mBottomMargin = ((RelativeLayout.LayoutParams) frame.getLayoutParams()).bottomMargin;
    mInitialButtonMargin = ((ConstraintLayout.LayoutParams) zoomFrame.getLayoutParams()).bottomMargin;
    mButtonWidth = mLayersButton.getLayoutParams().width;
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

    // Move the buttons container to follow the place page
    final float translation = mBottomMargin + translationY - mContentHeight;
    final float appliedTranslation = translation <= 0 ? translation : 0;
    mFrame.setTranslationY(appliedTranslation);

    // Reduce buttons margin to move them only if necessary
    // Zoom frame is above the layers so if must move twice as much
    final float appliedMarginTranslationLayers = Math.min(-appliedTranslation, mInitialButtonMargin);
    final float appliedMarginTranslationZoomFrame = Math.min(-appliedTranslation, 2 * mInitialButtonMargin);
    mLayersButton.setTranslationY(appliedMarginTranslationLayers);
    mZoomFrame.setTranslationY(appliedMarginTranslationZoomFrame);

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
