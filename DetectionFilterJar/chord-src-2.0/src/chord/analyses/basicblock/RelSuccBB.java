/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.basicblock;

import java.util.List;

import joeq.Compiler.Quad.BasicBlock;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.analyses.basicblock.DomB;

/**
 * Relation containing each pair of basic blocks (b1,b2) in each method
 * such that b1 is immediate postdominator of b2.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "succBB",
	sign = "B0,B1:B0xB1"
)
public class RelSuccBB extends ProgramRel {
	public void fill() {
		DomB domB = (DomB) doms[0];
		int numB = domB.size();
		for (int bIdx = 0; bIdx < numB; bIdx++) {
			BasicBlock bb = domB.get(bIdx);
			List<BasicBlock> succs = bb.getSuccessorsList();
			for (BasicBlock bb2 : succs) {
				int bIdx2 = domB.indexOf(bb2);
				assert (bIdx2 >= 0);
				add(bIdx, bIdx2);
			}
		}
	}
}
