package com.baidu.location.tyd;

import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import com.baidu.location.networklocation.data.LocationCalculator;
import com.baidu.location.networklocation.provider.INetworkLocationProvider;


public class NetworkLocationThread extends Thread {

	private static final String TAG = "nlp.NetworkLocationThread";
	private LocationCalculator calculator;
	private long autoTime;
	private boolean autoUpdate;
	private boolean forceUpdate;
	private boolean enabled;
	private Location lastLocation;
	private INetworkLocationProvider locationProvider;
	private long lastTime;

	public NetworkLocationThread() {
		autoUpdate = false;
		autoTime = Long.MAX_VALUE;
		lastTime = 0;
		enabled = true;
		forceUpdate = true;
	}

	public NetworkLocationThread(LocationCalculator calculator) {
		this();
		this.calculator = calculator;
	}

	public NetworkLocationThread(NetworkLocationThread oldThread) {
		this();
		if (oldThread != null) {
			lastLocation = oldThread.lastLocation;
			lastTime = oldThread.lastTime;
			calculator = oldThread.calculator;
		}
	}

	public void disable() {
		enabled = false;
		synchronized (this) {
			notify();
		}
	}

	public long getLastTime() {
		return lastTime;
	}

	public void setLastTime(final long lastTime) {
		this.lastTime = lastTime;
	}

	public boolean isActive() {
		return enabled && autoUpdate && (autoTime < 60000);
	}

	@Override
	public void run() {
		while (enabled) {
			boolean waited = false;
			try {
				synchronized (this) {
					if (!autoUpdate && !forceUpdate && enabled) {
						if (BaiduNetworkLocationService.DEBUG)
							Log.d(TAG, "We're not active, wait until we are...");
						wait();
						waited = true;
					}
				}
			} catch (final InterruptedException e) {
			}
			long wait;
			while ((wait = lastTime + autoTime - SystemClock.elapsedRealtime()) > 0 && autoUpdate && !forceUpdate &&
				   enabled) {
				final float w = wait / 1000F;
				if (BaiduNetworkLocationService.DEBUG) {
					Log.d(TAG, "lastTime: " + lastTime + " autoTime: " + autoTime + " currentTime: " +
							   SystemClock.elapsedRealtime());
					Log.d(TAG, "waiting " + w + "s to update...");
				}
				try {
					synchronized (this) {
						wait(wait);
					}
					waited = true;
				} catch (final InterruptedException e) {
					break;
				}
			}

			if (!waited && enabled) {
				if (BaiduNetworkLocationService.DEBUG) {
					Log.d(TAG, "We did not wait, lastTime: " + lastTime + " autoTime: " + autoTime + " currentTime: " +
							   SystemClock.elapsedRealtime());
					Log.w(TAG, "waiting 5s to prevent mass update...");
				}
				try {
					synchronized (this) {
						wait(5000);
					}
				} catch (final InterruptedException e) {
					continue;
				}
			}
			if ((autoUpdate || forceUpdate) && calculator != null && enabled) {
				if (forceUpdate) {
					if (BaiduNetworkLocationService.DEBUG)
						Log.d(TAG, "Update forced because of new incoming request");
					forceUpdate = false;
				}
				lastTime = SystemClock.elapsedRealtime();
				if (locationProvider != null) {
					if (BaiduNetworkLocationService.DEBUG)
						Log.d(TAG, "Now requesting \\o/");
					locationProvider.onLocationChanged(calculator.getCurrentLocation());
				}
			} else {
				if (BaiduNetworkLocationService.DEBUG)
					Log.d(TAG, "we're not active (or not initialized yet) = do not track!");
			}
		}
	}

	public void setAuto(final boolean autoUpdate, final long autoTime) {
		synchronized (this) {
			if (autoTime < this.autoTime) {
				forceUpdate = true;
			}
			this.autoUpdate = autoUpdate;
			this.autoTime = autoTime;
			notify();
		}
	}

	public void setCalculator(LocationCalculator calculator) {
		this.calculator = calculator;
	}

	public void setLastLocation(final Location location) {
		lastLocation = location;
	}

	public void setLocationProvider(INetworkLocationProvider locationProvider) {
		this.locationProvider = locationProvider;
	}
}
