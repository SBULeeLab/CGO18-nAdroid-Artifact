/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.CSObj;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alias.DomC;
import chord.analyses.alias.DomO;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.analyses.alloc.DomH;
import chord.util.Execution;
import chord.analyses.thread.DomA;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.analyses.field.DomF;
import chord.analyses.heapacc.DomE;
import chord.analyses.invk.DomI;
import chord.analyses.lock.DomL;
import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.SetUtils;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Static datarace analysis.
 * <p>
 * Outputs relation 'datarace' containing each tuple (a1,c1,e1,a2,c2,e2) denoting a possible race
 * between abstract threads a1 and a2 executing accesses e1 and e2, respectively, in abstract contexts
 * c1 and c2 of the methods containing those accesses, respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 *   <li>chord.exclude.escaping (default is false).</li>
 *   <li>chord.exclude.parallel (default is false).</li>
 *   <li>chord.exclude.nongrded (default is false).</li>
 *   <li>chord.print.results (default is false).</li>
 *   <li>All system properties recognized by abstract contexts analysis
 *       (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 */
@Chord(
	name="datarace-java",
	consumes="P"
)
public class DataraceAnalysis extends JavaAnalysis {
	private static final boolean percy = System.getProperty("percy", "false").equals("true");

	private ProgramRel relRefineH;
	private ProgramRel relRefineM;
	private ProgramRel relRefineV;
	private ProgramRel relRefineI;
	private DomM domM;
	private DomI domI;
	private DomF domF;
	private DomE domE;
	private DomA domA;
	private DomH domH;
	private DomC domC;
	private DomL domL;
	private CSAliasAnalysis hybridAnalysis;
	private ThrSenAbbrCSCGAnalysis thrSenAbbrCSCGAnalysis;

	private Execution X;

	private void init() {
		relRefineH = (ProgramRel) ClassicProject.g().getTrgt("refineH");
		relRefineM = (ProgramRel) ClassicProject.g().getTrgt("refineM");
		relRefineV = (ProgramRel) ClassicProject.g().getTrgt("refineV");
		relRefineI = (ProgramRel) ClassicProject.g().getTrgt("refineI");
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domF = (DomF) ClassicProject.g().getTrgt("F");
		domE = (DomE) ClassicProject.g().getTrgt("E");
		domA = (DomA) ClassicProject.g().getTrgt("A");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		domC = (DomC) ClassicProject.g().getTrgt("C");
		domL = (DomL) ClassicProject.g().getTrgt("L");
		hybridAnalysis = (CSAliasAnalysis) ClassicProject.g().getTrgt("cs-alias-java");
		thrSenAbbrCSCGAnalysis = (ThrSenAbbrCSCGAnalysis)
			ClassicProject.g().getTrgt("thrsen-abbr-cscg-java");
	}

	public void run() {
		if (percy) {
			X = Execution.v();		
	  X.putOption("taskNames", "datarace-dlog");
	  X.flushOptions();
			X.addSaveFiles("inputs.dat", "outputs.dat");		
			if (X.getBooleanArg("saveStrings", false))		
				X.addSaveFiles("inputs.strings", "outputs.strings");
		}
		
		int maxIters = Integer.getInteger("chord.max.iters", 0);
		assert (maxIters >= 0);
		
		boolean excludeParallel = Boolean.getBoolean("chord.exclude.parallel");
		boolean excludeEscaping = Boolean.getBoolean("chord.exclude.escaping");
		boolean excludeNongrded = Boolean.getBoolean("chord.exclude.nongrded");

		init();

		if (Config.verbose >= 2) Messages.log("maxIters=" + maxIters);
		int numIters = 0; // Number of refinement iterations
		while (true) {
			// Run analysis
			if (Config.verbose >= 2) Messages.log("Running datarace analysis (numIters="+numIters+")");
			ClassicProject.g().runTask("ctxts-java");
			ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
			ClassicProject.g().runTask("datarace-prologue-dlog");
			if (excludeParallel)
				ClassicProject.g().runTask("datarace-parallel-exclude-dlog");
			else
				ClassicProject.g().runTask("datarace-parallel-include-dlog");
			if (excludeEscaping)
				ClassicProject.g().runTask("datarace-escaping-exclude-dlog");
			else
				ClassicProject.g().runTask("datarace-escaping-include-dlog");
			if (excludeNongrded)
				ClassicProject.g().runTask("datarace-nongrded-exclude-dlog");
			else
				ClassicProject.g().runTask("datarace-nongrded-include-dlog");
			ClassicProject.g().runTask("datarace-dlog");
			ClassicProject.g().runTask("datarace-stats-dlog");
			if (numIters == maxIters)
				break;
			ClassicProject.g().runTask("datarace-feedback-dlog");
			if (!excludeParallel)
				ClassicProject.g().runTask("refine-mhp-dlog");
			if (!excludeEscaping)
				ClassicProject.g().runTask("refine-esc-dlog");
			ClassicProject.g().runTask("refine-hybrid-dlog");
			relRefineH.load();
			int numRefineH = relRefineH.size();
			relRefineH.close();
			relRefineM.load();
			int numRefineM = relRefineM.size();
			relRefineM.close();
			relRefineV.load();
			int numRefineV = relRefineV.size();
			relRefineV.close();
			relRefineI.load();
			int numRefineI = relRefineI.size();
			relRefineI.close();
			if (numRefineH == 0 && numRefineM == 0 &&
				numRefineV == 0 && numRefineI == 0)
				break;
			ClassicProject.g().resetTaskDone("ctxts-java");
			numIters++;
		}
		
		if (Config.printResults)
			printResults();

		if (percy) {
			outputRaces();		
			X.finish(null);
		}
	}

	private void outputCtxtInsDataraces() {
		PrintWriter writer =
			 OutDirUtils.newPrintWriter("ctxtInsDatarace.txt");
		final ProgramRel relDatarace = (ProgramRel) ClassicProject.g().getTrgt("ctxtInsDatarace");		
		relDatarace.load();		
		final PairIterable<Inst, Inst> tuples = relDatarace.getAry2ValTuples();		
		int numRaces = 0;		
		for (Pair<Inst, Inst> p : tuples) {		
			Quad quad0 = (Quad) p.val0;	
			int e1 = domE.indexOf(quad0);
			Quad quad1 = (Quad) p.val1;	
			int e2 = domE.indexOf(quad1);
			writer.println(e1 + " - " + e2);
			numRaces++;		
		}		
		relDatarace.close();
		writer.println("Total # of races=" + numRaces);
		writer.close();
	}

  int relSize(String name) {
	ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(name); rel.load();
	int n = rel.size();
	rel.close();
	return n;
  }

	private void outputRaces() {
		PrintWriter datOut = OutDirUtils.newPrintWriter("outputs.dat");		
			
		final ProgramRel relDatarace = (ProgramRel) ClassicProject.g().getTrgt("ctxtInsDatarace");		
		relDatarace.load();		
		final PairIterable<Inst, Inst> tuples = relDatarace.getAry2ValTuples();		
		int numRaces = 0;		
		for (Pair<Inst, Inst> p : tuples) {		
			Quad quad0 = (Quad) p.val0;		
			int e1 = domE.indexOf(quad0);		
			Quad quad1 = (Quad) p.val1;		
			int e2 = domE.indexOf(quad1);		
			datOut.println(e1 + " " + e2);		
			numRaces++;		
		}		
		relDatarace.close();		
		
	datOut.close();
	X.putOutput("numQueries", relSize("ctxtInsStartingRace"));
	X.putOutput("numRaces", numRaces);		
	X.putOutput("numUnproven", numRaces);		
	X.putOutput("absSize", domC.size());

		PrintWriter strOut = OutDirUtils.newPrintWriter("outputs.strings");		
		for (int e = 0; e < domE.size(); e++)		
			strOut.println("E"+e + " " + estr(e));		
		strOut.close();		
	}		
			
	public String estr(int e) {		
		if (e < 0) return "-";		
		Quad quad = (Quad)domE.get(e);		
		return quad.toJavaLocStr()+" "+quad.toString();
	}

	private void printResults() {
		outputCtxtInsDataraces();
		ClassicProject.g().runTask(hybridAnalysis);
		ClassicProject.g().runTask(thrSenAbbrCSCGAnalysis);
		final ICSCG thrSenAbbrCSCG = thrSenAbbrCSCGAnalysis.getCallGraph();
		ClassicProject.g().runTask("datarace-epilogue-dlog");
		final ProgramDom<Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> domTCE =
			new ProgramDom<Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>>();
		domTCE.setName("TCE");
		final DomO domO = new DomO();
		domO.setName("O");

		PrintWriter out;

		out = OutDirUtils.newPrintWriter("dataracelist.xml");
		out.println("<dataracelist>");
		final ProgramRel relDatarace = (ProgramRel) ClassicProject.g().getTrgt("datarace");
		relDatarace.load();
		final ProgramRel relRaceCEC = (ProgramRel) ClassicProject.g().getTrgt("raceCEC");
		relRaceCEC.load();
		final Iterable<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> tuples = relDatarace.getAry6ValTuples();
		for (Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				  Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tuple : tuples) {
			int tce1 = domTCE.getOrAdd(new Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>(
				tuple.val0, tuple.val1, tuple.val2));
			int tce2 = domTCE.getOrAdd(new Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>(
				tuple.val3, tuple.val4, tuple.val5));
			RelView view = relRaceCEC.getView();
			view.selectAndDelete(0, tuple.val1);
			view.selectAndDelete(1, tuple.val2);
			view.selectAndDelete(2, tuple.val4);
			view.selectAndDelete(3, tuple.val5);
			Set<Ctxt> pts = new ArraySet<Ctxt>(view.size());
			Iterable<Ctxt> res = view.getAry1ValTuples();
			for (Ctxt ctxt : res) {
				pts.add(ctxt);
			}
			view.free();
			int p = domO.getOrAdd(new CSObj(pts));
			jq_Field fld = tuple.val2.getField();
			int f = domF.indexOf(fld);
			out.println("<datarace Oid=\"O" + p +
				"\" Fid=\"F" + f + "\" " +
				"TCE1id=\"TCE" + tce1 + "\" "  +
				"TCE2id=\"TCE" + tce2 + "\"/>");
		}
		relDatarace.close();
		relRaceCEC.close();
		out.println("</dataracelist>");
		out.close();

		ClassicProject.g().runTask("LI");
		ClassicProject.g().runTask("LE");
		ClassicProject.g().runTask("syncCLC-dlog");
		final ProgramRel relLI = (ProgramRel) ClassicProject.g().getTrgt("LI");
		final ProgramRel relLE = (ProgramRel) ClassicProject.g().getTrgt("LE");
		final ProgramRel relSyncCLC = (ProgramRel) ClassicProject.g().getTrgt("syncCLC");
		relLI.load();
		relLE.load();
		relSyncCLC.load();

		final Map<Pair<Ctxt, jq_Method>, ShortestPathBuilder> srcNodeToSPB =
			new HashMap<Pair<Ctxt, jq_Method>, ShortestPathBuilder>();

		final IPathVisitor<Pair<Ctxt, jq_Method>> visitor =
			new IPathVisitor<Pair<Ctxt, jq_Method>>() {
				public String visit(Pair<Ctxt, jq_Method> origNode,
						Pair<Ctxt, jq_Method> destNode) {
					Set<Quad> insts = thrSenAbbrCSCG.getLabels(origNode, destNode);
					jq_Method srcM = origNode.val1;
					int mIdx = domM.indexOf(srcM);
					Ctxt srcC = origNode.val0;
					int cIdx = domC.indexOf(srcC);
					String lockStr = "";
					Quad inst = insts.iterator().next();
					int iIdx = domI.indexOf(inst);
					RelView view = relLI.getView();
					view.selectAndDelete(1, iIdx);
					Iterable<Inst> locks = view.getAry1ValTuples();
					for (Inst lock : locks) {
						int lIdx = domL.indexOf(lock);
						RelView view2 = relSyncCLC.getView();
						view2.selectAndDelete(0, cIdx);
						view2.selectAndDelete(1, lIdx);
						Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
						Set<Ctxt> pts = SetUtils.newSet(view2.size());
						for (Ctxt ctxt : ctxts)
							pts.add(ctxt);
						int oIdx = domO.getOrAdd(new CSObj(pts));
						view2.free();
						lockStr += "<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
							mIdx + "\" Oid=\"O" + oIdx + "\"/>";
					}
					view.free();
					return lockStr + "<elem Cid=\"C" + cIdx + "\" " +
						"Iid=\"I" + iIdx + "\"/>";
				}
			};

		out = OutDirUtils.newPrintWriter("TCElist.xml");
		out.println("<TCElist>");
		for (Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tce : domTCE) {
			Trio<Ctxt, Ctxt, jq_Method> srcOCM = tce.val0;
			Ctxt methCtxt = tce.val1;
			Quad heapInst = tce.val2;
			int cIdx = domC.indexOf(methCtxt);
			int eIdx = domE.indexOf(heapInst);
			out.println("<TCE id=\"TCE" + domTCE.indexOf(tce) + "\" " +
				"Tid=\"A" + domA.indexOf(srcOCM)	+ "\" " +
				"Cid=\"C" + cIdx + "\" " +
				"Eid=\"E" + eIdx + "\">");
			jq_Method dstM = heapInst.getMethod();
			int mIdx = domM.indexOf(dstM);
			RelView view = relLE.getView();
			view.selectAndDelete(1, eIdx);
			Iterable<Inst> locks = view.getAry1ValTuples();
			for (Inst lock : locks) {
				int lIdx = domL.indexOf(lock);
				RelView view2 = relSyncCLC.getView();
				view2.selectAndDelete(0, cIdx);
				view2.selectAndDelete(1, lIdx);
				Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
				Set<Ctxt> pts = SetUtils.newSet(view2.size());
				for (Ctxt ctxt : ctxts)
					pts.add(ctxt);
				int oIdx = domO.getOrAdd(new CSObj(pts));
				view2.free();
				out.println("<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
					mIdx + "\" Oid=\"O" + oIdx + "\"/>");
			}
			view.free();
			Pair<Ctxt, jq_Method> srcCM =
				new Pair<Ctxt, jq_Method>(srcOCM.val1, srcOCM.val2);
			ShortestPathBuilder spb = srcNodeToSPB.get(srcCM);
			if (spb == null) {
				spb = new ShortestPathBuilder(thrSenAbbrCSCG, srcCM, visitor);
				srcNodeToSPB.put(srcCM, spb);
			}
			Pair<Ctxt, jq_Method> dstCM =
				new Pair<Ctxt, jq_Method>(methCtxt, dstM);
			String path = spb.getShortestPathTo(dstCM);
			out.println("<path>");
			out.println(path);
			out.println("</path>");
			out.println("</TCE>");
		}
		out.println("</TCElist>");
		out.close();

		relLI.close();
		relLE.close();
		relSyncCLC.close();

		domO.saveToXMLFile();
		domC.saveToXMLFile();
		domA.saveToXMLFile();
		domH.saveToXMLFile();
		domI.saveToXMLFile();
		domM.saveToXMLFile();
		domE.saveToXMLFile();
		domF.saveToXMLFile();
		domL.saveToXMLFile();

		OutDirUtils.copyResourceByName("web/style.css");
		OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/method/M.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/lock/Llist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alloc/Hlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alloc/H.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/invk/Ilist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/invk/I.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/heapacc/Elist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/heapacc/E.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/field/Flist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/field/F.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/thread/Alist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/thread/A.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/alias/Olist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alias/O.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/alias/Clist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alias/C.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/datarace/web/results.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/datarace/web/results.xml");
		OutDirUtils.copyResourceByName("chord/analyses/datarace/web/group.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/datarace/web/paths.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/datarace/web/races.xsl");

		OutDirUtils.runSaxon("results.xml", "group.xsl");
		OutDirUtils.runSaxon("results.xml", "paths.xsl");
		OutDirUtils.runSaxon("results.xml", "races.xsl");

		Program.g().HTMLizeJavaSrcFiles();
	}
}
