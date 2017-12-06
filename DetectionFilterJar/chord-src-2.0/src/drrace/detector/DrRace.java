package drrace.detector;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;

@Chord(
	name="drrace-java"
)

public class DrRace extends JavaAnalysis{

	@Override
	public void run() {
		/*
		 * run Inline Analysis for access$ methods
		 * run object-sensitive analysis
		 * run drrace-prologue-dlog
		 * run escaping analysis
		 * run lock analysis
		 */
		ClassicProject.g().runTask("drrace-prologue-java");
		
		/*
		 * run drrace detector
		 * run drrace sound filters
		 * run drrace unsound filters
		 * apply all the filters to generate the final output
		 */
		ClassicProject.g().runTask("drrace-detector-java");
		
		/*
		 * print result
		 * print classification result
		 * print classification of threads
		 * print Path
		 */
		ClassicProject.g().runTask("drrace-epilogue-java");
		
	}
}
