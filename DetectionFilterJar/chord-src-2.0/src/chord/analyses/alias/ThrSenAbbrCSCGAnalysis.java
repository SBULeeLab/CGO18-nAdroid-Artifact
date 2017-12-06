/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-sensitive, abbreviated,
 * context-sensitive call graph of the program.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thrsen-abbr-cscg-java",
	consumes = { "thrSenAbbrCICM", "thrSenAbbrCMCM",
		"thrSenAbbrRootCM", "thrSenAbbrReachableCM" }
)
public class ThrSenAbbrCSCGAnalysis extends CSCGAnalysis {
	public void run() {
		domM = (DomM) ClassicProject.g().getTrgt("M");
		relCICM = (ProgramRel) ClassicProject.g().getTrgt("thrSenAbbrCICM");
		relCMCM = (ProgramRel) ClassicProject.g().getTrgt("thrSenAbbrCMCM");
		relRootCM = (ProgramRel) ClassicProject.g().getTrgt("thrSenAbbrRootCM");
		relReachableCM = (ProgramRel)
			ClassicProject.g().getTrgt("thrSenAbbrReachableCM");
	}
}
