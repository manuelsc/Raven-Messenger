package at.flack.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.ExecutionException;

import at.flack.FacebookMainActivity;
import at.flack.MainActivity;
import at.flack.R;
import at.flack.ui.ProfilePictureCache;

public class MyPreferenceFragment extends PreferenceFragment {
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

        profile_picture.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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
        myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                getActivity().getFragmentManager().beginTransaction().replace(R.id.prefscontent,
                        new MyPreferenceFragment2()).commit();
                return true;
            }
        });

        openDefSMS.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

        img_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new File(getActivity().getFilesDir(), "profile_picture_cache.dat").delete();
                Toast.makeText(getActivity(),
                        getActivity().getResources().getString(R.string.settings_fb_image_clear_cache),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        fb_logout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                logout();
                new File(getActivity().getFilesDir(), "cookie.dat").delete();
                Toast.makeText(getActivity(),
                        getActivity().getResources().getString(R.string.settings_logout_fb_done),
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mail_logout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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