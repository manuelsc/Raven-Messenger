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

import java.io.File;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Telephony;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.widget.Toast;
import at.flack.FacebookMainActivity;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.services.EMailService;
import at.flack.services.FacebookService;
import at.flack.ui.ColorSelector2;
import at.flack.ui.ProfilePictureCache;
import at.flack.utils.NotificationService;

import com.gc.materialdesign.widgets.ColorSelector.OnColorSelectedListener;

public class QuickPrefsActivity extends ActionBarActivity {

	public static int fragment;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_prefs);

		Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		fragment = 0;
		getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();

	}

	@Override
	public void onBackPressed() {
		if (QuickPrefsActivity.fragment == 0) {
			super.onBackPressed();
		} else {
			getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (QuickPrefsActivity.fragment == 0) {
				NavUtils.navigateUpFromSameTask(this);
				return true;
			} else {
				getFragmentManager().beginTransaction().replace(R.id.prefscontent, new MyPreferenceFragment()).commit();
				return true;
			}
		}
		return false;
	}

	class MyPreferenceFragment extends PreferenceFragment {
		private int color;
		private Activity ac;

		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			ac = getActivity();
			addPreferencesFromResource(R.xml.preferences);
			QuickPrefsActivity.fragment = 0;

			Preference img_cache = (Preference) findPreference("delete_img_cache");
			Preference mail_logout = (Preference) findPreference("mail_logout");
			Preference fb_logout = (Preference) findPreference("fb_logout");
			ListPreference profile_picture = (ListPreference) findPreference("profile_picture_from");
			getActivity().setTitle(this.getResources().getString(R.string.action_settings));

			Preference myPref = (Preference) findPreference("open_sub_notification");
			Preference openDefSMS = (Preference) findPreference("open_settings_default_sms");

			openDefSMS.setEnabled(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
					&& !Telephony.Sms.getDefaultSmsPackage(ac).equals(ac.getPackageName()));

			profile_picture.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (((String) newValue).equals("2")) { // From Facebook
						if (ac != null) {
							new Thread() {
								public void start() {
									try {
										ProfilePictureCache.getInstance(ac).put(
												"2MyprofilePicture",
												FacebookMainActivity.getInstance().getProfilePicture(
														MainActivity.fb_api.getProfilePicture()));
										ProfilePictureCache.getInstance(ac).save(ac);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}.start();

						}
					} else if (((String) newValue).equals("1")) {
						try {
							Bitmap profile_picture = MainActivity.fetchThumbnail(ac, ((TelephonyManager) ac
									.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number());
							if (profile_picture != null) {
								ProfilePictureCache.getInstance(ac).put("2MyprofilePicture", profile_picture);
							}
							ProfilePictureCache.getInstance(ac).save(ac);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return true;
				}
			});
			myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					getActivity().getFragmentManager().beginTransaction().replace(R.id.prefscontent,
							new MyPreferenceFragment2()).commit();
					return true;
				}
			});

			openDefSMS.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
							&& !Telephony.Sms.getDefaultSmsPackage(ac).equals(ac.getPackageName())) {
						Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
						intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ac.getPackageName());
						startActivity(intent);
					}
					return true;
				}
			});

			img_cache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					new File(getActivity().getFilesDir(), "profile_picture_cache.dat").delete();
					Toast.makeText(getActivity(),
							getActivity().getResources().getString(R.string.settings_fb_image_clear_cache),
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});
			fb_logout.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					logout();
					new File(getActivity().getFilesDir(), "cookie.dat").delete();
					Toast.makeText(getActivity(),
							getActivity().getResources().getString(R.string.settings_logout_fb_done),
							Toast.LENGTH_SHORT).show();
					return true;
				}
			});

			mail_logout.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences mail = MyPreferenceFragment.this.getActivity().getSharedPreferences("mail",
							Context.MODE_PRIVATE);
					mail.edit().clear().commit();
					MainActivity.mailprofile = null;
					new File(MyPreferenceFragment.this.getActivity().getFilesDir(), "mails_outgoing.dat").delete();
					Toast.makeText(getActivity(),
							getActivity().getResources().getString(R.string.settings_logout_mail_done),
							Toast.LENGTH_SHORT).show();
					return true;
				}

			});

		}
	}

	class MyPreferenceFragment2 extends PreferenceFragment {
		private int color;

		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences_notification);

			QuickPrefsActivity.fragment = 1;

			getActivity().setTitle(getResources().getString(R.string.sub_notifications));
			Preference led_color = (Preference) findPreference("notification_light");
			SwitchPreference fb_service = (SwitchPreference) findPreference("notification_fbs");
			SwitchPreference mail_service = (SwitchPreference) findPreference("notification_mail");

			mail_service.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean value = (Boolean) newValue;
					if (value == true) {
						if (MyPreferenceFragment2.this.getActivity() != null) {
							new NotificationService(MyPreferenceFragment2.this.getActivity())
									.startNotificationService();
						}
					}
					if (value == false) {
						EMailService.WorkerThread.staticBoolean = false;
					}
					return true;
				}

			});

			fb_service.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Boolean value = (Boolean) newValue;
					if (value == true) {
						if (MyPreferenceFragment2.this.getActivity() != null) {
							new NotificationService(MyPreferenceFragment2.this.getActivity())
									.startNotificationService();
						}
					}
					if (value == false) {
						FacebookService.staticBoolean = false;
					}
					return true;
				}

			});

			led_color.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					final SharedPreferences led = PreferenceManager
							.getDefaultSharedPreferences(MyPreferenceFragment2.this.getActivity());
					int def = led.getInt("notification_light", -16776961);
					final ColorSelector2 colorSelector = new ColorSelector2(MyPreferenceFragment2.this.getActivity(),
							def, new OnColorSelectedListener() {
								@Override
								public void onColorSelected(int color) {
									led.edit().putInt("notification_light", color).commit();
								}

							});
					colorSelector.show();

					return false;
				}

			});

		}
	}

	public void logout() {
		try {
			new FacebookLogout().execute().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	class FacebookLogout extends AsyncTask<Void, Void, Void> {

		private Exception exception;

		@Override
		protected Void doInBackground(Void... params) {
			try {
				MainActivity.fb_api.logout();
			} catch (Exception e) {
			}
			return null;
		}
	}

}
