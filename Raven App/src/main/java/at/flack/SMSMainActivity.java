/*
 Copyright 2015 Philipp Adam, Manuel Caspari, Nicolas Lukaschek
 contact@ravenapp.org

 This file is part of Raven.

 Raven is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Raven is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Raven. If not, see <http://www.gnu.org/licenses/>.

*/

package at.flack;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import safe.KeyEntity;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.flack.activity.MessageOverview;
import at.flack.activity.NewSMSContactActivity;
import at.flack.contacts.ContactNameMap;
import at.flack.contacts.SMSItem;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.ProfilePictureCache;

import com.melnykov.fab.FloatingActionButton;

public class SMSMainActivity extends Fragment {

	private ListView contactList;
	public static ContactNameMap contactNameMap;
	private View progressbar;
	private SwipeRefreshLayout swipe;
	private static SMSMainActivity me;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sms_main, container, false);

		contactNameMap = new ContactNameMap(getActivity());

		contactList = (ListView) rootView.findViewById(R.id.listviewsms);

		TextView padding = new TextView(getActivity());
		padding.setHeight(10);
		contactList.addHeaderView(padding);
		contactList.setHeaderDividersEnabled(false);
		contactList.addFooterView(padding, null, false);
		contactList.setFooterDividersEnabled(false);

		swipe = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
		swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (getActivity() instanceof MainActivity)
					((MainActivity) getActivity()).updateSMSContacts();
			}
		});
		contactList.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				int topRowVerticalPosition = (contactList == null || contactList.getChildCount() == 0) ? 0
						: contactList.getChildAt(0).getTop();
				swipe.setEnabled(topRowVerticalPosition >= 0);
			}
		});

		progressbar = rootView.findViewById(R.id.load_screen);
		if (MainActivity.getContacts() == null)
			progressbar.setVisibility(View.VISIBLE);

		updateContacts((MainActivity) getActivity());
		contactList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (MainActivity.getListItems() == null) {
					updateContacts((MainActivity) getActivity());
				}
				openMessageActivity(getActivity(), arg2 - 1);
			}

		});
		FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
		fab.attachToListView(contactList);
		fab.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent qr = new Intent(getActivity(), NewSMSContactActivity.class);
				startActivityForResult(qr, 1);
			}
		});

		setRetainInstance(true);

		return rootView;
	}

	public static SMSMainActivity getInstance() {
		return getInstance(false);
	}

	public static SMSMainActivity getInstance(boolean force) {
		if (me == null || force)
			me = new SMSMainActivity();
		return me;
	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (MainActivity.getContacts() == null)
			return;
		if (arg2 < 0)
			return;
		Intent smsIntent = new Intent(activity, MessageOverview.class);
		smsIntent.putExtra("CONTACT_NUMBER", MainActivity.getContacts().get(arg2).mAddress);
		smsIntent.putExtra("CONTACT_NAME", MainActivity.getListItems().get(arg2).getTitle());
		smsIntent.putExtra("CONTACT_ID", MainActivity.getContacts().get(arg2).mThreadID);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Bitmap temp = MainActivity.getContacts().get(arg2).fetchThumbnail(activity);
		if (temp != null) {
			temp.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] bytes = stream.toByteArray();

			smsIntent.putExtra("profilePicture", bytes);
		}
		try {
			KeyEntity key = KeySafe.getInstance(activity).get(
					PhoneNumberUtils.formatNumber(MainActivity.getContacts().get(arg2).mAddress));
			if (key != null && key.getVersion() != KeyEntity.ECDH_PRIVATE_KEY)
				smsIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}

		activity.startActivityForResult(smsIntent, 2);
	}

	public void updateContacts(MainActivity activity) {

		if (MainActivity.getContacts() == null)
			MainActivity.setContacts(readSMSContacts(getActivity()));
		if (MainActivity.getListItems() == null || MainActivity.getListItems().size() == 0)
			MainActivity.setListItems(getNames(MainActivity.getContacts(), activity));

		if (contactList != null && contactList.getAdapter() == null) {
			ContactAdapter arrayAdapter = new ContactAdapter(activity, MainActivity.getListItems(), 0);

			arrayAdapter.notifyDataSetChanged();
			contactList.setAdapter(arrayAdapter);
			contactList.setSelection(0);

		} else if (MainActivity.getListItems().size() > 0 && contactList != null) {
			((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
					MainActivity.getListItems(), 0);
		}

		if (swipe != null)
			swipe.setRefreshing(false);
		if (progressbar != null)
			progressbar.setVisibility(View.INVISIBLE);

	}

	public ArrayList<ContactModel> getNames(ArrayList<SMSItem> contacts2, Activity activity) {
		if (contactNameMap == null)
			contactNameMap = new ContactNameMap(activity);
		ArrayList<ContactModel> erg = new ArrayList<ContactModel>();
		Bitmap picture = null;
		LinkedHashSet<ContactModel> set = new LinkedHashSet<ContactModel>();
		MainActivity ac = (MainActivity) activity;
		for (int i = 0; i < contacts2.size(); i++) {

			String name = contactNameMap.getName(contacts2.get(i).mAddress);
			picture = contacts2.get(i).fetchThumbnail(ac);
			if (picture == null && ac != null && name != null)
				picture = ProfilePictureCache.getInstance(activity).get(name);
			if (name != null)
				erg.add(new ContactModel(picture, name, contacts2.get(i).mBody, contacts2.get(i).getDate(), contacts2
						.get(i).mRead == 0));
			else
				erg.add(new ContactModel(picture, contacts2.get(i).mAddress, contacts2.get(i).mBody, contacts2.get(i)
						.getDate(), contacts2.get(i).mRead == 0));
		}
		return erg;
	}

	public int getPositionOfItem(String s) {
		if (MainActivity.getContacts() == null)
			return 0;
		for (int i = 0; i < MainActivity.getContacts().size(); i++) {
			if (MainActivity.getContacts().get(i).mAddress.equals(s))
				return i;
		}
		return -1;
	}

	public ArrayList<SMSItem> readSMSContacts(Activity activity) {
		LinkedHashSet<SMSItem> smsList = new LinkedHashSet<SMSItem>();
		Cursor cur = activity.getContentResolver().query(Uri.parse("content://sms"), null, null, null, null);
		if (cur != null && cur.moveToFirst()) {
			SMSItem.initIdx(cur);
			do {
				SMSItem item = new SMSItem(cur);
				smsList.add(item);
			} while (cur.moveToNext());
		}
		return new ArrayList<SMSItem>(smsList);
	}

	public ListView getListView() {
		return contactList;
	}

}
