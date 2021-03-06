package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.TextStreamListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListFragment
		extends EditableListFragment<TextStreamObject, TextStreamListAdapter>
		implements TitleSupport
{
	private ArrayList<TextStreamObject> mSelectionList = new ArrayList<>();
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
		super.onCreate(savedInstanceState);
	}

	@Override
	public TextStreamListAdapter onAdapter()
	{
		return new TextStreamListAdapter(getActivity());
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mStatusReceiver, mIntentFilter);
		refreshList();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mStatusReceiver);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		TextStreamObject textStreamObject = (TextStreamObject) getAdapter().getItem(position);

		startActivity(new Intent(getContext(), TextEditorActivity.class)
				.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
				.putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, textStreamObject.id)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_text_stream, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.menu_action_type_new)
			startActivity(new Intent(getActivity(), TextEditorActivity.class)
					.setAction(TextEditorActivity.ACTION_EDIT_TEXT));

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_text_stream, menu);
		return true;
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		super.onPrepareActionMenu(context, actionMode);
		getSelectionList().clear();
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_abs_editable_multi_select) {
			getSelectionList().clear();
			setSelection(getListView().getCheckedItemCount() != getAdapter().getCount());

			return false;
		} else if (id == R.id.action_mode_text_stream_delete) {
			for (TextStreamObject textStreamObject : mSelectionList)
				getAdapter().getDatabase().remove(textStreamObject);

			return true;
		} else if (id == R.id.action_mode_text_stream_share || id == R.id.action_mode_text_stream_share_ts) {
			if (getSelectionList().size() == 1) {
				TextStreamObject streamObject = getSelectionList().get(0);

				Intent shareIntent = new Intent(item.getItemId() == R.id.action_mode_text_stream_share
						? Intent.ACTION_SEND : ShareActivity.ACTION_SEND)
						.putExtra(Intent.EXTRA_TEXT, streamObject.text)
						.setType("text/*");

				startActivity((item.getItemId() == R.id.action_mode_share_all_apps) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);
				return true;
			} else
				Toast.makeText(context, R.string.mesg_textShareLimit, Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected)
	{
		super.onItemChecked(context, actionMode, position, isSelected);

		TextStreamObject textStreamObject = (TextStreamObject) getAdapter().getItem(position);

		if (isSelected)
			getSelectionList().add(textStreamObject);
		else
			getSelectionList().remove(textStreamObject);

		actionMode.setTitle(String.valueOf(getSelectionList().size()));
	}

	public ArrayList<TextStreamObject> getSelectionList()
	{
		return mSelectionList;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_textStream);
	}

	private class StatusReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_CLIPBOARD)) {
				refreshList();
			}
		}
	}
}