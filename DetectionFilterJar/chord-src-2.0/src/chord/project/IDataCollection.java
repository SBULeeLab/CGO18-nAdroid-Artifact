/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.util.List;

import CnCHJ.api.ItemCollection;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IDataCollection {
	public void setName(String name);

	public String getName();
	
	public void setItemCollection(ItemCollection ic);
	
	public ItemCollection getItemCollection();

	public void setProducingCollections(List<IStepCollection> ic);

	public List<IStepCollection> getProducingCollections();
}
