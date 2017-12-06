/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.analyses.method.DomM;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.bddbddb.Rel.PairIterable;

/**
 * Static analysis computing reachable abstract threads.
 * <p>
 * Domain A is the domain of reachable abstract threads.
 * The 0th element does not denote any abstract thread; it is a
 * placeholder for convenience.
 * The 1st element denotes the main thread.
 * The remaining elements denote threads explicitly created by
 * calling the <tt>java.lang.Thread.start()</tt> method; there is a
 * separate element for each abstract object to which the
 * <tt>this</tt> argument of that method may point, as dictated by
 * the points-to analysis used.
 * <p>
 * Relation threadAOCM contains each tuple (a,o,c,m) such that
 * abstract thread 'a' corresponds to abstract object 'o' and is
 * started at thread-root method 'm' in abstract context 'c'.
 * Thread-root method 'm' may be either:
 * - the main method, in which case 'o' and 'c' are both epsilon
 * (element 0 in domain C), or
 * - the <tt>java.lang.Thread.start()</tt> method, in which case
 * 'o' is a non-epsilon element in domain C of the form [..., h]
 * where 'h' is the site at which the thread is allocated, and 'c'
 * may be epsilon (if the call graph is built using 0-CFA) or it
 * may be a chain of possibly interspersed call/allocation sites
 * (if the call graph is built using k-CFA or k-object-sensitive
 * analysis or a combination of the two).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "threads-java",
	consumes = { "threadOC" },
	produces = { "A", "threadAOCM" },
	namesOfSigns = { "threadAOCM" },
	signs = { "A0,C0,C1,M0:A0_M0_C0xC1" },
	namesOfTypes = { "A" },
	types = { DomA.class }
)
public class ThreadsAnalysis extends JavaAnalysis {
	public void run() {
		Program program = Program.g();
		DomC domC = (DomC) ClassicProject.g().getTrgt("C");
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		DomA domA = (DomA) ClassicProject.g().getTrgt("A");
		domA.clear();
		domA.add(null);
		jq_Method mainMeth = program.getMainMethod();
		Ctxt epsilon = domC.get(0);
		domA.add(new Trio<Ctxt, Ctxt, jq_Method>(epsilon, epsilon, mainMeth));
		jq_Method threadStartMeth = program.getThreadStartMethod();
		if (threadStartMeth != null) {
			ProgramRel relThreadOC = (ProgramRel) ClassicProject.g().getTrgt("threadOC");
			relThreadOC.load();
			PairIterable<Ctxt, Ctxt> tuples = relThreadOC.getAry2ValTuples();
			for (Pair<Ctxt, Ctxt> p : tuples) {
				Ctxt o = p.val0;
				Ctxt c = p.val1;
				domA.add(new Trio<Ctxt, Ctxt, jq_Method>(o, c, threadStartMeth));
			}
			relThreadOC.close();
		}
		domA.save();
		ProgramRel relThreadAOCM = (ProgramRel) ClassicProject.g().getTrgt("threadAOCM");
		relThreadAOCM.zero();
		for (int a = 1; a < domA.size(); a++) {
			Trio<Ctxt, Ctxt, jq_Method> ocm = domA.get(a);
			int o = domC.indexOf(ocm.val0);
			int c = domC.indexOf(ocm.val1);
			int m = domM.indexOf(ocm.val2);
			relThreadAOCM.add(a, o, c, m);
		}
		relThreadAOCM.save();
	}
}
