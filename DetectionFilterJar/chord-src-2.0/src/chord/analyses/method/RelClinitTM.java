/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.method;

import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Method;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,m) such that method m is
 * the class initializer method of class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "clinitTM",
	sign = "T0,M0:M0_T0"
)
public class RelClinitTM extends ProgramRel
		implements IMethodVisitor {
	private jq_Class ctnrClass;
	public void visit(jq_Class c) {
		ctnrClass = c;
	}
	public void visit(jq_Method m) {
		if (m instanceof jq_ClassInitializer) {
			add(ctnrClass, m);
		}
	}
}
