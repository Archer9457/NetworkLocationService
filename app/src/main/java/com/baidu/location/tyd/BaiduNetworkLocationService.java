package com.baidu.location.tyd;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.baidu.location.LocationApplication;
import com.baidu.location.networklocation.backends.apple.AppleWifiLocationSource;
import com.baidu.location.networklocation.backends.cellapi.CellAPI;
import com.baidu.location.networklocation.backends.file.NewFileCellLocationSource;
import com.baidu.location.networklocation.backends.file.OldFileCellLocationSource;
import com.baidu.location.networklocation.backends.mozilla.IchnaeaCellLocationSource;
import com.baidu.location.networklocation.backends.opencellid.OpenCellIdLocationSource;
import com.baidu.location.networklocation.data.CellSpec;
import com.baidu.location.networklocation.data.LocationCalculator;
import com.baidu.location.networklocation.data.LocationRetriever;
import com.baidu.location.networklocation.data.WifiSpec;
import com.baidu.location.networklocation.database.LocationDatabase;
import com.baidu.location.networklocation.helper.Reflected;
import com.baidu.location.networklocation.platform.PlatformFactory;
import com.baidu.location.networklocation.provider.IGeocodeProvider;
import com.baidu.location.networklocation.provider.INetworkLocationProvider;
import com.baidu.location.networklocation.retriever.ICellSpecRetriever;
import com.baidu.location.networklocation.retriever.IWifiSpecRetriever;
import com.baidu.location.networklocation.source.ILocationSource;
import com.baidu.location.service.LocationService;

import java.util.ArrayList;
import java.util.List;

public class BaiduNetworkLocationService extends Service {
    public static final boolean DEBUG;

    static {
        DEBUG = Log.isLoggable("nlp", Log.DEBUG);
    }

    private static final String TAG = "nlp.NetworkLocationService";
    private static Context context;
    private LocationCalculator locationCalculator;
    private LocationRetriever locationRetriever;
    private INetworkLocationProvider nlprovider;
    private IGeocodeProvider geoprovider;
    private WifiManager wifiManager;
    private BroadcastReceiver airplaneModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateProviderStateOnAirplaneMode();
        }
    };
    private LocationService locationService;

    public BaiduNetworkLocationService() {
        if (DEBUG) {
            Log.d(TAG, "new Service-Object constructed");
        }
    }

    public static Context getContext() {
        return context;
    }

    public Location getCurrentLocation() {
        return locationCalculator.getCurrentLocation();
    }

    public boolean isActive() {
        return nlprovider.isActive();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "Incoming Bind Intent: " + intent);
        }
        if (intent == null) {
            return null;
        }
        String action = intent.getAction();
        if (action == null) {
            return null;
        }
        if (action.equalsIgnoreCase("com.google.android.location.NetworkLocationProvider") ||
                action.equalsIgnoreCase("com.android.location.service.NetworkLocationProvider") ||
                action.equalsIgnoreCase("com.android.location.service.v2.NetworkLocationProvider") ||
                action.equalsIgnoreCase("com.android.location.service.v3.NetworkLocationProvider")) {
            return nlprovider.getBinder();
        } else if (action.equalsIgnoreCase("com.google.android.location.GeocodeProvider") ||
                action.equalsIgnoreCase("com.android.location.service.GeocodeProvider")) {
            return geoprovider.getBinder();
        }else if (action.equalsIgnoreCase("com.google.android.location.internal.ANDROID_NLP")) {
            Log.w(TAG, "somebody wants internal stuff o.O");
            return nlprovider.getBinder();
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        if (DEBUG) {
            Log.d(TAG, "Creating Service");
        }

        locationService = ((LocationApplication) getApplication()).locationService;
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        nlprovider = PlatformFactory.newNetworkLocationProvider();

        IWifiSpecRetriever wifiSpecRetriever = PlatformFactory.newWifiSpecRetriever(context);
        ICellSpecRetriever cellSpecRetriever = PlatformFactory.newCellSpecRetriever(context);
        LocationDatabase locationDatabase = new LocationDatabase(context);
        locationRetriever = new LocationRetriever(locationDatabase);
        locationCalculator =
                new LocationCalculator(locationDatabase, locationRetriever, cellSpecRetriever, wifiSpecRetriever);
        nlprovider.setCalculator(locationCalculator);

        List<ILocationSource<WifiSpec>> wifiSources = new ArrayList<ILocationSource<WifiSpec>>();
        wifiSources.add(new AppleWifiLocationSource(context));
        locationRetriever.setWifiLocationSources(wifiSources);

        List<ILocationSource<CellSpec>> cellSources = new ArrayList<ILocationSource<CellSpec>>();
        cellSources.add(new NewFileCellLocationSource());
        cellSources.add(new OldFileCellLocationSource());
        cellSources.add(new OpenCellIdLocationSource(context));
        cellSources.add(new CellAPI(context));
        cellSources.add(new IchnaeaCellLocationSource(context));
        locationRetriever.setCellLocationSources(cellSources);

        locationRetriever.start();

        registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        updateProviderStateOnAirplaneMode();
    }


    public void updateProviderStateOnAirplaneMode() {
        boolean airplane = isAirplaneModeOn();
        boolean wifi = wifiManager.isWifiEnabled();
        if (DEBUG) {
            Log.d(TAG, "airplane:" + airplane + " | wifi:" + wifi);
        }
        if (airplane && !wifi) {
            if (DEBUG) {
                Log.d(TAG, "AirplaneMode is enabled and wifi is off, so no way to get location for us");
            }
            nlprovider.disable();
            locationService.stop();

        } else {
            if (DEBUG) {
                Log.d(TAG, "AirplaneMode or wifi is enabled. make sure we're active!");
            }
            nlprovider.enable();
            locationService.start();
        }
    }

    private boolean isAirplaneModeOn() {
        return ((Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) ?
                (Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0)) :
                (Reflected.androidProviderSettingsGlobalGetInt(getContentResolver(), "airplane_mode_on", 0))) != 0;
    }
}