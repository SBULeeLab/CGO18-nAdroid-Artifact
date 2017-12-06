/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.util.List;

import CnCHJ.api.TagCollection;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface ICtrlCollection extends TagCollection {
	public void setName(String name);

	public String getName();
	
	public void setPrescribedCollections(List<IStepCollection> c);
	
	public List<IStepCollection> getPrescribedCollections();
}
