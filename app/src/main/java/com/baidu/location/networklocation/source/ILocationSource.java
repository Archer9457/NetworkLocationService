package com.baidu.location.networklocation.source;

import com.baidu.location.networklocation.data.LocationSpec;
import com.baidu.location.networklocation.data.PropSpec;

import java.util.Collection;

public interface ILocationSource<T extends PropSpec> extends IDataSource {
	Collection<LocationSpec<T>> retrieveLocation(Collection<T> specs);
}
