package drrace.epilogue.rel;

import java.io.PrintWriter;
import java.util.List;

import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramRel;

/*
 * This is a counter for relation size.
 */

public class RelCounter {
	/*
	 * relNameList is a list of relation name.
	 * outName is the name of the output file.
	 * This function prints the relation size of every relation in relNameList
	 * into the file named outName.
	 */
	public void printRelSize(List<String> relNameList, String outName){
		PrintWriter out = OutDirUtils.newPrintWriter(outName);
		for (String relName : relNameList) {
			final ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt(relName);
			assert(rel != null);
			
			rel.load();
			int relSize = rel.size();
			rel.close();
			
			out.println("size of " + relName + ": " + relSize);
		}
		out.close();
	}
}
