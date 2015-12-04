package lab.lab3b;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;


public class SettingsActivity extends PreferenceActivity {

	public static final String PREF_START_DELAY = "PREF_START_DELAY";
	public static final String PREF_SENSOR_FREQ = "PREF_SENSOR_FREQ";
	public static final String PREF_SERVER_IP = "PREF_SERVER_IP";
	public static final String PREF_SERVER_PORT = "PREF_SERVER_PORT";
	public static final String PREF_FILENAME = "PREF_FILENAME";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new MyPreferenceFragment())
				.commit();
		// well, the below call is deprecated...
		// addPreferencesFromResource(R.xml.userpreferences);
	}

	public static class MyPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.userpreferences);
		}
	}
}
