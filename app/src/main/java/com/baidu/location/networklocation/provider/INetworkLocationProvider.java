package com.baidu.location.networklocation.provider;

import android.location.Location;
import android.os.IBinder;

import com.baidu.location.networklocation.data.LocationCalculator;


public interface INetworkLocationProvider {
	String NETWORK_LOCATION_TYPE = "networkLocationType";

	void disable();

	void enable();

	IBinder getBinder();

	boolean isActive();

	void onLocationChanged(Location paramLocation);

	void setCalculator(LocationCalculator locationCalculator);
}
