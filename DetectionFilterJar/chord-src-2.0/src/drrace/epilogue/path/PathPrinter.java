package drrace.epilogue.path;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import chord.analyses.alias.Ctxt;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramRel;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import drrace.epilogue.printer.RelPrinterInput;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This is a printer to print the path of the TCE pair
 * Give a set of TCE pairs, it can print the path in this format:
 * TCE pair <t1,c1,e1,t3,c3,e3>
 * Use:  c0,m0 -> ...path... -> t1,c1,m1,e1
 * Free: c2,m2 -> ...path... -> t3,c3,m3,e3
 * 
 * Two modes:
 * (1)  Thread Sensitive: 
 * 		m0 and m2 are entry methods of t1 and t3, separately.
 * 
 * (2) 	Thread Oblivious: 
 * 		m0 and m2 are entry methods of dummy main method.
 * 		m0 and m2 should be entry methods of the same component (m0 != m2)
 * 		or m0 = m2
 * 
 * 
 * How to use:
 * 
 * PathPrinter mPathPrinter = new PathPrinter();
 * mPathPrinter.init("relationName1");
 * mPathPrinter.printThrSenPath();
 * mPathPrinter.printThrOblPath();
 * ...
 * mPathPrinter.init("relationNameN");
 * mPathPrinter.printThrSenPath();
 * mPathPrinter.printThrOblPath();
 * 
 */

public class PathPrinter {
	// relation name for TCE pairs
	private String relName;
	
	// directory for printing
	private final String printDir = "Use_after_Free_Races_Pathes";
	// directory for thrSenAbbrCSCGAnalysis output
	private String ThrSen_Dir;
	// directory for thrOblAbbrCSCGAnalysis output
	private String ThrObl_Dir;
	
	private final MyPathVisitor thrSenvisitor;
	private final MyPathVisitor thrOblvisitor;
	
	// relation for dlog input
	private final RelPrinterInput relPrinterInput;
	// dlog output file
	private final String printer_path_output_dlog = "drrace-printer-path-output-dlog";
	
	// These are two maps to cache ShortestPathBuilder
	final Map<Pair<Ctxt, jq_Method>, ShortestPathBuilder<Pair<Ctxt, jq_Method>>> SPBMapThrSen =
			new HashMap<Pair<Ctxt, jq_Method>, ShortestPathBuilder<Pair<Ctxt, jq_Method>>>();
	final Map<Pair<Ctxt, jq_Method>, ShortestPathBuilder<Pair<Ctxt, jq_Method>>> SPBMapThrObl =
					new HashMap<Pair<Ctxt, jq_Method>, ShortestPathBuilder<Pair<Ctxt, jq_Method>>>();
	
	// map: field -> Set<TCE_TCE>
	private final Map<jq_Field, 
				Set<Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
				         Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>>> f_TCE_TCE;
	
	private final Map<Pair<Trio<Ctxt, Ctxt, jq_Method>, Trio<Ctxt, Ctxt, jq_Method>>, 
						Set<Pair<Trio<Ctxt, Ctxt, jq_Method>, Trio<Ctxt, Ctxt, jq_Method>>>> TT_TT;
	
	public PathPrinter(){
		new File(Config.outDirName, printDir).mkdir();
		
		f_TCE_TCE = new HashMap<jq_Field, 
				Set<Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
						 Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>>>();
		
		TT_TT = new HashMap<Pair<Trio<Ctxt, Ctxt, jq_Method>, Trio<Ctxt, Ctxt, jq_Method>>, 
							Set<Pair<Trio<Ctxt, Ctxt, jq_Method>, Trio<Ctxt, Ctxt, jq_Method>>>>();
		
		ThrSenAbbrCSCGGraphAnalysis thrSenAbbrCSCGGraphAnalysis = (ThrSenAbbrCSCGGraphAnalysis)
				ClassicProject.g().getTrgt("thrsen-abbr-cscg-graph-java");
		ClassicProject.g().runTask(thrSenAbbrCSCGGraphAnalysis);
		thrSenvisitor = new MyPathVisitor(thrSenAbbrCSCGGraphAnalysis.getCallGraph());
		
		ThrOblAbbrCSCGGraphAnalysis thrOblAbbrCSCGGraphAnalysis = (ThrOblAbbrCSCGGraphAnalysis)
				ClassicProject.g().getTrgt("throbl-abbr-cscg-graph-java");
		ClassicProject.g().runTask(thrOblAbbrCSCGGraphAnalysis);
		thrOblvisitor = new MyPathVisitor(thrOblAbbrCSCGGraphAnalysis.getCallGraph());
		
		relPrinterInput = (RelPrinterInput) ClassicProject.g().getTrgt("printerInput");
		
	}
	
	// This print method is used for printing a single relation
	public void init(String relName){
		this.relName = relName;
		
		init();
		analyzeRel();
	}
	
	private void init(){
		new File(Config.outDirName, printDir + "/" + relName).mkdir();
		
		ThrSen_Dir = printDir + "/" + relName + "/ThrSenPath";
		ThrObl_Dir =   printDir + "/" + relName  + "/ThrOblPath";
		
		new File(Config.outDirName, ThrSen_Dir).mkdir();
		new File(Config.outDirName, ThrObl_Dir).mkdir();
		
		f_TCE_TCE.clear();
		
		relPrinterInput.clearSet();
		ClassicProject.g().resetTaskDone(relPrinterInput);
		ClassicProject.g().resetTaskDone(printer_path_output_dlog);
	}
	
	// load the TCE pairs to store them into f_TCE_TCE and relPrinterInput
	private void analyzeRel(){
		final ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt(relName);
		rel.load();
		final Iterable<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
							Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> tuples = rel.getAry6ValTuples();
		for(Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				 Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tuple : tuples){
			Trio<Ctxt, Ctxt, jq_Method> t1 = tuple.val0;
			Ctxt c1 = tuple.val1;
			Quad e1 = tuple.val2;
			jq_Field f1 = e1.getField();
			
			Trio<Ctxt, Ctxt, jq_Method> t2 = tuple.val3;
			Ctxt c2 = tuple.val4;
			Quad e2 = tuple.val5;
			jq_Field f2 = e1.getField();
			
			assert(f1 == f2);
			
			if(!f_TCE_TCE.containsKey(f1))
				f_TCE_TCE.put(f1, new HashSet<Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
										 		   Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>>());
			f_TCE_TCE.get(f1).add(new Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
								 		   Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>(t1,c1,e1,t2,c2,e2));
			
			relPrinterInput.addToSet(tuple);;
		}
		rel.close();
		
		ClassicProject.g().runTask(relPrinterInput);
		ClassicProject.g().runTask(printer_path_output_dlog);
		generate_TT_TT();
	}
	
	/*
	 * using dlog to generate TT_TT
	 * 
	 * output_TT_TT(t0,t1,t2,t3)
	 * t0 -> t1: thread of entry method -> thread of method containing Use statement
	 * t2 -> t3: thread of entry method -> thread of method containing Free statement
	 * t0 and t2 should be entry threads of the same component (t0 != t2)
	 * or t0 = t2
	 * 
	 * TT_TT -> map<tailTPair(t1,t3), Set<headTPair(t0,t2)>>
	 * 
	 */
	 
	private void generate_TT_TT(){
		final ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("output_TT_TT");
		rel.load();
		Iterable<chord.util.tuple.object.Quad<
					Trio<Ctxt, Ctxt, jq_Method>, 
					Trio<Ctxt, Ctxt, jq_Method>, 
					Trio<Ctxt, Ctxt, jq_Method>, 
					Trio<Ctxt, Ctxt, jq_Method> >> tuples = rel.getAry4ValTuples();
	 	for(chord.util.tuple.object.Quad<
				Trio<Ctxt, Ctxt, jq_Method>, 
				Trio<Ctxt, Ctxt, jq_Method>, 
				Trio<Ctxt, Ctxt, jq_Method>, 
				Trio<Ctxt, Ctxt, jq_Method> > tuple : tuples){
	 		Trio<Ctxt, Ctxt, jq_Method> t0 = tuple.val0;
	 		Trio<Ctxt, Ctxt, jq_Method> t1 = tuple.val1;
	 		Trio<Ctxt, Ctxt, jq_Method> t2 = tuple.val2;
	 		Trio<Ctxt, Ctxt, jq_Method> t3 = tuple.val3;
			
	 		Pair<Trio<Ctxt, Ctxt, jq_Method>, 
	 				Trio<Ctxt, Ctxt, jq_Method>> headTPair = new Pair<Trio<Ctxt, Ctxt, jq_Method>, 
	 		 															Trio<Ctxt, Ctxt, jq_Method>>(t0,t2);
			Pair<Trio<Ctxt, Ctxt, jq_Method>, 
	 				Trio<Ctxt, Ctxt, jq_Method>> tailTPair = new Pair<Trio<Ctxt, Ctxt, jq_Method>, 
	 		 															Trio<Ctxt, Ctxt, jq_Method>>(t1,t3);
			if(!TT_TT.containsKey(tailTPair))
				TT_TT.put(tailTPair, 
						    new HashSet<Pair<Trio<Ctxt, Ctxt, jq_Method>, Trio<Ctxt, Ctxt, jq_Method>>>());
				
			TT_TT.get(tailTPair).add(headTPair);
		}
	 	rel.close();
	}
	
	private final boolean ThrSen = true;
	private final boolean ThrObl = false;
	
	// print result for thread sensitive path
	public void printThrSenPath(){
		printPath(ThrSen);
	}
	
	// print result for thread oblivious path 
	public void printThrOblPath(){
		printPath(ThrObl);
	}
	
	/*
	 * flag is used for selecting mode (thread sensitive or thread oblivious)
	 * 
	 * Difference:
	 * (1) print directory
	 * (2) call graph
	 * (3) source pair<>context, method>
	 * 
	 */
	private void printPath(boolean flag){
		String dir;
		if(flag == ThrSen){
			dir = ThrSen_Dir;
		}else{
			assert(flag == ThrObl);
			dir = ThrObl_Dir;
		}
		
		new File(Config.outDirName, dir).mkdir();
		
		int fieldCounter = 0;
		for(Entry<jq_Field, 
				  Set<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad, 
				  		   Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>>> entry : f_TCE_TCE.entrySet()){
			jq_Field mF = entry.getKey();
			fieldCounter = fieldCounter + 1;
			
			PrintWriter out = OutDirUtils.newPrintWriter(dir + "/" + fieldCounter + ": " + mF.getName());
			out.println("Field No." + fieldCounter + ": " + mF);
			
			int counterPerField = 0;
			Set<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad, 
					 Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> TCE_TCE_Set = entry.getValue();
			for(Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad, 
					 Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> TCE_TCE : TCE_TCE_Set){
				Trio<Ctxt, Ctxt, jq_Method> t1 = TCE_TCE.val0;
				Ctxt c1 = TCE_TCE.val1;
				Quad e1 = TCE_TCE.val2;
				jq_Method m1 = e1.getMethod();
				
				Trio<Ctxt, Ctxt, jq_Method> t2 = TCE_TCE.val3;
				Ctxt c2 = TCE_TCE.val4;
				Quad e2 = TCE_TCE.val5;
				jq_Method m2 = e2.getMethod();
				
				counterPerField = counterPerField + 1;
				
				out.println("");
				out.println("Datarace No." + counterPerField);
				
				out.println("t1= " + t1);
				out.println("c1= " + c1);
				out.println("m1= " + m1);
				out.println("e1= " + e1);
				
				out.println("|||");
				
				out.println("t2= " + t2);
				out.println("c2= " + c2);
				out.println("m2= " + m2);
				out.println("e2= " + e2);
				
				out.println("");
				
				System.out.println("t1= " + t1);
				System.out.println("c1= " + c1);
				System.out.println("m1= " + m1);
				System.out.println("e1= " + e1);
				
				System.out.println("t2= " + t2);
				System.out.println("c2= " + c2);
				System.out.println("m2= " + m2);
				System.out.println("e2= " + e2);
				
				
				Pair<Ctxt, jq_Method> dstCM1 = new Pair<Ctxt, jq_Method>(c1, m1);
				Pair<Ctxt, jq_Method> dstCM2 = new Pair<Ctxt, jq_Method>(c2, m2);
				
				Pair<Trio<Ctxt, Ctxt, jq_Method>, 
						Trio<Ctxt, Ctxt, jq_Method>> tailTPair = new Pair<Trio<Ctxt, Ctxt, jq_Method>, 
	 																		Trio<Ctxt, Ctxt, jq_Method>>(t1,t2);
				Set<Pair<Trio<Ctxt, Ctxt, jq_Method>, 
							Trio<Ctxt, Ctxt, jq_Method>>> headTTPairSet = TT_TT.get(tailTPair);
				
				assert(headTTPairSet != null);
				assert(!headTTPairSet.isEmpty());
				
				if(flag == ThrSen){
					out.println("Path:");
					
					Pair<Ctxt, jq_Method> srcCM1 = new Pair<Ctxt, jq_Method>(t1.val1, t1.val2);
					out.println("Use:");
					out.println(getPath(srcCM1, dstCM1, thrSenvisitor));
					
					out.println();
					
					Pair<Ctxt, jq_Method> srcCM2 = new Pair<Ctxt, jq_Method>(t2.val1, t2.val2);
					out.println("Free:");
					out.println(getPath(srcCM2, dstCM2, thrSenvisitor));
					
					out.println();
				}else{
					assert(flag == ThrObl);
					
					int counter = 0;
					for(Pair<Trio<Ctxt, Ctxt, jq_Method>, 
								Trio<Ctxt, Ctxt, jq_Method>> headTTPair : headTTPairSet){
						counter = counter + 1;
						
						out.println("Path No. " + counter + " for Datarace No." + counterPerField);
						
						Trio<Ctxt, Ctxt, jq_Method> entryT_t1 = headTTPair.val0;
						Pair<Ctxt, jq_Method> srcCM1 = new Pair<Ctxt, jq_Method>(entryT_t1.val1, entryT_t1.val2);
						Pair<Ctxt, jq_Method> srcCM1_t1 = new Pair<Ctxt, jq_Method>(t1.val1, t1.val2);
						out.println("Use:");
						String thrOblPath1 = getPath(srcCM1, srcCM1_t1, thrOblvisitor);
						if(thrOblPath1 != "")
							out.println(thrOblPath1 + "\n|");
						out.println(getPath(srcCM1_t1, dstCM1, thrSenvisitor));
						
						out.println();
						
						Trio<Ctxt, Ctxt, jq_Method> entryT_t2 = headTTPair.val1;
						Pair<Ctxt, jq_Method> srcCM2 = new Pair<Ctxt, jq_Method>(entryT_t2.val1, entryT_t2.val2);
						Pair<Ctxt, jq_Method> srcCM2_t2 = new Pair<Ctxt, jq_Method>(t2.val1, t2.val2);
						out.println("Free:");
						String thrOblPath2 = getPath(srcCM2, srcCM2_t2, thrOblvisitor);
						if(thrOblPath2 != "")
							out.println(thrOblPath2 + "\n|");
						out.println(getPath(srcCM2_t2, dstCM2, thrSenvisitor));
						
						out.println("Path No. " + counter + " for Datarace No." + counterPerField + " Ends");
						out.println();
					}
				}
				out.println("Datarace No." + counterPerField + " Ends");
			}
			out.close();
		}
	}
	
	// getPath from srcCM to dstCM using visitor
	private String getPath(
			Pair<Ctxt, jq_Method> srcCM,
			Pair<Ctxt, jq_Method> dstCM,
			MyPathVisitor visitor){
		
		ShortestPathBuilder<Pair<Ctxt, jq_Method>> spb = null;
		if(visitor == thrSenvisitor){
			spb = SPBMapThrSen.get(srcCM);
		}else{
			assert(visitor == thrOblvisitor);
			spb = SPBMapThrObl.get(srcCM);
		}
			
		if (spb == null){
			spb = new ShortestPathBuilder<Pair<Ctxt, jq_Method>>(visitor.getCSCG(), srcCM, visitor);
			
			if(visitor == thrSenvisitor){
				SPBMapThrSen.put(srcCM, spb);
			}else{
				assert(visitor == thrOblvisitor);
				SPBMapThrObl.put(srcCM, spb);
			}
		} else {
			System.out.println("Here is to test ShortestPathBuilder Map");
		}
		
		String path = spb.getShortestPathTo(dstCM);
		if(path.length() >= 3)
			path = path.substring(0, path.length()-3);
		return path;
	}
	
	// MyPathVisitor implements IPathVisitor
	private class MyPathVisitor implements IPathVisitor<Pair<Ctxt, jq_Method>>{
		
		// context-sensitive call graph
		// can be thread-sensitive or thread-oblivious
		private CSCG mCSCG;
		
		MyPathVisitor(CSCG mCSCG){
			this.mCSCG = mCSCG;
		}
		
		/*
		 * Method called while visiting each edge in a path of a
		 * directed graph.
		 * 
		 * @param	origNode	The source node of the visited edge.
		 * @param	destNode	The target node of the visited edge.
		 * 
		 * @return	A string-valued result of visiting the edge.
		 */
		@Override
		public String visit(Pair<Ctxt, jq_Method> origNode, Pair<Ctxt, jq_Method> destNode) {
			jq_Method srcM = origNode.val1;
			Ctxt srcC = origNode.val0;
			
			String path = "";
			path += "c = " + srcC + "\n";
			path += "m = " + srcM + "\n";
			path += "|\n";
			
			return path;
		}
		
		public CSCG getCSCG(){
			return mCSCG;
		}
		
	}
}
