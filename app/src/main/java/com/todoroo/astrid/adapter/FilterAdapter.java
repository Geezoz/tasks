/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.adapter;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.andlib.utility.AndroidUtilities.assertMainThread;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.FilterListItem;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.tasks.billing.Inventory;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.locale.Locale;
import org.tasks.themes.ColorProvider;
import org.tasks.themes.Theme;
import org.tasks.themes.ThemeAccent;

public class FilterAdapter extends BaseAdapter {

  private static final String TOKEN_FILTERS = "token_filters";
  private static final String TOKEN_SELECTED = "token_selected";
  private static final int VIEW_TYPE_COUNT = FilterListItem.Type.values().length;
  private final Activity activity;
  private final ThemeAccent accent;
  private final Locale locale;
  private final Inventory inventory;
  private final ColorProvider colorProvider;
  private final LayoutInflater inflater;
  private Filter selected = null;
  private List<FilterListItem> items = new ArrayList<>();

  @Inject
  public FilterAdapter(
      Activity activity,
      Theme theme,
      Locale locale,
      Inventory inventory,
      ColorProvider colorProvider) {
    this.activity = activity;
    this.accent = theme.getThemeAccent();
    this.locale = locale;
    this.inventory = inventory;
    this.colorProvider = colorProvider;
    this.inflater = theme.getLayoutInflater(activity);
  }

  public void save(Bundle outState) {
    outState.putParcelableArrayList(TOKEN_FILTERS, getItems());
    outState.putParcelable(TOKEN_SELECTED, selected);
  }

  public void restore(Bundle savedInstanceState) {
    items = savedInstanceState.getParcelableArrayList(TOKEN_FILTERS);
    selected = savedInstanceState.getParcelable(TOKEN_SELECTED);
  }

  public void setData(List<FilterListItem> items, @Nullable Filter selected) {
    setData(items, selected, -1);
  }

  public void setData(List<FilterListItem> items, @Nullable Filter selected, int defaultIndex) {
    assertMainThread();
    this.items = items;
    this.selected = defaultIndex >= 0 ? getFilter(indexOf(selected, defaultIndex)) : selected;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    assertMainThread();
    return items.size();
  }

  @Override
  public FilterListItem getItem(int position) {
    assertMainThread();
    return items.get(position);
  }

  private Filter getFilter(int position) {
    FilterListItem item = getItem(position);
    return item instanceof Filter ? (Filter) item : null;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  /** Create or reuse a view */
  private View newView(View convertView, ViewGroup parent, FilterListItem.Type viewType) {
    if (convertView == null) {
      convertView = inflater.inflate(viewType.layout, parent, false);
      FilterViewHolder viewHolder;
      switch (viewType) {
        case ITEM:
          viewHolder =
              new FilterViewHolder(
                  convertView, accent, false, locale, activity, inventory, colorProvider, null);
          break;
        case SEPARATOR:
          viewHolder = new FilterViewHolder(convertView);
          break;
        case SUBHEADER:
          viewHolder = new FilterViewHolder(convertView, activity);
          break;
        default:
          throw new RuntimeException();
      }
      convertView.setTag(viewHolder);
    }
    return convertView;
  }

  public ArrayList<FilterListItem> getItems() {
    assertMainThread();
    return newArrayList(items);
  }

  public int indexOf(FilterListItem item, int defaultValue) {
    assertMainThread();
    int index = items.indexOf(item);
    return index == -1 ? defaultValue : index;
  }

  @NonNull
  @Override
  public View getView(int position, View convertView, @NonNull ViewGroup parent) {
    FilterListItem item = getItem(position);
    convertView = newView(convertView, parent, item.getItemType());
    FilterViewHolder viewHolder = (FilterViewHolder) convertView.getTag();
    switch (item.getItemType()) {
      case ITEM:
        viewHolder.bind(item, item.equals(selected), 0);
        break;
      case SUBHEADER:
        viewHolder.bind((NavigationDrawerSubheader) item);
        break;
      case SEPARATOR:
        break;
    }

    return convertView;
  }

  @Override
  public int getViewTypeCount() {
    return VIEW_TYPE_COUNT;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItem(position).getItemType() == FilterListItem.Type.ITEM;
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).getItemType().ordinal();
  }
}
