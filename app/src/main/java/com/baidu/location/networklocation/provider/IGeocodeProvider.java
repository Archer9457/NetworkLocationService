package com.baidu.location.networklocation.provider;

import android.os.IBinder;

import com.baidu.location.networklocation.database.GeocodeDatabase;
import com.baidu.location.networklocation.source.IGeocodeSource;

import java.util.List;

public interface IGeocodeProvider {
	IBinder getBinder();

	void setGeocodeDatabase(GeocodeDatabase geocodeDatabase);

	void setSources(List<IGeocodeSource> geocodeSources);
}
