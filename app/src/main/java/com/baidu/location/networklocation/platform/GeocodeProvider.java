package com.baidu.location.networklocation.platform;

import android.annotation.TargetApi;
import android.location.Address;
import android.location.GeocoderParams;
import android.util.Log;

import com.android.location.provider.GeocodeProviderBase;
import com.baidu.location.networklocation.database.GeocodeDatabase;
import com.baidu.location.networklocation.provider.IGeocodeProvider;
import com.baidu.location.networklocation.source.IGeocodeSource;
import com.baidu.location.tyd.BaiduNetworkLocationService;


import java.util.ArrayList;
import java.util.List;

class GeocodeProvider extends GeocodeProviderBase
		implements IGeocodeProvider {
	private static final String TAG = "nlp.LocationGeocodeProvider";
	private static final String UNKNOWN_RESULT_ERROR = "unknown";
	private GeocodeDatabase geocodeDatabase;
	private List<IGeocodeSource> sources;

	@Override
	public void setGeocodeDatabase(GeocodeDatabase geocodeDatabase) {
		this.geocodeDatabase = geocodeDatabase;
	}

	@Override
	public String onGetFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params,
									List<Address> addrs) {
		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Reverse request: " + latitude + "/" + longitude);
		}
		List<Address> addresses = null;
		if (geocodeDatabase != null) {
			addresses = geocodeDatabase.get(latitude, longitude);
		}
		if ((addresses != null) && !addresses.isEmpty()) {
			// database hit
			if (BaiduNetworkLocationService.DEBUG) {
				Log.d(TAG, "Using database entry: " + addrs.get(0));
			}
			addrs.addAll(addresses);
			return null;
		}
		for (IGeocodeSource source : sources) {
			if (source.isSourceAvailable()) {
				if (BaiduNetworkLocationService.DEBUG) {
					Log.d(TAG, "Try reverse using: " + source.getName());
				}
				try {
					addresses =
							source.getFromLocation(latitude, longitude, params.getClientPackage(), params.getLocale());
				} catch (Throwable t) {
					Log.w(TAG, source.getName() + " throws exception!", t);
				}
				if ((addresses != null) && !addresses.isEmpty()) {
					geocodeDatabase.put(latitude, longitude, addresses);
					addrs.addAll(addresses);
					if (BaiduNetworkLocationService.DEBUG) {
						Log.d(TAG, latitude + "/" + longitude + " reverse geolocated to:" + addrs.get(0));
					}
					return null; // null means everything is ok!
				}
			}
		}
		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Could not reverse geolocate: " + latitude + "/" + longitude);
		}
		return UNKNOWN_RESULT_ERROR;
	}

	@Override
	public String onGetFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude,
                                        double upperRightLatitude, double upperRightLongitude, int maxResults,
                                        GeocoderParams params, List<Address> addrs) {
		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Forward request: " + locationName);
		}
		List<Address> addresses = geocodeDatabase.get(locationName);
		if ((addresses != null) && !addresses.isEmpty()) {
			// database hit
			addrs.addAll(addresses);
			return null;
		}
		for (IGeocodeSource source : sources) {
			if (source.isSourceAvailable()) {
				try {
					addresses = source.getFromLocationName(locationName, lowerLeftLatitude, lowerLeftLongitude,
														   upperRightLatitude, upperRightLongitude,
														   params.getClientPackage(), params.getLocale());
				} catch (Throwable t) {
					Log.w(TAG, source.getName() + " throws exception!", t);
				}
				if ((addresses != null) && !addresses.isEmpty()) {
					geocodeDatabase.put(locationName, addresses);
					addrs.addAll(addresses);
					if (BaiduNetworkLocationService.DEBUG) {
						Log.d(TAG, locationName + " forward geolocated to:" + addrs.get(0));
					}
					return null; // null means everything is ok!
				}
			}
		}

		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Could not forward geolocate: " + locationName);
		}
		return UNKNOWN_RESULT_ERROR;
	}

	public void setSources(List<IGeocodeSource> sources) {
		this.sources = new ArrayList<IGeocodeSource>(sources);
	}

}
