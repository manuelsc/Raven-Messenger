package at.flack.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.gc.materialdesign.widgets.ColorSelector;

import at.flack.R;
import at.flack.services.EMailService;
import at.flack.services.FacebookService;
import at.flack.ui.ColorSelector2;
import at.flack.utils.NotificationService;

public class MyPreferenceFragment2 extends PreferenceFragment {
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

        mail_service.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

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

        fb_service.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

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

        led_color.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final SharedPreferences led = PreferenceManager
                        .getDefaultSharedPreferences(MyPreferenceFragment2.this.getActivity());
                int def = led.getInt("notification_light", -16776961);
                final ColorSelector2 colorSelector = new ColorSelector2(MyPreferenceFragment2.this.getActivity(),
                        def, new ColorSelector.OnColorSelectedListener() {
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