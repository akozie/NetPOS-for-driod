package com.woleapp.netpos.contactless.ui.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.woleapp.netpos.contactless.R

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