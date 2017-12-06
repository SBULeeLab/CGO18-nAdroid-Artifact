package drrace.epilogue.printer;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import chord.analyses.alias.Ctxt;
import chord.bddbddb.Rel.PentIterable;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Trio;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This class is used for print result of a relation.
 * The relation should be in this format:
 * (t1,c1,e1,t2,c2,e2)
 * Three kinds of result will be printed (all grouped by fileds):
 * (1) F_TCME_TCME 
 * (2) F_MM_MM (entry method->method freeing or using)
 * (3) F_T_T
 * 
 * The number of the result will also be printed.
 * 
 * How to use:
 * 
 * DrRacePrinter mDrRacePrinter = new DrRacePrinter("printDir");
 * mDrRacePrinter.print("relationName1");
 * ...
 * mDrRacePrinter.print("relationNameN");
 * 
 * String[] relNameArray = {
 * 		"relationName1",
 * 		...
 * 		"relationNameN"
 * };
 * List<String> relNameList = Arrays.asList(relNameArray);
 * mDrRacePrinter_dlog.print(relNameList);
 * 
 */

public class DrRacePrinter_dlog {
	private final String printDir;
	
	private String TCE_TCE_Dir;
	private String MM_MM_Dir;
	private String T_T_Dir;
	
	private String relName;
	
	// map: field -> Set<TCE_TCE>
		private Map<jq_Field, 
					Set<Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
					         Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>>> f_TCE_TCE;
	// map: field -> Set<MM_MM>
	private Map<jq_Field, 
				Set<chord.util.tuple.object.Quad<jq_Method,jq_Method,jq_Method,jq_Method>>> f_MM_MM;
	// map: field -> Set<T_T>
	private Map<jq_Field, 
				Set<Pair<Trio<Ctxt, Ctxt, jq_Method>,
						 Trio<Ctxt, Ctxt, jq_Method>>>> f_T_T;
	// list for counters
	private List<Counter> counterList;
	
	// Map for counterList storing counterList for every relation which has been printed
	private Map<String, List<Counter>> counterListMap = new HashMap<String, List<Counter>>();
	
	// relation for dlog input
	private final RelPrinterInput relPrinterInput;
	// dlog output file
	private final String printer_output_dlog = "drrace-printer-output-dlog";
	
	public DrRacePrinter_dlog(String printDir){
		this.printDir = printDir;
		new File(Config.outDirName, printDir).mkdir();
		
		f_TCE_TCE = new HashMap<jq_Field, 
				Set<Hext<Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad, 
						 Trio<Ctxt, Ctxt, jq_Method>,Ctxt, Quad>>>();

		f_MM_MM = new HashMap<jq_Field, 
			  		  Set<chord.util.tuple.object.Quad<jq_Method,jq_Method,jq_Method,jq_Method>>>();
		
		f_T_T = new HashMap<jq_Field, 
					Set<Pair<Trio<Ctxt, Ctxt, jq_Method>,
							 Trio<Ctxt, Ctxt, jq_Method>>>>();
		
		counterList = new ArrayList<Counter>();
		
		relPrinterInput = (RelPrinterInput) ClassicProject.g().getTrgt("printerInput");
	}
	
	// This print method is used for printing a single relation
	public void print(String relName){
		this.relName = relName;
		
		init();
		
		analyzeRel();
		
		print_TCE_TCE();
		print_MM_MM();
		print_TT_TT();
		
		printAndStoreCounter();
	}
	
	// This print method is used for printing multiple relations which have be printed before.
	// relNameList is a list of relation name.
	public void print(List<String> relNameList){
		PrintWriter out = OutDirUtils.newPrintWriter(printDir + "/Counters");
		
		for(String mRelName : relNameList){
			List<Counter> mCounterList = counterListMap.get(mRelName);
			out.println(mRelName + ":");
			for(Counter counter : mCounterList){
				out.println(counter.getName() + " = " + counter.getNum());
			}
			out.println();
		}
		
		out.close();
	}
	
	private void init(){
		new File(Config.outDirName, printDir + "/" + relName).mkdir();
		
		TCE_TCE_Dir = printDir + "/" + relName + "/TCE_TCE";
		MM_MM_Dir =   printDir + "/" + relName  + "/MM_MM";
		T_T_Dir =     printDir + "/" + relName  + "/T_T";
		
		new File(Config.outDirName, TCE_TCE_Dir).mkdir();
		new File(Config.outDirName, MM_MM_Dir).mkdir();
		new File(Config.outDirName, T_T_Dir).mkdir();
		
		f_TCE_TCE.clear();
		f_MM_MM.clear();
		f_T_T.clear();
		
		// we have to re-allocate counterList here
		// because the counterListMap needs the old one
		counterList = new ArrayList<Counter>();;
		
		relPrinterInput.clearSet();
		ClassicProject.g().resetTaskDone(relPrinterInput);
		ClassicProject.g().resetTaskDone(printer_output_dlog);
	}
	
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
			
			if(!f_T_T.containsKey(f1))
				f_T_T.put(f1, new HashSet<Pair<Trio<Ctxt, Ctxt, jq_Method>,
									 		   Trio<Ctxt, Ctxt, jq_Method>>>());
			f_T_T.get(f1).add(new Pair<Trio<Ctxt, Ctxt, jq_Method>,
							 		   Trio<Ctxt, Ctxt, jq_Method>>(t1,t2));
			
			relPrinterInput.addToSet(tuple);;
		}
		rel.close();
		
		ClassicProject.g().runTask(relPrinterInput);
		ClassicProject.g().runTask(printer_output_dlog);
		generate_F_MM_MM();
	}
	
	// using dlog to generate F_MM_MM
	private void generate_F_MM_MM(){
		final ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("output_F_MM_MM");
		rel.load();
		PentIterable<jq_Field, jq_Method, jq_Method, 
					 		   jq_Method, jq_Method > tuples = rel.getAry5ValTuples();
	 	for(Pent<jq_Field, jq_Method, jq_Method, jq_Method, jq_Method> tuple : tuples){
	 		jq_Field mF = tuple.val0;
			jq_Method entryCallback_m1 = tuple.val1;
			jq_Method m1 = tuple.val2;
			jq_Method entryCallback_m2 = tuple.val3;
			jq_Method m2 = tuple.val4;
			
			if(!f_MM_MM.containsKey(mF))
				f_MM_MM.put(mF, 
						    new HashSet<chord.util.tuple.object.Quad<jq_Method,jq_Method,
						    										 jq_Method,jq_Method>>());
				f_MM_MM.get(mF).add(new chord.util.tuple.object.Quad
											<jq_Method,jq_Method,
											 jq_Method,jq_Method>
												(entryCallback_m1,m1,entryCallback_m2,m2));
	 	}
		
		rel.close();
	}
	
	// print result in format TCMI_TCME grouped by fields
	private void print_TCE_TCE() {
		int counterAll = 0;
		int fieldCounter = 0;
		for(Entry<jq_Field, 
				  Set<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad, 
				  		   Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>>> entry : f_TCE_TCE.entrySet()){
			jq_Field mF = entry.getKey();
			fieldCounter = fieldCounter + 1;
			
			PrintWriter out = OutDirUtils.newPrintWriter(TCE_TCE_Dir + "/" + fieldCounter + ": " + mF.getName());
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
				counterAll = counterAll + 1;
				
				out.println("");
				out.println("No." + counterPerField);
				out.println("t1= " + t1);
				out.println("c1= " + c1);
				out.println("m1= " + m1);
				out.println("e1= " + e1);
				out.println("|||");
				out.println("t2= " + t2);
				out.println("c2= " + c2);
				out.println("m2= " + m2);
				out.println("e2= " + e2);
			}
			out.close();
		}
		counterList.add(new Counter("TCE_TCE", counterAll));
	}
	
	// print result in format MM_MM grouped by fields
	private void print_MM_MM() {
		int counterAll = 0;
		int fieldCounter = 0;
		for(Entry<jq_Field, 
		  		  Set<chord.util.tuple.object.Quad<jq_Method,jq_Method,
		  		  								   jq_Method,jq_Method>>> entry : f_MM_MM.entrySet()){
			jq_Field mF = entry.getKey();
			fieldCounter = fieldCounter + 1;
			
			PrintWriter out = OutDirUtils.newPrintWriter(MM_MM_Dir + "/" + fieldCounter + ": " + mF.getName());
			out.println("Field No." + fieldCounter + ": " + mF);
			
			int counterPerField = 0;
			Set<chord.util.tuple.object.Quad<jq_Method,jq_Method,
				   							 jq_Method,jq_Method>> MM_MM_Set = entry.getValue();
			for(chord.util.tuple.object.Quad<jq_Method,jq_Method,
						 					 jq_Method,jq_Method> MM_MM : MM_MM_Set){
				jq_Method m0 = MM_MM.val0;
				jq_Method m1 = MM_MM.val1;
				jq_Method m2 = MM_MM.val2;
				jq_Method m3 = MM_MM.val3;
								
				counterPerField = counterPerField + 1;
				counterAll = counterAll + 1;

				out.println("");
				out.println("No." + counterPerField);
				out.println(m0);
				out.println("-->");
				out.println(m1);
				out.println("|||");
				out.println(m2);
				out.println("-->");
				out.println(m3);
			}
			out.close();
		}
		counterList.add(new Counter("MM_MM", counterAll));
	}
	
	// print result in format T_T grouped by fields
	private void print_TT_TT() {
		int counterAll = 0;
		int fieldCounter = 0;
		for(Entry<jq_Field, 
				  Set<Pair<Trio<Ctxt, Ctxt, jq_Method>,
				  		   Trio<Ctxt, Ctxt, jq_Method>>>> entry : f_T_T.entrySet()){
			jq_Field mF = entry.getKey();
			fieldCounter = fieldCounter + 1;
			
			PrintWriter out = OutDirUtils.newPrintWriter(T_T_Dir + "/" + fieldCounter + ": " + mF.getName());
			out.println("Field No." + fieldCounter + ": " + mF);
			
			int counterPerField = 0;
			Set<Pair<Trio<Ctxt, Ctxt, jq_Method>,
	  		   		 Trio<Ctxt, Ctxt, jq_Method>>> T_T_Set = entry.getValue();
			for(Pair<Trio<Ctxt, Ctxt, jq_Method>,
	  		   		 Trio<Ctxt, Ctxt, jq_Method>> T_T : T_T_Set){
				Trio<Ctxt, Ctxt, jq_Method> t1 = T_T.val0;
				Trio<Ctxt, Ctxt, jq_Method> t2 = T_T.val1;
								
				counterPerField = counterPerField + 1;
				counterAll = counterAll + 1;
				
				out.println("");
				out.println("No." + counterPerField);
				out.println("t1:" + t1);
				out.println("|||");
				out.println("t2:" + t2);
			}
			out.close();
		}
		counterList.add(new Counter("T_T", counterAll));
	}
	
	// print counter information for each result format
	// store the counter list into counterListMap
	private void printAndStoreCounter(){
		PrintWriter out = OutDirUtils.newPrintWriter(printDir + "/" + relName + "/" + relName + "_counter");
		for(Counter counter : counterList){
			out.println(counter.getName() + " = " + counter.getNum());
		}
		out.close();
		
		counterListMap.put(relName, counterList);
	}
}
