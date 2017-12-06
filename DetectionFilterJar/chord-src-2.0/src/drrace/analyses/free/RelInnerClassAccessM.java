/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package drrace.analyses.free;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
This ProgramRel RelInnerClassAccessM is a set of access$.. methods.
These methods are used by inner class to access fields of outer class.
*/

@Chord(
	name = "innerClassAccessM",
	sign = "M0"
)
public class RelInnerClassAccessM extends ProgramRel implements IMethodVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isStatic() && 
				m.getName().toString().startsWith("access$"))
			add(m);
	}
}
