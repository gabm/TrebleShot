package com.genonbeta.TrebleShot.adapter;

import android.content.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.TrebleShot.*;
import com.genonbeta.TrebleShot.helper.*;
import java.io.*;
import java.util.*;

public class ReceivedFilesListAdapter extends AbstractFlexibleAdapter
{
	public Context mContext;
	private String mSearchWord;
	public ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	private Comparator<FileInfo> mComparator = new Comparator<FileInfo>()
	{
		@Override
		public int compare(ReceivedFilesListAdapter.FileInfo compareFrom, ReceivedFilesListAdapter.FileInfo compareTo)
		{
			return compareFrom.fileName.toLowerCase().compareTo(compareTo.fileName.toLowerCase());
		}
	};
	
	public ReceivedFilesListAdapter(Context context)
	{
		this.mContext = context;
	}
	
	@Override
	protected void onSearch(String word)
	{
		this.mSearchWord = word;
	}

	@Override
	protected void onUpdate()
	{
		this.mList.clear();

		for (File file : ApplicationHelper.getApplicationDirectory(mContext).listFiles())
		{
			if ((this.mSearchWord == null || (this.mSearchWord != null && ApplicationHelper.searchWord(file.getName(), this.mSearchWord))) && file.isFile())
				this.mList.add(new FileInfo(file.getName(), FileUtils.sizeExpression(file.length(), false), file));
		}

		Collections.sort(mList, this.mComparator);
	}
	
	@Override
	public int getCount()
	{
		return this.mList.size();
	}

	@Override
	public Object getItem(int itemId)
	{
		return this.mList.get(itemId);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup container)
	{
		return getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list_received_files, container, false), position);
	}
	
	public View getViewAt(View view, int position)
	{
		TextView fileNameText = (TextView) view.findViewById(R.id.text);
		TextView sizeText = (TextView) view.findViewById(R.id.text2);
		FileInfo fileInfo = (FileInfo) getItem(position);
		
		fileNameText.setText(fileInfo.fileName);
		sizeText.setText(fileInfo.fileSize);
		
		return view;
	}
	
	public static class FileInfo
	{
		public String fileName;
		public String fileSize;
		public File file;
		
		public FileInfo(String name, String size, File file)
		{
			this.fileName = name;
			this.fileSize = size;
			this.file = file;
		}
	}
}