package com.baidu.location.networklocation.platform;

import android.content.Context;

import com.baidu.location.networklocation.provider.INetworkLocationProvider;
import com.baidu.location.networklocation.retriever.ICellSpecRetriever;
import com.baidu.location.networklocation.retriever.IWifiSpecRetriever;

public class PlatformFactory {
	private PlatformFactory() {

	}

	public static INetworkLocationProvider newNetworkLocationProvider() {
			return new NetworkLocationProviderV2();
	}

	public static ICellSpecRetriever newCellSpecRetriever(Context context) {
		return new CellSpecRetriever(context);
	}

	public static IWifiSpecRetriever newWifiSpecRetriever(Context context) {
		return new WifiSpecRetriever(context);
	}

	public static GeocodeProvider newGeocodeProvider() {
		return new GeocodeProvider();
	}
}
