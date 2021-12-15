package com.baidu.location.networklocation.backends.mozilla;

import android.content.Context;
import android.net.ConnectivityManager;

import com.baidu.location.networklocation.data.CellSpec;
import com.baidu.location.networklocation.data.LocationSpec;
import com.baidu.location.networklocation.source.ILocationSource;
import com.baidu.location.networklocation.source.OnlineDataSource;

import java.util.Collection;

public class IchnaeaCellLocationSource extends OnlineDataSource implements ILocationSource<CellSpec> {
	private static final String NAME = "Mozilla Location Service";
	private static final String DESCRIPTION = "Retrieve cell locations from Mozilla while online";
	private static final String COPYRIGHT = "© Mozilla\nLicense: unknown";

	private static final String SEARCH_URL = "https://location.services.mozilla.com/v1/search";

	public IchnaeaCellLocationSource(Context context) {
		super(context);
	}

	protected IchnaeaCellLocationSource(ConnectivityManager connectivityManager) {
		super(connectivityManager);
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
	public Collection<LocationSpec<CellSpec>> retrieveLocation(Collection<CellSpec> specs) {
		return null; //TODO: Implement
	}

	@Override
	public boolean isSourceAvailable() {
		return false; // TODO: until stuff is done
	}
}
