package de.markusfisch.android.binaryeye.actions.wifi

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.widget.Toast
import de.markusfisch.android.binaryeye.R
import de.markusfisch.android.binaryeye.actions.IAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object WifiAction : IAction {
	override val iconResId = R.drawable.ic_action_wifi
	override val titleResId = R.string.connect_to_wifi

	override fun canExecuteOn(data: ByteArray): Boolean =
		WifiConfigurationFactory.parse(String(data)) != null

	override suspend fun execute(context: Context, data: ByteArray) = withContext(Dispatchers.IO) {
		val wifiConfig = WifiConfigurationFactory.parse(
			String(data)
		) ?: return@withContext
		val wifiManager = context.applicationContext.getSystemService(
			WIFI_SERVICE
		) as WifiManager

		wifiManager.enableWifi(context)
		wifiManager.mayRemoveOldNetwork(wifiConfig)
		wifiManager.enableNewNetwork(wifiConfig)
		withContext(Dispatchers.Main) {
			Toast.makeText(
				context,
				R.string.wifi_added,
				Toast.LENGTH_LONG
			).show()
		}
	}

	private suspend fun WifiManager.enableWifi(context: Context): Boolean {
		if (!isWifiEnabled) {
			if (!setWifiEnabled(true)) {
				withContext(Dispatchers.Main) {
					Toast.makeText(
						context,
						R.string.wifi_config_failed,
						Toast.LENGTH_LONG
					).show()
				}
				return false
			}
			// wait for wifi to really be enabled, takes some time
			withTimeoutOrNull(10000) {
				while (!isWifiEnabled) delay(500)
			} ?: if (!isWifiEnabled) {
				withContext(Dispatchers.Main) {
					Toast.makeText(
						context,
						R.string.wifi_config_failed,
						Toast.LENGTH_LONG
					).show()
				}
				return false
			}
		}
		return true
	}

	private suspend fun WifiManager.mayRemoveOldNetwork(
		wifiConfig: WifiConfiguration
	) {
		configuredNetworks?.firstOrNull {
			it.SSID == wifiConfig.SSID &&
				it.allowedKeyManagement == wifiConfig.allowedKeyManagement
		}?.networkId?.also {
			removeNetwork(it)
		}
	}

	private suspend fun WifiManager.enableNewNetwork(
		wifiConfig: WifiConfiguration
	) {
		val id = addNetwork(wifiConfig)
		disconnect()
		enableNetwork(id, true)
		reconnect()
	}
}
