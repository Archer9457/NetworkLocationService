package com.baidu.location.networklocation.data;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.baidu.location.networklocation.database.LocationDatabase;
import com.baidu.location.networklocation.retriever.ICellSpecRetriever;
import com.baidu.location.networklocation.retriever.IWifiSpecRetriever;
import com.baidu.location.tyd.BaiduNetworkLocationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class LocationCalculator {
	public static final int MAX_WIFI_RADIUS = 500;
	private static final String TAG = "nlp.LocationCalculator";
	private final LocationDatabase locationDatabase;
	private final LocationRetriever locationRetriever;
	private final ICellSpecRetriever cellSpecRetriever;
	private final IWifiSpecRetriever wifiSpecRetriever;

	public LocationCalculator(LocationDatabase locationDatabase, LocationRetriever locationRetriever,
							  ICellSpecRetriever cellSpecRetriever, IWifiSpecRetriever wifiSpecRetriever) {
		this.locationDatabase = locationDatabase;
		this.locationRetriever = locationRetriever;
		this.cellSpecRetriever = cellSpecRetriever;
		this.wifiSpecRetriever = wifiSpecRetriever;
	}

	private static <T extends PropSpec> Collection<Collection<LocationSpec<T>>> divideInClasses(
			Collection<LocationSpec<T>> locationSpecs, double accuracy) {
		Collection<Collection<LocationSpec<T>>> classes = new ArrayList<Collection<LocationSpec<T>>>();
		for (LocationSpec<T> locationSpec : locationSpecs) {
			boolean used = false;
			for (Collection<LocationSpec<T>> locClass : classes) {
				if (locationCompatibleWithClass(locationSpec, locClass, accuracy)) {
					locClass.add(locationSpec);
					used = true;
				}
			}
			if (!used) {
				Collection<LocationSpec<T>> locClass = new ArrayList<LocationSpec<T>>();
				locClass.add(locationSpec);
				classes.add(locClass);
			}
		}
		return classes;
	}

	private static <T extends PropSpec> boolean locationCompatibleWithClass(LocationSpec<T> locationSpec,
																			Collection<LocationSpec<T>> locClass,
																			double accuracy) {
		for (LocationSpec<T> spec : locClass) {
			if ((locationSpec.distanceTo(spec) - locationSpec.getAccuracy() - spec.getAccuracy() -
				 accuracy) < 0) {
				return true;
			}
		}
		return false;
	}

	private static <T extends PropSpec> boolean locationCompatibleWithClass(Location location,
																			Collection<LocationSpec<T>> locClass) {
		for (LocationSpec<T> spec : locClass) {
			if (BaiduNetworkLocationService.DEBUG) {
				Log.d(TAG, "location: " + location + ", spec: " + spec + " => dist:" + spec.distanceTo(location) + "m");
			}
			if ((spec.distanceTo(location) - location.getAccuracy() - spec.getAccuracy()) < 0) {
				return true;
			}
		}
		return false;
	}

	private <T extends PropSpec> Location getAverageLocation(Collection<LocationSpec<T>> locationSpecs) {
		// TODO: This is a stupid way to do this, we could do better by using the signal strength and triangulation
		double latSum = 0, lonSum = 0, accSum = 0;
		for (LocationSpec<T> locationSpec : locationSpecs) {
			latSum += locationSpec.getLatitude();
			lonSum += locationSpec.getLongitude();
			accSum += locationSpec.getAccuracy();
		}

		Location location = new Location("network");
		location.setAccuracy((float) (accSum / locationSpecs.size()));
		location.setLatitude(latSum / locationSpecs.size());
		location.setLongitude(lonSum / locationSpecs.size());
		return location;
	}

	public Location getCurrentCellLocation() {
		Collection<LocationSpec<CellSpec>> cellLocationSpecs = getLocation(getCurrentCells());

		if ((cellLocationSpecs == null) || cellLocationSpecs.isEmpty()) {
			return null;
		}
		Location location = getAverageLocation(cellLocationSpecs);


		Bundle b = new Bundle();
		b.putString("networkLocationType", "cell");
		location.setExtras(b);
		return location;
	}

	private Collection<CellSpec> getCurrentCells() {
		return cellSpecRetriever.retrieveCellSpecs();
	}

	public Location getCurrentLocation() {
		Location cellLocation = getCurrentCellLocation();
		Location wifiLocation = getCurrentWifiLocation(cellLocation);
		if (wifiLocation != null) {
			return wifiLocation;
		}
		return cellLocation;
	}

	public Location getCurrentWifiLocation(Location cellLocation) {
		Collection<LocationSpec<WifiSpec>> wifiLocationSpecs = getLocation(getCurrentWifis());

		if (wifiLocationSpecs.isEmpty() || ((cellLocation == null) && (wifiLocationSpecs.size() < 2))) {
			return null;
		}

		Location location = null;
		if (cellLocation == null) {
			List<Collection<LocationSpec<WifiSpec>>> classes = new ArrayList<Collection<LocationSpec<WifiSpec>>>(
					divideInClasses(wifiLocationSpecs, MAX_WIFI_RADIUS));
			Collections.sort(classes, CollectionSizeComparator.INSTANCE);
			location = getAverageLocation(classes.get(0));
		} else {
			List<Collection<LocationSpec<WifiSpec>>> classes = new ArrayList<Collection<LocationSpec<WifiSpec>>>(
					divideInClasses(wifiLocationSpecs, cellLocation.getAccuracy()));
			Collections.sort(classes, CollectionSizeComparator.INSTANCE);
			for (Collection<LocationSpec<WifiSpec>> locClass : classes) {
				if (BaiduNetworkLocationService.DEBUG) {
					Log.d(TAG, "Test location class with "+locClass.size()+" entries");
				}
				if (locationCompatibleWithClass(cellLocation, locClass)) {
					if (BaiduNetworkLocationService.DEBUG) {
						Log.d(TAG, "Location class matches, using its average");
					}
					location = getAverageLocation(locClass);
					break;
				}
			}
		}
		if (location != null) {
			Bundle b = new Bundle();
			b.putString("networkLocationType", "wifi");
			location.setExtras(b);
		}
		return location;
	}

	private Collection<WifiSpec> getCurrentWifis() {
		return wifiSpecRetriever.retrieveWifiSpecs();
	}

	private <T extends PropSpec> Collection<LocationSpec<T>> getLocation(Collection<T> specs) {
		Collection<LocationSpec<T>> locationSpecs = new HashSet<LocationSpec<T>>();
		for (T spec : specs) {
			LocationSpec<T> locationSpec = locationDatabase.get(spec);
			if (locationSpec == null) {
				locationRetriever.queueLocationRetrieval(spec);
			} else if (!locationSpec.isUndefined()){
				locationSpecs.add(locationSpec);
			}
		}
		return locationSpecs;
	}

	public static class CollectionSizeComparator implements Comparator<Collection<LocationSpec<WifiSpec>>> {
		public static CollectionSizeComparator INSTANCE = new CollectionSizeComparator();

		@Override
		public int compare(Collection<LocationSpec<WifiSpec>> left, Collection<LocationSpec<WifiSpec>> right) {
			return (left.size() < right.size()) ? -1 : ((left.size() > right.size()) ? 1 : 0);
		}
	}
}

