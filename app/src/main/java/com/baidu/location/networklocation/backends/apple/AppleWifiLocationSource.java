package com.baidu.location.networklocation.backends.apple;

import android.content.Context;
import android.util.Log;

import com.baidu.location.tyd.BaiduNetworkLocationService;
import com.baidu.location.networklocation.data.LocationSpec;
import com.baidu.location.networklocation.data.MacAddress;
import com.baidu.location.networklocation.data.WifiSpec;
import com.baidu.location.networklocation.source.ILocationSource;
import com.baidu.location.networklocation.source.OnlineDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class AppleWifiLocationSource extends OnlineDataSource implements ILocationSource<WifiSpec> {

	public static final float LATLON_WIRE = 1E8F;
	private static final String TAG = "nlp.AppleWifiLocationSource";
	private static final String NAME = "Apple Location Service";
	private static final String DESCRIPTION = "Retrieve Wifi locations from Apple";
	private static final String COPYRIGHT = "© Apple\nLicense: proprietary or unknown";
	private final LocationRetriever locationRetriever = new LocationRetriever();

	public AppleWifiLocationSource(Context context) {
		super(context);
	}

	public static String niceMac(String mac) {
		mac = mac.toLowerCase(Locale.getDefault());
		final StringBuilder builder = new StringBuilder();
		final String[] arr = mac.split(":");
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].length() == 1) {
				builder.append("0");
			}
			builder.append(arr[i]);
			if (i < arr.length - 1) {
				builder.append(":");
			}
		}
		return builder.toString();
	}

	@Override
	public String getCopyright() {
		return COPYRIGHT;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Collection<LocationSpec<WifiSpec>> retrieveLocation(Collection<WifiSpec> specs) {
		Collection<LocationSpec<WifiSpec>> locationSpecs = new ArrayList<LocationSpec<WifiSpec>>();
		Collection<String> macs = new ArrayList<String>();
		for (WifiSpec spec : specs) {
			macs.add(niceMac(spec.getMac().toString()));
		}

		try {
			Response response = locationRetriever.retrieveLocations(macs);
			if ((response == null) || (response.wifis == null) || response.wifis.isEmpty()) {
				Log.d(TAG, "Got nothing usable from Apple's servers!");
				return locationSpecs;
			}
			int locsGet = 0;
			for (Response.ResponseWifi responseWifi : response.wifis) {
				try {
					WifiSpec wifiSpec = new WifiSpec(MacAddress.parse(responseWifi.mac), responseWifi.channel);
					if ((responseWifi.location.altitude != null) && (responseWifi.location.altitude > -500)) {
						locationSpecs
								.add(new LocationSpec<WifiSpec>(wifiSpec, responseWifi.location.latitude / LATLON_WIRE,
																responseWifi.location.longitude / LATLON_WIRE,
																responseWifi.location.accuracy,
																responseWifi.location.altitude));
					} else {
						locationSpecs
								.add(new LocationSpec<WifiSpec>(wifiSpec, responseWifi.location.latitude / LATLON_WIRE,
																responseWifi.location.longitude / LATLON_WIRE,
																responseWifi.location.accuracy));
					}
					locsGet++;
				} catch (Exception e) {
					if (BaiduNetworkLocationService.DEBUG) {
						Log.w(TAG, e);
					}
				}
			}
			if (BaiduNetworkLocationService.DEBUG) {
				Log.d(TAG, "got " + locsGet + " usable locations from server");
			}

		} catch (IOException e) {
			if (BaiduNetworkLocationService.DEBUG) {
				Log.w(TAG, e);
			}
		}
		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "got locationSpecs=" + locationSpecs);
		}
		return locationSpecs;
	}
}
