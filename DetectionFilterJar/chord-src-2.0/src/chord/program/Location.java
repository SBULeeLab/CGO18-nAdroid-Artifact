/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;

/**
 * Representation of the location of a statement.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Location {
	// q == null iff qIdx == -1 iff empty bb
	public final jq_Method m;
	public final BasicBlock bb;
	public final int qIdx;
	public final Quad q;
	public Location(jq_Method m, BasicBlock bb, int qIdx, Quad q) {
		this.m = m;
		this.bb = bb;
		this.qIdx = qIdx;
		this.q = q;
	}
	public int hashCode() {
		return (q != null) ? q.hashCode() : bb.hashCode();
	}
	public boolean equals(Object o) {
		if (!(o instanceof Location))
			return false;
		Location l = (Location) o;
		return (q != null) ? (q == l.q) : (bb == l.bb);
	}
	public String toString() {
		return "<" + m + ", " + q + ">";
	}
}

