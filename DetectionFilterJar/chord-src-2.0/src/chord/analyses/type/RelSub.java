/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.type;

import joeq.Class.jq_Reference;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;

/**
 * Relation containing each tuple (s,t) such that type s is a
 * subtype of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "sub",
	sign = "T1,T0:T0_T1"
)
public class RelSub extends ProgramRel {
	public void fill() {
		Program program = Program.g();
		IndexSet<jq_Reference> classes = program.getClasses();
		for (jq_Reference t1 : classes) {
			for (jq_Reference t2 : classes) {
				if (t1.isSubtypeOf(t2)) {
					add(t1, t2);
				}
			}
		}
	}
}
