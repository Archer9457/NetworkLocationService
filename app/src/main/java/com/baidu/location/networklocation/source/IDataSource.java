package com.baidu.location.networklocation.source;

public interface IDataSource {
	String getName();
	String getDescription();
	String getCopyright();
	boolean isSourceAvailable();
}
