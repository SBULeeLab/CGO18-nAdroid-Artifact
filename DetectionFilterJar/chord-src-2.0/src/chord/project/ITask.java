/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

/**
 * Specification of an analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface ITask {
	/**
	 * Sets the name of this analysis.
	 * 
	 * @param	name	A name unique across all analyses included
	 * in a Chord project.
	 */
	public void setName(String name);
	/**
	 * Provides the name of this analysis.
	 * 
	 * @return	The name of this analysis.
	 */
	public String getName();
	/**
	 * Executes this analysis in a "classic" project.
	 * 
	 * This method must usually not be called directly.
	 * The correct way to call it is to call
	 * {@link chord.project.ClassicProject#runTask(String)} or
	 * {@link chord.project.ClassicProject#runTask(ITask)}, providing
	 * this analysis either by its name or its object.
	 */
	public void run();
	/**
	 * Executes this analysis in a "modern" project.
	 * 
	 * This method must usually not be called directly.
	 * 
	 * @param	ctrl
	 * @param	sc
	 */
	public void run(Object ctrl, IStepCollection sc);
}
