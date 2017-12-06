/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-oblivious, abbreviated,
 * context-sensitive call graph of the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "throbl-abbr-cscg-java",
	consumes = { "thrOblAbbrCICM", "thrOblAbbrCMCM",
		"thrOblAbbrRootCM", "thrOblAbbrReachableCM" }
)
public class ThrOblAbbrCSCGAnalysis extends CSCGAnalysis {
	public void run() {
		domM = (DomM) ClassicProject.g().getTrgt("M");
		relCICM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrCICM");
		relCMCM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrCMCM");
		relRootCM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrRootCM");
		relReachableCM = (ProgramRel)
			ClassicProject.g().getTrgt("thrOblAbbrReachableCM");
	}
}
