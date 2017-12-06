/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.Project;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-oblivious, abbreviated,
 * context-insensitive call graph of the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "throbl-abbr-cicg-java",
	consumes = { "thrOblAbbrIM", "thrOblAbbrMM",
		"thrOblAbbrRootM", "thrOblAbbrReachableM" }
)
public class ThrOblAbbrCICGAnalysis extends CICGAnalysis {
	public void run() {
		domM = (DomM) ClassicProject.g().getTrgt("M");
		relIM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrIM");
		relMM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrMM");
		relRootM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrRootM");
		relReachableM = (ProgramRel)
			ClassicProject.g().getTrgt("thrOblAbbrReachableM");
	}
}
