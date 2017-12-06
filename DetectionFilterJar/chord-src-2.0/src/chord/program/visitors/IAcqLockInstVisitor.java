/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.visitors;

import joeq.Compiler.Quad.Quad;

/**
 * Visitor over all monitorenter statements in all methods
 * in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IAcqLockInstVisitor extends IMethodVisitor {
	/**
	 * Visits all monitorenter statements in all methods
	 * in the program.
	 * 
	 * @param	q	A monitorenter statement.
	 */
	public void visitAcqLockInst(Quad q);
}
