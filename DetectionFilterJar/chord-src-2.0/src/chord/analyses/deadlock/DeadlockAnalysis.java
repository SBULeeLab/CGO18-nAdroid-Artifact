/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.deadlock;

import java.io.PrintWriter;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

import chord.project.Config;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

import chord.util.ArraySet;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.CSObj;
import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.analyses.alias.DomO;
import chord.analyses.alias.DomC;
import chord.analyses.alloc.DomH;
import chord.bddbddb.Rel.RelView;
import chord.analyses.thread.DomA;
import chord.analyses.invk.DomI;
import chord.analyses.lock.DomL;
import chord.analyses.method.DomM;
import chord.util.SetUtils;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Static deadlock analysis.
 * <p>
 * Outputs relation 'deadlock' containing each tuple (a1,l1,l2,a2,l3,l4) denoting a possible
 * deadlock between abstract thread a1, which acquires abstract lock l1 followed by abstract lock l2,
 * and abstract thread a2, which acquires abstract lock l3 followed by abstract lock l4.
 * <p>
 * Recognized system properties:
 * <ul>
 *   <li>chord.exclude.escaping (default is false).</li>
 *   <li>chord.exclude.parallel (default is false).</li>
 *   <li>chord.exclude.nonreent (default is false).</li>
 *   <li>chord.exclude.nongrded (default is false).</li>
 *   <li>chord.print.results (default is false).</li>
 *   <li>All system properties recognized by abstract contexts analysis
 *       (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Zhifeng Lai (zflai@cse.ust.hk)
 */
@Chord(
	name="deadlock-java",
	namesOfTypes = { "N" },
	types = { DomN.class }
)
public class DeadlockAnalysis extends JavaAnalysis {
	private DomA domA;
	private DomC domC;	
	private DomH domH;
	private DomI domI;
	private DomL domL;
	private DomM domM;
	private DomN domN;
	
	private ProgramRel relNC;
	private ProgramRel relNL;
	private ProgramRel relDeadlock;
	private ProgramRel relSyncCLC;
	
	private ICSCG thrSenAbbrCSCG;
	private ThrSenAbbrCSCGAnalysis thrSenAbbrCSCGAnalysis;
	private final Map<CM, Set<CM>> CMCMMap = new HashMap<CM, Set<CM>>();

	private void init() {
		domA = (DomA) ClassicProject.g().getTrgt("A");
		domC = (DomC) ClassicProject.g().getTrgt("C");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domL = (DomL) ClassicProject.g().getTrgt("L");
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domN = (DomN) ClassicProject.g().getTrgt("N");
		
		relNC = (ProgramRel) ClassicProject.g().getTrgt("NC");
		relNL = (ProgramRel) ClassicProject.g().getTrgt("NL");
		relDeadlock = (ProgramRel) ClassicProject.g().getTrgt("deadlock");
		relSyncCLC = (ProgramRel) ClassicProject.g().getTrgt("syncCLC");
		
		thrSenAbbrCSCGAnalysis = (ThrSenAbbrCSCGAnalysis)
			ClassicProject.g().getTrgt("thrsen-abbr-cscg-java");
	}
	
	private void finish() {
		if (relNC.isOpen()) {
			relNC.isOpen();
		}
		if (relNL.isOpen()) {
			relNL.close();
		}
		if (relDeadlock.isOpen()) {
			relDeadlock.close();
		}
		if (relSyncCLC.isOpen()) {
			relSyncCLC.close();
		}
	}
	
	public void run() {
		boolean excludeParallel = Boolean.getBoolean("chord.exclude.parallel");
		boolean excludeEscaping = Boolean.getBoolean("chord.exclude.escaping");
		boolean excludeNonreent = Boolean.getBoolean("chord.exclude.nonreent");
		boolean excludeNongrded = Boolean.getBoolean("chord.exclude.nongrded");

		init();
		
		ClassicProject.g().runTask(domL);

		ClassicProject.g().runTask("ctxts-java");
		ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
		ClassicProject.g().runTask(thrSenAbbrCSCGAnalysis);
		thrSenAbbrCSCG = thrSenAbbrCSCGAnalysis.getCallGraph();
		domN.clear();
		Program program = Program.g();
		for (Inst i : domL) {
			jq_Method m = i.getMethod();
			Set<Ctxt> cs = thrSenAbbrCSCG.getContexts(m);
			for (Ctxt c : cs) {
				domN.add(new Pair<Ctxt, Inst>(c, i));
			}
		}
		domN.save();

		relNC.zero();
		relNL.zero();
		for (Pair<Ctxt, Inst> cm : domN) {
			int n = domN.indexOf(cm);
			int c = domC.indexOf(cm.val0);
			int l = domL.indexOf(cm.val1);
			relNC.add(n, c);
			relNL.add(n, l);
		}
		relNC.save();
		relNL.save();

		if (excludeParallel)
			ClassicProject.g().runTask("deadlock-parallel-exclude-dlog");
		else
			ClassicProject.g().runTask("deadlock-parallel-include-dlog");
		if (excludeEscaping)
			ClassicProject.g().runTask("deadlock-escaping-exclude-dlog");
		else
			ClassicProject.g().runTask("deadlock-escaping-include-dlog");
		if (excludeNonreent)
			ClassicProject.g().runTask("deadlock-nonreent-exclude-dlog");
		else
			ClassicProject.g().runTask("deadlock-nonreent-include-dlog");
		if (excludeNongrded)
			ClassicProject.g().runTask("deadlock-nongrded-exclude-dlog");
		else
			ClassicProject.g().runTask("deadlock-nongrded-include-dlog");
		ClassicProject.g().runTask("deadlock-dlog");
		ClassicProject.g().runTask("deadlock-stats-dlog");

		if (Config.printResults) {
			printResults();
		}
		
		finish();
	}

	private CSObj getPointsTo(int cIdx, int lIdx) {
		RelView view = relSyncCLC.getView();
		view.selectAndDelete(0, cIdx);
		view.selectAndDelete(1, lIdx);
		Iterable<Ctxt> ctxts = view.getAry1ValTuples();
		Set<Ctxt> pts = SetUtils.newSet(view.size());
		for (Ctxt ctxt : ctxts)
			pts.add(ctxt);
		view.free();
		return new CSObj(pts);
	}
	
	private void printResults() {
		final DomO domO = new DomO();
		domO.setName("O");
		
		PrintWriter out;

		relDeadlock.load();
		relSyncCLC.load();

		Program program = Program.g();

		out = OutDirUtils.newPrintWriter("deadlocklist.xml");
		out.println("<deadlocklist>");
		for (Object[] tuple : relDeadlock.getAryNValTuples()) {
			Trio<Ctxt, Ctxt, jq_Method> t1Val = (Trio) tuple[0];
			Pair<Ctxt, Inst> n1Val = (Pair) tuple[1];
			Ctxt c1Val = n1Val.val0;
			Inst l1Val = n1Val.val1;
			Pair<Ctxt, Inst> n2Val = (Pair) tuple[2];
			Ctxt c2Val = n2Val.val0;
			Inst l2Val = n2Val.val1;
			Trio<Ctxt, Ctxt, jq_Method> t2Val = (Trio) tuple[3];
			Pair<Ctxt, Inst> n3Val = (Pair) tuple[4];
			Ctxt c3Val = n3Val.val0;
			Inst l3Val = n3Val.val1;
			Pair<Ctxt, Inst> n4Val = (Pair) tuple[5];
			Ctxt c4Val = n4Val.val0;
			Inst l4Val = n4Val.val1;
			int l1 = domL.indexOf(l1Val);
			int l2 = domL.indexOf(l2Val);
			int l3 = domL.indexOf(l3Val);
			int l4 = domL.indexOf(l4Val);
			// require l1,l2 <= l3,l4 and if not switch
			if (l1 > l3 || (l1 == l3 && l2 > l4)) {
				{
					int tmp;
					tmp = l1; l1 = l3; l3 = tmp;
					tmp = l2; l2 = l4; l4 = tmp;
				}
				{
					Inst tmp;
					tmp = l1Val; l1Val = l3Val; l3Val = tmp;
					tmp = l2Val; l2Val = l4Val; l4Val = tmp;
				}
				{
					Ctxt tmp;
					tmp = c1Val; c1Val = c3Val; c3Val = tmp; 
					tmp = c2Val; c2Val = c4Val; c4Val = tmp;
				}
				{
					Trio<Ctxt, Ctxt, jq_Method> tmp;
					tmp = t1Val; t1Val = t2Val; t2Val = tmp;
				}
			}
			int t1 = domA.indexOf(t1Val);
			int t2 = domA.indexOf(t2Val);
			int c1 = domC.indexOf(c1Val);
			int c2 = domC.indexOf(c2Val);
			int c3 = domC.indexOf(c3Val);
			int c4 = domC.indexOf(c4Val);
			Ctxt t1cVal = t1Val.val1;
			Ctxt t2cVal = t2Val.val1;
			int t1c = domC.indexOf(t1cVal);
			int t2c = domC.indexOf(t2cVal);
			jq_Method t1mVal = t1Val.val2;
			jq_Method t2mVal = t2Val.val2;
			int t1m = domM.indexOf(t1mVal);
			int t2m = domM.indexOf(t2mVal);
			jq_Method m1Val = l1Val.getMethod();
			jq_Method m2Val = l2Val.getMethod();
			jq_Method m3Val = l3Val.getMethod();
			jq_Method m4Val = l4Val.getMethod();
			int m1 = domM.indexOf(m1Val);
			int m2 = domM.indexOf(m2Val);
			int m3 = domM.indexOf(m3Val);
			int m4 = domM.indexOf(m4Val);
			CSObj o1Val = getPointsTo(c1, l1);
			CSObj o2Val = getPointsTo(c2, l2);
			CSObj o3Val = getPointsTo(c3, l3);
			CSObj o4Val = getPointsTo(c4, l4);
			int o1 = domO.getOrAdd(o1Val);
			int o2 = domO.getOrAdd(o2Val);
			int o3 = domO.getOrAdd(o3Val);
			int o4 = domO.getOrAdd(o4Val);
			addToCMCMMap(t1cVal, t1mVal, c1Val, m1Val);
			addToCMCMMap(t2cVal, t2mVal, c3Val, m3Val);
			addToCMCMMap(c1Val , m1Val , c2Val, m2Val);
			addToCMCMMap(c3Val , m3Val , c4Val, m4Val);
			out.println("<deadlock " +
				"group=\"" + l1 + "_" + l2 + "_" + l3 + "_" + l4 + "\" " +
				"T1id=\"A" + t1 + "\" T2id=\"A" + t2 + "\" " +
				"C1id=\"C"  + c1 + "\" M1id=\"M" + m1 + "\" L1id=\"L" + l1 + "\" O1id=\"O" + o1 + "\" " +
				"C2id=\"C"  + c2 + "\" M2id=\"M" + m2 + "\" L2id=\"L" + l2 + "\" O2id=\"O" + o2 + "\" " +
				"C3id=\"C"  + c3 + "\" M3id=\"M" + m3 + "\" L3id=\"L" + l3 + "\" O3id=\"O" + o3 + "\" " +
				"C4id=\"C"  + c4 + "\" M4id=\"M" + m4 + "\" L4id=\"L" + l4 + "\" O4id=\"O" + o4 + "\"/>");
		}
		out.println("</deadlocklist>");
		out.close();		
		
		IPathVisitor<Pair<Ctxt, jq_Method>> visitor =
			new IPathVisitor<Pair<Ctxt, jq_Method>>() {
				public String visit(Pair<Ctxt, jq_Method> origNode,
						Pair<Ctxt, jq_Method> destNode) {
					Ctxt ctxt = origNode.val0;
					Set<Quad> insts = thrSenAbbrCSCG.getLabels(origNode, destNode);
					for (Quad inst : insts) {
						return "<elem Cid=\"C" + domC.indexOf(ctxt) + "\" " +
							"Iid=\"I" + domI.indexOf(inst) + "\"/>";
					}
					return "";
				}
			};

		out = OutDirUtils.newPrintWriter("CMCMlist.xml");
		out.println("<CMCMlist>");
		
		for (CM cm1 : CMCMMap.keySet()) {
			Ctxt ctxt1 = cm1.val0;
			jq_Method meth1 = cm1.val1;
			int c1 = domC.indexOf(ctxt1);
			int m1 = domM.indexOf(meth1);
			Set<CM> cmSet = CMCMMap.get(cm1);
			ShortestPathBuilder<Pair<Ctxt, jq_Method>> builder =
				new ShortestPathBuilder(thrSenAbbrCSCG, cm1, visitor);
			for (CM cm2 : cmSet) {
				Ctxt ctxt2 = cm2.val0;
				jq_Method meth2 = cm2.val1;
				int c2 = domC.indexOf(ctxt2);
				int m2 = domM.indexOf(meth2);
				out.println("<CMCM C1id=\"C" + c1 + "\" M1id=\"M" + m1 +
					"\" C2id=\"C" + c2 + "\" M2id=\"M" + m2 + "\">");
				   String path = builder.getShortestPathTo(cm2);
				out.println("<path>");
				out.println(path);
				out.println("</path>");
				out.println("</CMCM>");
			}
		}
		out.println("</CMCMlist>");
		out.close();
		
		domO.saveToXMLFile();
		domC.saveToXMLFile();
		domA.saveToXMLFile();
		domH.saveToXMLFile();
		domI.saveToXMLFile();
		domM.saveToXMLFile();
		domL.saveToXMLFile();

		OutDirUtils.copyResourceByName("web/style.css");
		OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/method/M.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/lock/Llist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alloc/Hlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alloc/H.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/invk/Ilist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/invk/I.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/thread/Alist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/thread/A.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/alias/Olist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alias/O.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/alias/Clist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/alias/C.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/deadlock/web/results.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/deadlock/web/results.xml");
		OutDirUtils.copyResourceByName("chord/analyses/deadlock/web/group.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/deadlock/web/paths.xsl");

		OutDirUtils.runSaxon("results.xml", "group.xsl");
		OutDirUtils.runSaxon("results.xml", "paths.xsl");

		program.HTMLizeJavaSrcFiles();
	}

	private class CM extends Pair<Ctxt, jq_Method> {
		public CM(Ctxt c, jq_Method m) {
			super(c, m);
		}
	};

	private void addToCMCMMap(Ctxt c1, jq_Method m1,
			Ctxt c2, jq_Method m2) {
		CM cm1 = new CM(c1, m1);
		Set<CM> s = CMCMMap.get(cm1);
		if (s == null) {
			s = new ArraySet<CM>();
			CMCMMap.put(cm1, s);
		}
		CM cm2 = new CM(c2, m2);
		s.add(cm2);
	}
}
