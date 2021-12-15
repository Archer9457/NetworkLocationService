package com.baidu.location.networklocation.retriever;

import com.baidu.location.networklocation.data.WifiSpec;

import java.util.Collection;

public interface IWifiSpecRetriever {
	Collection<WifiSpec> retrieveWifiSpecs();
}
