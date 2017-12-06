/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.invk;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing all method invocation statements whose
 * target is an instance method.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "instI",
	sign = "I0"
)
public class RelInstI extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			jq_Method m = Invoke.getMethod(q).getMethod();
			if (!m.isStatic())
				add(iIdx);
		}
	}
}
