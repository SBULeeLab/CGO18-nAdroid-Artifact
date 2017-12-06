package drrace.detector;

import chord.analyses.alias.CtxtsAnalysis;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;

@Chord(
	name="drrace-prologue-java"
)

/*
 * run Inline Analysis for access$ methods
 * run object-sensitive analysis
 * run drrace-prologue-dlog
 * run escaping analysis
 * run lock analysis
 */

public class DrRacePrologue extends JavaAnalysis{

	@Override
	public void run() {
		//run Inline Analysis for access$ methods
		ClassicProject.g().runTask("drrace-inline");
		
		//run object-sensitive analysis
		ClassicProject.g().runTask("ctxts-java");
		ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
		
		//prepare for drrace detector
		ClassicProject.g().runTask("drrace-prologue-dlog");
		
		//run escaping analysis
		//determine whether the register is local or can be shared
		ClassicProject.g().runTask("datarace-escaping-include-dlog");
		
		//run lock analysis
		ClassicProject.g().runTask("datarace-nongrded-include-dlog");
	}
}
