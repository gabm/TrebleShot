package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public abstract class ShareableListFragment<T extends Shareable, E extends ShareableListAdapter<T>>
		extends EditableListFragment<T, E>
{
	private ArrayList<T> mSelectionList = new ArrayList<>();
	private ArrayList<T> mCachedList = new ArrayList<>();
	private boolean mSearchSupport = true;
	private boolean mSearchActive = false;

	private SearchView.OnQueryTextListener mSearchComposer = new SearchView.OnQueryTextListener()
	{
		@Override
		public boolean onQueryTextSubmit(String word)
		{
			return search(word);
		}

		@Override
		public boolean onQueryTextChange(String word)
		{
			return search(word);
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		if (getSearchSupport()) {
			inflater.inflate(R.menu.actions_search, menu);

			((SearchView) menu.findItem(R.id.search).getActionView())
					.setOnQueryTextListener(mSearchComposer);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		Shareable shareable = (Shareable) getAdapter().getItem(position);
		openFile(shareable.uri, getString(R.string.text_fileOpenAppChoose));
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
		getSelectionList().clear();
		return true;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_share, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_share_trebleshot || id == R.id.action_mode_share_all_apps) {
			Intent shareIntent = null;
			String action = (item.getItemId() == R.id.action_mode_share_all_apps) ? (getSelectionList().size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND) : (getSelectionList().size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND);

			if (getSelectionList().size() > 1) {
				ArrayList<Uri> uriList = new ArrayList<>();
				ArrayList<CharSequence> nameList = new ArrayList<>();

				for (T sharedItem : getSelectionList()) {
					uriList.add(sharedItem.uri);
					nameList.add(sharedItem.fileName);
				}

				shareIntent = new Intent(action)
						.setType("*/*")
						.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
						.putCharSequenceArrayListExtra(ShareActivity.EXTRA_FILENAME_LIST, nameList);
			} else if (getSelectionList().size() == 1) {
				T sharedItem = getSelectionList().get(0);

				shareIntent = new Intent(action)
						.putExtra(Intent.EXTRA_STREAM, sharedItem.uri)
						.putExtra(ShareActivity.EXTRA_FILENAME_LIST, sharedItem.fileName)
						.setType("*/*");
			}

			if (shareIntent != null)
				startActivity((item.getItemId() == R.id.action_mode_share_all_apps) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);
		} else if (id == R.id.action_mode_abs_editable_multi_select) {
			getSelectionList().clear();
			setSelection(getListView().getCheckedItemCount() != getAdapter().getCount());
			return false;
		} else
			return false;

		return true;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected)
	{
		super.onItemChecked(context, actionMode, position, isSelected);

		T shareable = (T) getAdapter().getItem(position);

		if (isSelected)
			getSelectionList().add(shareable);
		else
			getSelectionList().remove(shareable);

		actionMode.setTitle(String.valueOf(getSelectionList().size()));
	}

	public ArrayList<T> getCachedList()
	{
		return mCachedList;
	}

	public boolean getSearchSupport()
	{
		return mSearchSupport;
	}

	public ArrayList<T> getSelectionList()
	{
		return mSelectionList;
	}

	@Override
	public boolean isRefreshLocked()
	{
		return super.isRefreshLocked() || mSearchActive;
	}

	public void openFile(Uri uri, String chooserText)
	{
		try {
			Intent openIntent = new Intent(Intent.ACTION_VIEW);
			StreamInfo streamInfo = StreamInfo.getStreamInfo(getActivity(), uri, false);

			openIntent.setDataAndType(StreamInfo.Type.FILE.equals(streamInfo.type)
					? FileUtils.getUriForFile(getActivity(), new File(URI.create(streamInfo.uri.toString())), openIntent)
					: streamInfo.uri, streamInfo.mimeType);

			startActivity(Intent.createChooser(openIntent, chooserText));
		} catch (RuntimeException e) {
			e.printStackTrace();
			Toast.makeText(getActivity(), R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getActivity(), R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
		}
	}

	public boolean search(String word)
	{
		if (getPowerfulActionMode() != null)
			getPowerfulActionMode().finish(this);

		mSearchActive = word != null && word.length() > 0;

		if (mSearchActive) {
			if (mCachedList.size() == 0)
				mCachedList.addAll(getAdapter().getList());

			ArrayList<T> searchableList = new ArrayList<>();

			for (T shareable : mCachedList)
				if (shareable.searchMatches(word))
					searchableList.add(shareable);

			getAdapter().onUpdate(searchableList);
			getAdapter().notifyDataSetChanged();
		} else if (!loadIfRequested() && mCachedList.size() != 0) {
			getAdapter().onUpdate(mCachedList);
			getAdapter().notifyDataSetChanged();

			mCachedList.clear();
		}

		return getAdapter().getCount() > 0;
	}

	public void setSearchSupport(boolean searchSupport)
	{
		mSearchSupport = searchSupport;
	}
}
