/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.visitors;

import joeq.Class.jq_Class;

/**
 * Visitor over all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IClassVisitor {
	/**
	 * Visits all classes in the program.
	 *
	 * @param	c	A class.
	 */
	public void visit(jq_Class c);
}
