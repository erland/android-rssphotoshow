package info.isaksson.rssphotoshow;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    SharedPreferences.OnSharedPreferenceChangeListener listener = null;
    List<RepositoryEntry> repositoryEntries = new ArrayList<RepositoryEntry>();

    private static class RepositoryParser extends DefaultHandler {
        List<RepositoryEntry> entries = new ArrayList<RepositoryEntry>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("feed")) {
                String name = attributes.getValue("name");
                String url = attributes.getValue("url");
                String provider = attributes.getValue("provider");
                entries.add(new RepositoryEntry(name, url, provider));
            }
        }

        public List<RepositoryEntry> getEntries() {
            return entries;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preference preference = findPreference(key);
                updateSummaryWithValue(sharedPreferences, preference);
                if (key.equals("repositoryflow")) {
                    String url = sharedPreferences.getString("repositoryflow", null);
                    preference.setSummary(((ListPreference) preference).getEntry());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("imageflowurl", url);
                    editor.commit();
                }
            }
        };
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference preference = getPreferenceScreen().getPreference(i);
            updateSummaryWithValue(getPreferenceScreen().getSharedPreferences(), preference);
        }
        ListPreference repositoryFlow = (ListPreference) findPreference("repositoryflow");
        repositoryFlow.setEnabled(false);

        new AsyncTask<Void, Void, List<RepositoryEntry>>() {
            @Override
            protected List<RepositoryEntry> doInBackground(Void... voids) {
                HttpClient client = new DefaultHttpClient();
                try {
                    HttpResponse response = client.execute(new HttpGet("http://rssphotoshow.isaksson.info/repository.xml"));
                    HttpEntity entity = response.getEntity();
                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                    RepositoryParser rssParser = new RepositoryParser();
                    parser.parse(entity.getContent(), rssParser);
                    return rssParser.getEntries();
                } catch (SAXException e) {
                    Log.e(SettingsActivity.class.getName(), "Error when parsing feed repository", e);
                } catch (ClientProtocolException e) {
                    Log.e(SettingsActivity.class.getName(), "Error retrieving feed repository", e);
                } catch (ParserConfigurationException e) {
                    Log.e(SettingsActivity.class.getName(), "Error when configuring parser for feed repository", e);
                } catch (IOException e) {
                    Log.e(SettingsActivity.class.getName(), "Error when reading feed repository", e);
                }
                return new ArrayList<RepositoryEntry>();
            }

            @Override
            protected void onPostExecute(List<RepositoryEntry> repositoryEntries) {
                ListPreference repositoryFlow = (ListPreference) findPreference("repositoryflow");
                EditTextPreference imageFlowUrl = (EditTextPreference) findPreference("imageflowurl");
                List<String> entries = new ArrayList<String>();
                List<String> values = new ArrayList<String>();
                for (RepositoryEntry entry : repositoryEntries) {
                    entries.add(entry.getName() + (entry.getProvider() != null ? "\n(" + entry.getProvider() + ")" : ""));
                    values.add(entry.getUrl());
                    if (imageFlowUrl != null && imageFlowUrl.getText().equals(entry.getUrl())) {
                        repositoryFlow.setSummary(entry.getName());
                    }
                }
                repositoryFlow.setEntries(entries.toArray(new String[0]));
                repositoryFlow.setEntryValues(values.toArray(new String[0]));
                repositoryFlow.setEnabled(true);
                SettingsActivity.this.repositoryEntries = repositoryEntries;
            }
        }.execute();
    }

    private static class RepositoryEntry {
        private String name;
        private String url;
        private String provider;

        private RepositoryEntry(String name, String url, String provider) {
            this.name = name;
            this.url = url;
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    private void updateSummaryWithValue(SharedPreferences sharedPreferences, Preference preference) {
        if (preference instanceof ListPreference) {
            if (!preference.getKey().equals("repositoryflow")) {
                if (((ListPreference) preference).getEntry() != null) {
                    preference.setSummary(((ListPreference) preference).getEntry());
                } else {
                    preference.setSummary("");
                }
            }
        } else if (preference instanceof EditTextPreference) {
            String value = sharedPreferences.getString(preference.getKey(), null);
            if (value != null) {
                preference.setSummary(value);
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
