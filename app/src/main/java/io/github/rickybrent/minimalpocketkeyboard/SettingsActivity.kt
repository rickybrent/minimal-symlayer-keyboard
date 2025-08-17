package io.github.rickybrent.minimalpocketkeyboard

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.settings_activity)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.settings, SettingsFragment())
				.commit()
		}
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onSupportNavigateUp(): Boolean {
		if (supportFragmentManager.popBackStackImmediate()) {
			return true
		}
		return super.onSupportNavigateUp()
	}

	override fun onPreferenceStartFragment(
		caller: PreferenceFragmentCompat,
		pref: Preference
	): Boolean {
		// Instantiate the new Fragment
		val args = pref.extras
		val fragment = supportFragmentManager.fragmentFactory.instantiate(
			classLoader,
			pref.fragment!!
		)
		fragment.arguments = args
		fragment.setTargetFragment(caller, 0)
		// Replace the existing Fragment with the new one
		supportFragmentManager.beginTransaction()
			.replace(R.id.settings, fragment)
			.addToBackStack(null)
			.commit()
		return true
	}

	class SettingsFragment : PreferenceFragmentCompat() {
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.preferences, rootKey)
			val context = activity
			if(context != null) {
				findPreference<Preference>("Reset")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
					AlertDialog.Builder(context)
						.setTitle("Reset settings")
						.setMessage("Do you really want to reset all the settings to their default value?")
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
							val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
							editor.clear()
							editor.commit()
							setPreferencesFromResource(R.xml.preferences, rootKey)
						})
						.setNegativeButton(android.R.string.no, null)
						.show()
					true
				}
			}
		}
	}

	class AboutFragment : PreferenceFragmentCompat() {
		data class LicenseInfo(
			val componentName: String,
			val licenseName: String,
			val upstream: Uri,
			val resourceId: Int
		)

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			setPreferencesFromResource(R.xml.preferences_about, rootKey)

			try {
				val version = requireActivity().packageManager.getPackageInfo(
					requireActivity().packageName,
					0
				).versionName
				findPreference<Preference>("about_version")?.summary = "Version $version"
			} catch (e: Exception) {
				findPreference<Preference>("about_version")?.summary = "Version not available"
			}

			findPreference<Preference>("about_main_license")?.setOnPreferenceClickListener {
				showLicenseTextDialog("Application License", R.raw.license_gpl)
				true
			}

			findPreference<Preference>("about_third_party")?.setOnPreferenceClickListener {
				showThirdPartyLicenseList()
				true
			}

			findPreference<Preference>("about_github")?.setOnPreferenceClickListener {
				val intent = Intent(
					Intent.ACTION_VIEW,
					"https://github.com/rickybrent/minimalpocketkeyboard".toUri()
				)
				startActivity(intent)
				true
			}
		}

		private fun showThirdPartyLicenseList() {
			val licenses = listOf(
				LicenseInfo(
					"all_emojis.txt",
					"Unicode License",
					"https://github.com/Mange/emoji-data".toUri(),
					R.raw.license_unicode
				),
				LicenseInfo(
					"Material Symbols Icons",
					"Apache License v2.0",
					"https://fonts.google.com/icons".toUri(),
					R.raw.license_apache2
				)
				// Add other licenses here, for example:
				// LicenseInfo("Custom Font Name", "SIL Open Font License", R.raw.license_sil_ofl)
			)

			val licenseDisplayNames =
				licenses.map { "${it.componentName} - ${it.licenseName}" }.toTypedArray()

			AlertDialog.Builder(requireContext())
				.setTitle("Third-Party Licenses")
				.setItems(licenseDisplayNames) { _, which ->
					val selectedLicense = licenses[which]
					showLicenseTextDialog(selectedLicense.componentName, selectedLicense.resourceId)
				}
				.setPositiveButton(android.R.string.ok, null)
				.show()
		}

		private fun showLicenseTextDialog(title: String, resourceId: Int) {
			val licenseText = try {
				resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
			} catch (e: Exception) {
				"Could not load license."
			}

			AlertDialog.Builder(requireContext())
				.setTitle(title)
				.setMessage(licenseText)
				.setPositiveButton(android.R.string.ok, null)
				.show()
		}
	}
}