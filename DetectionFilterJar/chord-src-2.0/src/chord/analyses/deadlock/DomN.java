/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.deadlock;

import joeq.Compiler.Quad.Inst;
import chord.project.ClassicProject;
import chord.project.Project;
import chord.project.analyses.ProgramDom;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.analyses.lock.DomL;
import chord.util.tuple.object.Pair;

/**
 * Domain of abstract threads.
 * <p>
 * An abstract thread is a triple <tt>(o,c,m)</tt> denoting the thread
 * whose abstract object is 'o' and which starts at method 'm' in
 * abstract context 'c'.
 *
 * @see chord.analyses.thread.ThreadsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomN extends ProgramDom<Pair<Ctxt, Inst>> {
	private DomC domC;
	private DomL domL;
	@Override
	public String toUniqueString(Pair<Ctxt, Inst> v) {
		return "<" + v.val0 + ", " + v.val1.toByteLocStr() + ">";
	}
	@Override
	public String toXMLAttrsString(Pair<Ctxt, Inst> v) {
		if (domC == null)
			domC = (DomC) ClassicProject.g().getTrgt("C");
		if (domL == null)
			domL = (DomL) ClassicProject.g().getTrgt("L");
		if (v == null)
			return "";
		int c = domC.indexOf(v.val0);
		int l = domL.indexOf(v.val1);
		return "Cid=\"C" + c + "\" Lid=\"L" + l + "\"";
	}
}
