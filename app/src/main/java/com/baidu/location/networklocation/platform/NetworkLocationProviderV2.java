package com.baidu.location.networklocation.platform;

import android.annotation.TargetApi;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;

import com.baidu.location.networklocation.data.LocationCalculator;
import com.baidu.location.networklocation.helper.Reflected;
import com.baidu.location.networklocation.provider.INetworkLocationProvider;
import com.baidu.location.tyd.BaiduNetworkLocationService;
import com.baidu.location.tyd.NetworkLocationThread;

import com.android.location.provider.LocationProviderBase;
import com.android.location.provider.LocationRequestUnbundled;
import com.android.location.provider.ProviderPropertiesUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;

@TargetApi(17)
class NetworkLocationProviderV2 extends LocationProviderBase implements INetworkLocationProvider {

	private final static String IDENTIFIER = "network";
	private static final String TAG = "nlp.NetworkLocationProviderV2";
	private static final int MIN_AUTO_TIME = 5000;
	private NetworkLocationThread background = new NetworkLocationThread();
	private boolean enabledByService = false;
	private boolean enabledBySetting = false;

	public NetworkLocationProviderV2() {
		// Note: Also this does not totally reflect how we work, we use the same Properties as Google's Provider for compatibility reasons
		super(TAG, ProviderPropertiesUnbundled
				.create(true, false, true, false, false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE));
	}

	@Deprecated
	public NetworkLocationProviderV2(final boolean internal) {
		this();
	}

	@Override
	public synchronized void disable() {
		background.setLocationProvider(null);
		background.disable();
		enabledByService = false;
	}

	@Override
	public synchronized void enable() {
		enabledByService = true;
		if (enabledBySetting)
			enableBackground();
	}

	private void enableBackground() {
		background.disable();
		background = new NetworkLocationThread(background);
		background.setLocationProvider(this);
		background.start();
	}

	@Override
	public boolean isActive() {
		return (background != null) && background.isAlive() && background.isActive();
	}

	@Override
	public synchronized void onDisable() {
		enabledBySetting = false;
		background.disable();
	}

	@Override
	public synchronized void onEnable() {
		enabledBySetting = true;
		if (enabledByService)
			enableBackground();
	}

	@Override
	public int onGetStatus(final Bundle arg0) {
		return LocationProvider.AVAILABLE;
	}

	@Override
	public long onGetStatusUpdateTime() {
		return background.getLastTime();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Reporting: " + location);
		}
		if (location != null) {
			background.setLastTime(SystemClock.elapsedRealtime());
			background.setLastLocation(location);
			Reflected.androidLocationLocationMakeComplete(location);
			Reflected.androidLocationLocationSetExtraLocation(location, "noGPSLocation", new Location(location));
			reportLocation(location);
		}
	}

	@Override
	public void onSetRequest(final ProviderRequestUnbundled requests, final WorkSource ws) {
		long autoTime = Long.MAX_VALUE;
		boolean autoUpdate = false;
		for (final LocationRequestUnbundled request : requests.getLocationRequests()) {
			if (request.getInterval() < autoTime) {
				autoTime = request.getInterval();
			}
			autoUpdate = true;
		}
		if (autoTime < MIN_AUTO_TIME) {
			autoTime = MIN_AUTO_TIME;
		}
		background.setAuto(autoUpdate, autoTime);
	}

	@Override
	public void setCalculator(LocationCalculator locationCalculator) {
		background.setCalculator(locationCalculator);
	}

}
