/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import java.util.Set;

import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

/**
 * Domain of abstract objects.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomO extends ProgramDom<CSObj> {
	private DomC domC;
	public String toXMLAttrsString(CSObj oVal) {
		if (domC == null)
			domC = (DomC) ClassicProject.g().getTrgt("C");
		Set<Ctxt> pts = oVal.pts;
		if (pts.size() == 0)
			return "";
		String s = "Cids=\"";
		for (Ctxt cVal : pts) {
			int cIdx = domC.indexOf(cVal);
			s += "C" + cIdx + " ";
		}
		s = s.substring(0, s.length() - 1);
		return s + "\"";
	}
}
