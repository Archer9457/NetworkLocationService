package com.baidu.location.networklocation.backends.file;

import android.os.Environment;
import android.util.Log;

import com.baidu.location.tyd.BaiduNetworkLocationService;
import com.baidu.location.networklocation.data.CellSpec;
import com.baidu.location.networklocation.data.LocationSpec;
import com.baidu.location.networklocation.source.ILocationSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewFileCellLocationSource implements ILocationSource<CellSpec> {
	private static final String TAG = "nlp.NewFileCellLocationSource";
	private static final String NAME = "Local File Database (lacells.db)";
	private static final String DESCRIPTION = "Read cell locations from a database located on the (virtual) sdcard";
	private static final String COPYRIGHT = "© unknown\nLicense: unknown";
	private final CellLocationFile FILE = new CellLocationFile(new File(Environment.getExternalStorageDirectory(), ".nogapps/lacells.db"));

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getCopyright() {
		return COPYRIGHT;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isSourceAvailable() {
		return FILE.exists();
	}

	@Override
	public Collection<LocationSpec<CellSpec>> retrieveLocation(Collection<CellSpec> specs) {
		List<LocationSpec<CellSpec>> locationSpecs = new ArrayList<LocationSpec<CellSpec>>();
		FILE.open();
		for (CellSpec spec : specs) {
			if (BaiduNetworkLocationService.DEBUG) {
				Log.i(TAG, "checking " + FILE.getPath() + " for " + spec);
			}
			LocationSpec<CellSpec> location = FILE.getLocation(spec);
			if (location != null) {
				locationSpecs.add(location);
			}
		}
		FILE.close();
		return locationSpecs;
	}
}
