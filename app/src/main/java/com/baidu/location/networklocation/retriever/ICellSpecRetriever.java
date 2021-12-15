package com.baidu.location.networklocation.retriever;

import com.baidu.location.networklocation.data.CellSpec;
import java.util.Collection;

public interface ICellSpecRetriever {
	Collection<CellSpec> retrieveCellSpecs();
}
