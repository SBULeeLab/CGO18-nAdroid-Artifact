/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.visitors;

import joeq.Class.jq_Field;

/**
 * Visitor over all fields of all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IFieldVisitor extends IClassVisitor {
	/**
	 * Visits all fields of all classes in the program.
	 * 
	 * @param	f	A field.
	 */
	public void visit(jq_Field f);
}
