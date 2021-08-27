package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener { prefs, key ->
            //Timber.e(Prefs.contains(key).toString())
            //Timber.e(Prefs.getString(key, ""))
        }
    }
}