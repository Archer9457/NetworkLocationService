package com.baidu.location.networklocation.platform;

import android.content.Context;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;


import com.baidu.location.networklocation.data.CellSpec;
import com.baidu.location.networklocation.data.Radio;
import com.baidu.location.networklocation.retriever.ICellSpecRetriever;
import com.baidu.location.tyd.BaiduNetworkLocationService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class CellSpecRetriever implements ICellSpecRetriever {
	private static final String TAG = "nlp.CellSpecRetriever";
	private TelephonyManager telephonyManager;

	public CellSpecRetriever(TelephonyManager telephonyManager) {
		this.telephonyManager = telephonyManager;
	}

	public CellSpecRetriever(Context context) {
		this((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
	}

	private void addCellSpecFromCellLocation(Collection<CellSpec> cellSpecs, int mcc, int mnc,
											 CellLocation cellLocation) {
		if (cellLocation instanceof GsmCellLocation) {
			GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
			int cid = gsmCellLocation.getCid();
			int lac = gsmCellLocation.getLac();
			int psc = gsmCellLocation.getPsc();
			if (psc == -1) {
				cellSpecs.add(new CellSpec(Radio.GSM, mcc, mnc, lac, cid));
			} else if ((cid != -1) && (lac != -1)) {
				CellSpec cellSpec = new CellSpec(Radio.UMTS, mcc, mnc, lac, cid);
				cellSpec.setPsc(psc);
				cellSpecs.add(cellSpec);
			}
		} else if (cellLocation instanceof CdmaCellLocation) {
			CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
			int sid = cdmaCellLocation.getSystemId(); // as mnc
			int nid = cdmaCellLocation.getNetworkId(); // as lac
			int bid = cdmaCellLocation.getBaseStationId(); // as cid
			cellSpecs.add(new CellSpec(Radio.CDMA, mcc, sid, nid, bid));
		} else {
			if (BaiduNetworkLocationService.DEBUG) {
				Log.d(TAG, "Not connected to network or using LTE, which is not supported for SDK <= 16");
			}
		}
	}

	private void addCellSpecsFromNeighboringCellInfo(Collection<CellSpec> cellSpecs, int mcc, int mnc,
													 List<NeighboringCellInfo> neighboringCellInfo) {
		if (neighboringCellInfo == null) {
			return;
		}
		for (NeighboringCellInfo cellInfo : neighboringCellInfo) {
			int lac = cellInfo.getLac();
			int cid = cellInfo.getCid();
			int psc = cellInfo.getPsc();
			// TODO handle rssi: int rssi = cellInfo.getRssi();
			if ((lac != NeighboringCellInfo.UNKNOWN_CID) && (cid != NeighboringCellInfo.UNKNOWN_CID)) {

				if (psc != NeighboringCellInfo.UNKNOWN_CID) {
					CellSpec cellSpec = new CellSpec(Radio.UMTS, mcc, mnc, lac, cid);
					cellSpec.setPsc(psc);
					cellSpecs.add(cellSpec);
				} else {
					cellSpecs.add(new CellSpec(Radio.GSM, mcc, mnc, lac, cid));
				}

			}
		}

	}

	public Collection<CellSpec> retrieveCellSpecs() {
		Collection<CellSpec> cellSpecs = new ArrayList<CellSpec>();
		if (telephonyManager == null) {
			return cellSpecs;
		}
		CellLocation.requestLocationUpdate();
		String mncString = telephonyManager.getNetworkOperator();
		if ((mncString == null) || (mncString.length() < 5) || (mncString.length() > 6))
			return cellSpecs;
		int mcc = Integer.parseInt(mncString.substring(0,3));
		int mnc = Integer.parseInt(mncString.substring(3));

		//addCellSpecFromCellLocation(cellSpecs, mcc, mnc, telephonyManager.getCellLocation());
		//addCellSpecsFromNeighboringCellInfo(cellSpecs, mcc, mnc, telephonyManager.getNeighboringCellInfo());

		if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) && BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Android SDK > 16 features are not used yet (but would be good here!)");
		}


		if (BaiduNetworkLocationService.DEBUG) {
			Log.d(TAG, "Found "+cellSpecs.size()+" Cells");
		}

		return cellSpecs;
	}

}
