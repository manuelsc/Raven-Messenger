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

package at.flack.activity;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;

import safe.KeyEntity;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import at.flack.R;
import at.flack.contacts.ContactNameMap;
import at.flack.exchange.KeySafe;
import at.flack.ui.ContactAdapter;
import at.flack.ui.ContactModel;
import at.flack.ui.ProfilePictureCache;

public class NewSMSContactActivity extends ActionBarActivity implements SearchView.OnQueryTextListener {

	private ListView contactList;
	public static ContactNameMap contactNameMap;
	private ProfilePictureCache fb_img;
	public ArrayList<ContactModel> contacts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_contact);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		contactNameMap = new ContactNameMap(this);

		contactList = (ListView) this.findViewById(R.id.listview);

		TextView padding = new TextView(this);
		padding.setHeight(10);
		contactList.addHeaderView(padding);
		contactList.setHeaderDividersEnabled(false);
		contactList.addFooterView(padding, null, false);
		contactList.setFooterDividersEnabled(false);

		updateContacts();
		contactList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (contacts == null) {
					updateContacts();
				}
				openMessageActivity(NewSMSContactActivity.this, arg2 - 1);
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.new_contact, menu);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		MenuItem searchMenuItem = menu.findItem(R.id.search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setSubmitButtonEnabled(true);
		searchView.setOnQueryTextListener(this);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.action_message_encrypted: {
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	public void openMessageActivity(Activity activity, int arg2) {
		if (arg2 < 0)
			return;
		Intent smsIntent = new Intent(activity, MessageOverview.class);
		ContactAdapter adapter = ((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter())
				.getWrappedAdapter());
		smsIntent.putExtra("CONTACT_NUMBER", ((ContactModel) adapter.getItem(arg2)).getLastMessage());
		smsIntent.putExtra("CONTACT_NAME", ((ContactModel) (adapter.getItem(arg2))).getTitle());
		smsIntent.putExtra("CONTACT_ID", ((ContactModel) (adapter.getItem(arg2))).getThreadID());

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Bitmap temp = ((ContactModel) (adapter.getItem(arg2))).getPicture();
		if (temp != null) {
			temp.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] bytes = stream.toByteArray();

			smsIntent.putExtra("profilePicture", bytes);
		}
		try {
			KeyEntity key = KeySafe.getInstance(this).get(
					PhoneNumberUtils.formatNumber(contacts.get(arg2).getLastMessage()));
			if (key != null)
				smsIntent.putExtra("CONTACT_KEY", key);
		} catch (NullPointerException e) {
		}
		activity.startActivityForResult(smsIntent, 2);
	}

	public void updateContacts() {
		if (contacts == null)
			contacts = readSMSContacts(this);

		if (contactList != null && contactList.getAdapter() != null) {
			if (contacts.size() > 0 && contactList != null) {
				((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).refill(
						contacts, 0);
			}
		} else {
			if (contactList != null) {
				ContactAdapter arrayAdapter = new ContactAdapter(this, contacts, 0);
				contactList.setAdapter(arrayAdapter);
			}
		}
	}

	public ArrayList<ContactModel> readSMSContacts(Activity activity) {
		LinkedHashSet<ContactModel> smsList = new LinkedHashSet<ContactModel>();
		Cursor phones = activity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
				null, null, null);
		while (phones.moveToNext()) {
			String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
			String phoneNumber = phones.getString(phones.getColumnIndex(PhoneNumberUtils
					.formatNumber(ContactsContract.CommonDataKinds.Phone.NUMBER)));
			String photo = phones.getString(phones
					.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
			String primary = name;
			if (name == null)
				primary = phoneNumber;
			smsList.add(new ContactModel(fetchThumbnail(activity, photo != null ? Uri.parse(photo) : null), primary,
					phoneNumber, "", false));
		}
		phones.close();
		ArrayList<ContactModel> unsorted = new ArrayList<ContactModel>(smsList);
		Collections.sort(unsorted, new Comparator<ContactModel>() {
			@Override
			public int compare(ContactModel lhs, ContactModel rhs) {
				return lhs.getTitle().compareTo(rhs.getTitle());
			}
		});
		return unsorted;
	}

	public final Bitmap fetchThumbnail(Context context, Uri uri) {
		if (uri == null)
			return null;
		FileDescriptor fileDescriptor;
		try {
			fileDescriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r").getFileDescriptor();
		} catch (FileNotFoundException e) {
			return null;
		}

		if (fileDescriptor != null) {
			return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, null);
		}
		return null;
	}

	@Override
	public boolean onQueryTextChange(String arg0) {
		((ContactAdapter) ((HeaderViewListAdapter) contactList.getAdapter()).getWrappedAdapter()).getFilter().filter(
				arg0);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		return true;
	}

}
