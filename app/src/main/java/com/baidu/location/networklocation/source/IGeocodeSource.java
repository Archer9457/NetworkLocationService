package com.baidu.location.networklocation.source;

import android.location.Address;

import java.util.List;
import java.util.Locale;

public interface IGeocodeSource extends IDataSource {
	List<Address> getFromLocation(double latitude, double longitude, String sourcePackage, Locale locale);

	List<Address> getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude,
							 double upperRightLatitude, double upperRightLongitude, String sourcePackage, Locale locale);
}
