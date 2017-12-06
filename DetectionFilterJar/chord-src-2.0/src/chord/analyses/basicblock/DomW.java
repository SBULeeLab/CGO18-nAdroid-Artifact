/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.basicblock;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Class.jq_Method;
import chord.project.Chord;

/**
 * Domain of loop head/exit basic blocks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "W"
)
public class DomW extends DomB {
	public int getOrAdd(BasicBlock b, jq_Method m) {
		basicBlockToMethodMap.put(b, m);
		return super.getOrAdd(b);
	}
}
