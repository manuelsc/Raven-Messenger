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

package at.flack.contacts;

import java.text.SimpleDateFormat;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

public class SMSItem implements Comparable {
	public static final String ID = "_id";
	public static final String THREAD = "thread_id";
	public static final String ADDRESS = "address";
	public static final String PERSON = "person";
	public static final String DATE = "date";
	public static final String READ = "read";
	public static final String BODY = "body";
	public static final String SUBJECT = "subject";

	public static final String[] PHOTO_ID_PROJECTION = new String[] { ContactsContract.Contacts.PHOTO_ID };

	public static final String[] PHOTO_BITMAP_PROJECTION = new String[] { ContactsContract.CommonDataKinds.Photo.PHOTO };

	public String mAddress;
	public String mBody;
	public String mSubject;
	public long mID;
	public long mThreadID;
	public long mDate;
	public long mRead;
	public long mPerson;
	public Uri profilePicture;
	public int photoID;

	private static int mIdIdx;
	private static int mThreadIdx;
	private static int mAddrIdx;
	private static int mPersonIdx;
	private static int mDateIdx;
	private static int mReadIdx;
	private static int mBodyIdx;
	private static int mSubjectIdx;

	public SMSItem(Cursor cur) {
		mID = cur.getLong(mIdIdx);
		mThreadID = cur.getLong(mThreadIdx);
		mAddress = cur.getString(mAddrIdx);
		mPerson = cur.getLong(mPersonIdx);
		mDate = cur.getLong(mDateIdx);
		mRead = cur.getLong(mReadIdx);
		mBody = cur.getString(mBodyIdx);
		mSubject = cur.getString(mSubjectIdx);
	}

	public SMSItem() {

	}

	public String printArray(String[] sa) {
		String erg = "";
		for (String s : sa)
			erg += s + "; ";
		return erg;
	}

	public SMSItem(Cursor cur, boolean b) {
		mID = cur.getLong(mIdIdx);
		mAddress = cur.getString(cur.getColumnIndex(PhoneNumberUtils
				.formatNumber(ContactsContract.CommonDataKinds.Phone.NUMBER)));
		mPerson = cur.getLong(mPersonIdx);
	}

	public static void initIdx(Cursor cur) {
		mIdIdx = cur.getColumnIndex(ID);
		mThreadIdx = cur.getColumnIndex(THREAD);
		mAddrIdx = cur.getColumnIndex(ADDRESS);
		mPersonIdx = cur.getColumnIndex(PERSON);
		mDateIdx = cur.getColumnIndex(DATE);
		mReadIdx = cur.getColumnIndex(READ);
		mBodyIdx = cur.getColumnIndex(BODY);
		mSubjectIdx = cur.getColumnIndex(SUBJECT);
	}

	public String toString() {
		String ret = ID + ":" + String.valueOf(mID) + " " + THREAD + ":" + String.valueOf(mThreadID) + " " + ADDRESS
				+ ":" + mAddress + " " + PERSON + ":" + String.valueOf(mPerson) + " " + DATE + ":"
				+ String.valueOf(mDate) + " " + READ + ":" + String.valueOf(mRead) + " " + SUBJECT + ":" + mSubject
				+ " " + BODY + ":" + mBody;
		return ret;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(mThreadID).hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return Long.valueOf(mThreadID).equals(((SMSItem) o).mThreadID);
	}

	@Override
	public int compareTo(Object arg0) {
		return mAddress.compareTo(((SMSItem) (arg0)).mAddress);
	}

	public String getDate() {
		long cur = System.currentTimeMillis();
		String erg = "";
		if ((long) (cur / 86400000) == (long) (mDate / 86400000))
			erg = new SimpleDateFormat("HH:mm").format(mDate);
		else if (mDate >= cur - 518400000)
			erg = new SimpleDateFormat("EE").format(mDate);
		else
			erg = new SimpleDateFormat("dd. MMM").format(mDate);
		return erg;
	}

	public Uri getPhotoUri() {
		return profilePicture;
	}

	private Integer fetchThumbnailId(Context context) {

		final Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri
				.encode(this.mAddress));
		final Cursor cursor = context.getContentResolver().query(uri, PHOTO_ID_PROJECTION, null, null,
				ContactsContract.Contacts.DISPLAY_NAME + " ASC");

		try {
			Integer thumbnailId = null;
			if (cursor.moveToFirst()) {
				thumbnailId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
			}
			return thumbnailId;
		} finally {
			cursor.close();
		}

	}

	public final Bitmap fetchThumbnail(Context context) {
		Integer id = fetchThumbnailId(context);
		if (id == null)
			return null;
		final Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, id);
		final Cursor cursor = context.getContentResolver().query(uri, PHOTO_BITMAP_PROJECTION, null, null, null);

		try {
			Bitmap thumbnail = null;
			if (cursor.moveToFirst()) {
				final byte[] thumbnailBytes = cursor.getBlob(0);
				if (thumbnailBytes != null) {
					thumbnail = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.length);
				}
			}
			return thumbnail;
		} finally {
			cursor.close();
		}
	}

}