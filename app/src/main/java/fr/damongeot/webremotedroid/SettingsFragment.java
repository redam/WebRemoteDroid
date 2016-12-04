package fr.damongeot.webremotedroid;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by regis on 06/10/16.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
