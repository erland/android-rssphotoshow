package info.isaksson.rssphotoshow;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    SharedPreferences.OnSharedPreferenceChangeListener listener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preference preference = findPreference(key);
                updateSummaryWithValue(preference);
            }
        };
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = getPreferenceScreen().getPreference(i);
            updateSummaryWithValue(preference);
        }
    }

    private void updateSummaryWithValue(Preference preference) {
        if (preference instanceof ListPreference) {
            preference.setSummary("(" + ((ListPreference) preference).getEntry() + ")");
        } else if (preference instanceof EditTextPreference) {
            String value = ((EditTextPreference) preference).getText();
            if (value != null) {
                preference.setSummary("(" + ((EditTextPreference) preference).getText() + ")");
            } else {
                preference.setSummary("");
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (listener != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        }
        super.onDestroy();
    }
}
