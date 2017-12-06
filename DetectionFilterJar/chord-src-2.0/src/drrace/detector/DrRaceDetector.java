package drrace.detector;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;

@Chord(
	name="drrace-detector-java"
)

/*
 * run drrace detector
 * run drrace sound filters
 * run drrace unsound filters
 * apply all the filters to generate the final output
 */

public class DrRaceDetector extends JavaAnalysis{

	@Override
	public void run() {
		//run drrace detector
		ClassicProject.g().runTask("drrace-detector-dlog");
		
		//run drrace sound filters
		ClassicProject.g().runTask("drrace-filters-sound-dlog");
		
		//run drrace unsound filters
		ClassicProject.g().runTask("drrace-filters-unsound-dlog");
		
		//apply all the filters to generate the final output
		ClassicProject.g().runTask("drrace-filters-applying-dlog");
	}
}
