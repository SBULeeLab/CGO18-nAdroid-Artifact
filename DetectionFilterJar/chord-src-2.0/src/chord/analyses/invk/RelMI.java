/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.invk;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,i) such that method m contains
 * method invocation statement i.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MI",
	sign = "M0,I0:I0xM0"
)
public class RelMI extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomI domI = (DomI) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			jq_Method m = q.getMethod();
			int mIdx = domM.indexOf(m);
			assert (mIdx >= 0);
			add(mIdx, iIdx);
		}
	}
}
