package io.github.rickybrent.minimal_symlayer_keyboard

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceDataStore
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.settings_activity)
		setSupportActionBar(findViewById(R.id.toolbar))
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

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.settings_menu, menu)
		val searchItem = menu.findItem(R.id.action_search)
		val searchView = searchItem.actionView as SearchView
		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			override fun onQueryTextSubmit(query: String?): Boolean {
				return false
			}

			override fun onQueryTextChange(newText: String?): Boolean {
				val fragment = supportFragmentManager.findFragmentById(R.id.settings) as? SettingsFragment
				fragment?.filterPreferences(newText)
				return true
			}
		})
		return true
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
			preferenceManager.preferenceDataStore = DeviceProtectedPreferenceDataStore(requireContext())
			setPreferencesFromResource(R.xml.preferences, rootKey)
			val context = activity
			if(context != null) {
				findPreference<Preference>("Reset")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
					AlertDialog.Builder(context, R.style.AlertDialogTheme)
						.setTitle("Reset settings")
						.setMessage("Do you really want to reset all the settings to their default value?")
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(android.R.string.yes, DialogInterface.OnClickListener { dialog, which ->
							val sharedPreferences = context.createDeviceProtectedStorageContext().getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
							sharedPreferences.edit { clear() }
							setPreferencesFromResource(R.xml.preferences, rootKey)
						})
						.setNegativeButton(android.R.string.no, null)
						.show()
					true
				}
			}
		}

		fun filterPreferences(query: String?) {
			val preferenceScreen = preferenceScreen
			val lowerCaseQuery = query?.lowercase()?.trim()

			for (i in 0 until preferenceScreen.preferenceCount) {
				val preference = preferenceScreen.getPreference(i)
				if (preference is PreferenceGroup) {
					filterPreferenceGroup(preference, lowerCaseQuery)
				} else {
					filterPreference(preference, lowerCaseQuery)
				}
			}
		}

		private fun filterPreference(preference: Preference, query: String?): Boolean {
			val title = preference.title.toString().lowercase()
			val summary = preference.summary?.toString()?.lowercase() ?: ""
			val visible = query.isNullOrEmpty() || title.contains(query) || summary.contains(query)
			preference.isVisible = visible
			return visible
		}

		private fun filterPreferenceGroup(preferenceGroup: PreferenceGroup, query: String?): Boolean {
			var visible = false
			for (i in 0 until preferenceGroup.preferenceCount) {
				val preference = preferenceGroup.getPreference(i)
				if (preference is PreferenceGroup) {
					if (filterPreferenceGroup(preference, query)) {
						visible = true
					}
				} else {
					if (filterPreference(preference, query)) {
						visible = true
					}
				}
			}
			preferenceGroup.isVisible = visible
			return visible
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
					"https://github.com/rickybrent/minimal-symlayer-keyboard".toUri()
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

class DeviceProtectedPreferenceDataStore(context: Context) : PreferenceDataStore() {
	private val sharedPreferences by lazy {
		val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			context.createDeviceProtectedStorageContext()
		} else {
			context
		}
		storageContext.getSharedPreferences(
			"${context.packageName}_preferences",
			Context.MODE_PRIVATE
		)
	}

	override fun putString(key: String?, value: String?) {
		sharedPreferences.edit { putString(key, value) }
	}

	override fun getString(key: String?, defValue: String?): String? {
		return sharedPreferences.getString(key, defValue)
	}

	override fun putBoolean(key: String?, value: Boolean) {
		sharedPreferences.edit { putBoolean(key, value) }
	}

	override fun getBoolean(key: String?, defValue: Boolean): Boolean {
		return sharedPreferences.getBoolean(key, defValue)
	}

	override fun putInt(key: String?, value: Int) {
		sharedPreferences.edit { putInt(key, value) }
	}

	override fun getInt(key: String?, defValue: Int): Int {
		return sharedPreferences.getInt(key, defValue)
	}
}