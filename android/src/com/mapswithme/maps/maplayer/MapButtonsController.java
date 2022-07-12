package com.mapswithme.maps.maplayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapswithme.maps.R;
import com.mapswithme.maps.downloader.MapManager;
import com.mapswithme.maps.downloader.UpdateInfo;
import com.mapswithme.maps.routing.RoutingController;
import com.mapswithme.maps.widget.menu.MyPositionButton;
import com.mapswithme.maps.widget.placepage.PlacePageController;
import com.mapswithme.util.Config;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.log.Logger;

import java.util.HashMap;
import java.util.Map;

public class MapButtonsController extends Fragment
{
  Map<MapButtons, View> mButtonsMap;
  private View mFrame;
  private View mInnerButtonsFrame;
  private View mInnerLeftButtonsFrame;
  private View mInnerRightButtonsFrame;
  @Nullable
  private View mBottomButtonsFrame;
  @Nullable
  private MapLayersController mToggleMapLayerController;

  @Nullable
  private MyPositionButton mNavMyPosition;
  private SearchWheel mSearchWheel;
  private BadgeDrawable mBadgeDrawable;
  private float mContentHeight;
  private float mContentWidth;

  private MapButtonClickListener mMapButtonClickListener;
  private View.OnClickListener mOnSearchCanceledListener;
  private PlacePageController mPlacePageController;
  private OnBottomButtonsHeightChangedListener mOnBottomButtonsHeightChangedListener;

  private LayoutMode mLayoutMode;
  private int mMyPositionMode;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    Logger.v("", "onCreateView: " + mLayoutMode);
    if (mLayoutMode == LayoutMode.navigation)
      mFrame = inflater.inflate(R.layout.map_buttons_layout_navigation, container, false);
    else if (mLayoutMode == LayoutMode.planning)
      mFrame = inflater.inflate(R.layout.map_buttons_layout_planning, container, false);
    else
      mFrame = inflater.inflate(R.layout.map_buttons_layout_regular, container, false);

    mInnerButtonsFrame = mFrame.findViewById(R.id.map_buttons_inner);
    mInnerLeftButtonsFrame = mFrame.findViewById(R.id.map_buttons_inner_left);
    mInnerRightButtonsFrame = mFrame.findViewById(R.id.map_buttons_inner_right);
    mBottomButtonsFrame = mFrame.findViewById(R.id.map_buttons_bottom);
    View zoomFrame = mFrame.findViewById(R.id.zoom_buttons_container);
    mFrame.findViewById(R.id.nav_zoom_in)
          .setOnClickListener((v) -> mMapButtonClickListener.onClick(MapButtons.zoomIn));
    mFrame.findViewById(R.id.nav_zoom_out)
          .setOnClickListener((v) -> mMapButtonClickListener.onClick(MapButtons.zoomOut));
    View bookmarksButton = mFrame.findViewById(R.id.btn_bookmarks);
    bookmarksButton.setOnClickListener((v) -> mMapButtonClickListener.onClick(MapButtons.bookmarks));
    View myPosition = mFrame.findViewById(R.id.my_position);
    mNavMyPosition = new MyPositionButton(myPosition, mMyPositionMode, (v) -> mMapButtonClickListener.onClick(MapButtons.myPosition));

    // Some buttons do not exist in navigation mode
    FloatingActionButton layersButton = mFrame.findViewById(R.id.layers_button);
    if (layersButton != null)
    {
      mToggleMapLayerController = new MapLayersController(layersButton,
                                                          () -> mMapButtonClickListener.onClick(MapButtons.toggleMapLayer), requireActivity());
    }
    View menuButton = mFrame.findViewById(R.id.menu_button);
    if (menuButton != null)
      menuButton.setOnClickListener((v) -> mMapButtonClickListener.onClick(MapButtons.menu));
    View helpButton = mFrame.findViewById(R.id.help_button);
    if (helpButton != null)
      helpButton.setOnClickListener((v) -> mMapButtonClickListener.onClick(MapButtons.help));

    mSearchWheel = new SearchWheel(mFrame, (v) -> mMapButtonClickListener.onClick(MapButtons.search), mOnSearchCanceledListener);
    View searchButton = mFrame.findViewById(R.id.btn_search);

    // Used to get the maximum height the buttons will evolve in
    mFrame.addOnLayoutChangeListener(new MapButtonsController.ContentViewLayoutChangeListener(mFrame));

    mButtonsMap = new HashMap<>();
    mButtonsMap.put(MapButtons.zoom, zoomFrame);
    mButtonsMap.put(MapButtons.myPosition, myPosition);
    mButtonsMap.put(MapButtons.bookmarks, bookmarksButton);
    mButtonsMap.put(MapButtons.search, searchButton);

    if (layersButton != null)
      mButtonsMap.put(MapButtons.toggleMapLayer, layersButton);
    if (menuButton != null)
      mButtonsMap.put(MapButtons.menu, menuButton);
    if (helpButton != null)
      mButtonsMap.put(MapButtons.help, helpButton);
    return mFrame;
  }

  @Override
  public void onStart()
  {
    super.onStart();
    showMapButtons(true);
  }

  public LayoutMode getLayoutMode()
  {
    return mLayoutMode;
  }

  public void init(LayoutMode layoutMode, int myPositionMode, MapButtonClickListener mapButtonClickListener, @NonNull View.OnClickListener onSearchCanceledListener, PlacePageController placePageController, OnBottomButtonsHeightChangedListener onBottomButtonsHeightChangedListener)
  {
    mLayoutMode = layoutMode;
    mMyPositionMode = myPositionMode;
    mMapButtonClickListener = mapButtonClickListener;
    mOnSearchCanceledListener = onSearchCanceledListener;
    mPlacePageController = placePageController;
    mOnBottomButtonsHeightChangedListener = onBottomButtonsHeightChangedListener;
  }

  public void showButton(boolean show, MapButtonsController.MapButtons button)
  {
    View buttonView = mButtonsMap.get(button);
    if (buttonView == null)
      return;
    switch (button)
    {
      case zoom:
        UiUtils.showIf(show && Config.showZoomButtons(), buttonView);
        break;
      case toggleMapLayer:
        if (mToggleMapLayerController != null)
          mToggleMapLayerController.showButton(show && !isInNavigationMode());
        break;
      case myPosition:
        if (mNavMyPosition != null)
          mNavMyPosition.showButton(show);
        break;
      case search:
        mSearchWheel.show(show);
      case bookmarks:
      case menu:
        UiUtils.showIf(show, buttonView);
    }
  }

  @OptIn(markerClass = com.google.android.material.badge.ExperimentalBadgeUtils.class)
  public void updateMarker(@NonNull Activity activity)
  {
    View menuButton = mButtonsMap.get(MapButtons.menu);
    if (menuButton == null)
      return;
    final UpdateInfo info = MapManager.nativeGetUpdateInfo(null);
    final int count = (info == null ? 0 : info.filesCount);
    BadgeUtils.detachBadgeDrawable(mBadgeDrawable, menuButton);
    mBadgeDrawable = BadgeDrawable.create(activity);
    mBadgeDrawable.setHorizontalOffset(30);
    mBadgeDrawable.setVerticalOffset(20);
    mBadgeDrawable.setNumber(count);
    mBadgeDrawable.setVisible(count > 0);
    BadgeUtils.attachBadgeDrawable(mBadgeDrawable, menuButton);
  }

  private boolean isScreenWideEnough()
  {
    final boolean isLeftEnough = mInnerLeftButtonsFrame == null || mContentWidth / 2 > (mPlacePageController.getPlacePageWidth() / 2)
                                                                                       + mInnerLeftButtonsFrame.getWidth();
    final boolean isRightEnough = mInnerRightButtonsFrame == null || mContentWidth / 2 > (mPlacePageController.getPlacePageWidth() / 2)
                                                                                         + mInnerRightButtonsFrame.getWidth();
    return isLeftEnough && isRightEnough;
  }

  public void move(float translationY)
  {
    if (mContentHeight == 0 || isScreenWideEnough())
      return;

    // Move the buttons container to follow the place page
    final float translation = translationY - mInnerButtonsFrame.getBottom();
    final float appliedTranslation = translation <= 0 ? translation : 0;
    mInnerButtonsFrame.setTranslationY(appliedTranslation);

    updateButtonsVisibility(appliedTranslation);
  }

  public void updateButtonsVisibility()
  {
    updateButtonsVisibility(mInnerButtonsFrame.getTranslationY());
  }

  private void updateButtonsVisibility(final float translation)
  {
    for (Map.Entry<MapButtons, View> entry : mButtonsMap.entrySet())
    {
      // Only move items inside the inner frame
      // Top and bottom items should be static
      if (entry.getValue().getParent().getParent() == mInnerButtonsFrame)
        showButton(getViewTopOffset(translation, entry.getValue()) > 0, entry.getKey());
    }
  }

  public float getBottomButtonsHeight()
  {
    if (mBottomButtonsFrame != null && mFrame != null && UiUtils.isVisible(mFrame))
    {
      return mBottomButtonsFrame.getMeasuredHeight();
    }
    else
      return 0;
  }

  public void showMapButtons(boolean show)
  {
    if (show)
    {
      UiUtils.show(mFrame);
      showButton(true, MapButtons.zoom);
    }
    else
      UiUtils.hide(mFrame);
    mOnBottomButtonsHeightChangedListener.OnBottomButtonsHeightChanged();
  }

  private boolean isInNavigationMode()
  {
    return RoutingController.get().isPlanning() || RoutingController.get().isNavigating();
  }

  public void toggleMapLayer(@NonNull Mode mode)
  {
    if (mToggleMapLayerController != null)
      mToggleMapLayerController.toggleMode(mode);
  }

  public void updateNavMyPositionButton(int newMode)
  {
    if (mNavMyPosition != null)
      mNavMyPosition.update(newMode);
  }

  private int getViewTopOffset(float translation, View v)
  {
    return (int) (translation + v.getTop());
  }

  public void onResume(@NonNull Activity activity)
  {

    mSearchWheel.onResume();
    updateMarker(activity);
  }

  public void resetSearch()
  {
    mSearchWheel.reset();
  }

  public void saveNavSearchState(@NonNull Bundle outState)
  {
    mSearchWheel.saveState(outState);
  }

  public void restoreNavSearchState(@NonNull Bundle savedInstanceState)
  {
    mSearchWheel.restoreState(savedInstanceState);
  }

  public enum LayoutMode
  {
    regular,
    planning,
    navigation
  }

  public enum MapButtons
  {
    myPosition,
    toggleMapLayer,
    zoomIn,
    zoomOut,
    zoom,
    search,
    bookmarks,
    menu,
    help
  }

  public interface MapButtonClickListener
  {
    void onClick(MapButtons button);
  }

  public interface OnBottomButtonsHeightChangedListener
  {
    void OnBottomButtonsHeightChanged();
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
      mOnBottomButtonsHeightChangedListener.OnBottomButtonsHeightChanged();
      mContentView.removeOnLayoutChangeListener(this);
    }
  }
}
