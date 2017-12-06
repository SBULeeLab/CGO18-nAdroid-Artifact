package drrace.epilogue.path;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/*
 * Call graph analysis producing a thread-oblivious, abbreviated,
 * context-sensitive call graph of the program.
 * 
 * Using CSCG implemented by using Map (not using bdd file)
 */
@Chord(
	name = "throbl-abbr-cscg-graph-java"
)
public class ThrOblAbbrCSCGGraphAnalysis extends JavaAnalysis {
	protected ProgramRel relCMCM;
	protected ProgramRel relRootCM;
	protected ProgramRel relReachableCM;
	protected CSCG callGraph;
	
	@Override
	public void run() {
		relCMCM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrCMCM");
		relRootCM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrRootCM");
		relReachableCM = (ProgramRel) ClassicProject.g().getTrgt("thrOblAbbrReachableCM");
	}
	/*
	 * Provides the program's context-sensitive call graph.
	 * 
	 * @return	The program's context-sensitive call graph.
	 */
	public CSCG getCallGraph() {
		if (callGraph == null) {
			callGraph = new CSCG(relRootCM, relReachableCM, relCMCM);
		}
		return callGraph;
	}
}
