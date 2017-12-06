/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.alias;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import gnu.trove.TIntArrayList;

import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.Execution;
import chord.util.StatFig;
import chord.bddbddb.Rel.RelView;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.Config;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ChordRuntimeException;

/**
 * Analysis for pre-computing abstract contexts.
 * <p>
 * The goal of this analysis is to translate client-specified inputs concerning the desired kind of context sensitivity
 * into relations that are subsequently consumed by context-sensitive may alias and call graph analyses.
 * <p>
 * This analysis allows:
 * <ul>
 *   <li>each method to be analyzed using a different kind of context sensitivity (one of context insensitivity, k-CFA,
 *       k-object-sensitivity, and copy-context-sensitivity),</li>
 *   <li>each local variable to be analyzed context sensitively or insensitively, and</li>
 *   <li>a different 'k' value to be used for each object allocation site and method call site.</li>
 * </ul>
 * This analysis can be called multiple times and in each invocation it can incorporate feedback from a client to adjust
 * the precision of the points-to information and call graph computed subsequently by the may alias and call graph
 * analyses.  Clients can indicate in each invocation:
 * <ul>
 *   <li>Which methods must be analyzed context sensitively (in addition to those already being analyzed context
 *       sensitively in the previous invocation of this analysis) and using what kind of context sensitivity; the
 *       remaining methods will be analyzed context insensitively (that is, in the lone 'epsilon' context)</li>
 *   <li>Which local variables of reference type must be analyzed context sensitively (in addition to those already being
 *       analyzed context sensitively in the previous invocation of this analysis); the remaining ones will be analyzed
 *       context insensitively (that is, their points-to information will be tracked in the lone 'epsilon' context).</li>
 *   <li>The object alocation sites and method call sites whose 'k' values must be incremented (over those used in the
 *       previous invocation of this analysis).</li>
 * </ul>
 * Recognized system properties:
 * <ul>
 *   <li>chord.ctxt.kind: the kind of context sensitivity to use for each method (and all its locals).
 *       It may be 'ci' (context insensitive) or 'cs' (k-CFA).</li>
 *   <li>chord.inst.ctxt.kind: the kind of context sensitivity to use for each instance method (and all its locals).
 *       It may be 'ci' (context insensitive), 'cs' (k-CFA), or 'co' (k-object-sensitive).</li>
 *   <li>chord.stat.ctxt.kind: the kind of context sensitivity to use for each static method (and all its locsals).
 *       It may be one of 'ci' (context insensitive), 'cs' (k-CFA), or 'cc' (copy-context-sensitive).</li>
 *   <li>chord.kobj.k and chord.kcfa.k: the 'k' value to use for each object allocation site and each method call site,
 *       respectively.</li>
 * </ul>
 * 
 * 
 * This analysis outputs the following domains and relations:
 * <ul>
 *   <li>C: domain containing all abstract contexts</li>
 *   <li>CC: relation containing each pair (c,c2) such that c2 is all but the last element of context c</li>
 *   <li>CH: relation containing each (c,h) such that object allocation site h is the last element of abstract context c</li>
 *   <li>CI: relation containing each (c,i) such that call site i is the last element of abstract context c</li>
 *   <li>CVC: relation containing each (c,v,o) such that local v might point to object o in context c of its declaring method.</li>
 *   <li>CFC: relation containing each (o1,f,o2) such that instance field f of object o1 might point to object o2</li>
 *   <li>FC: relation containing each (f,o) such that static field f may point to object o</li>
 *   <li>CICM: relation containing each (c,i,c2,m) if invocation i in context c can reach method 2 (in context c2)</li>
 *   <li>rootCM: relation containing each (c,m) such that method m is an entry method in context c</li>
 *   <li>reachableCM: relation containing each (c,m) such that method m can be called in context c</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "ctxts-java",
	consumes = { "IM", "VH" },
	produces = { "C", "CC", "CH", "CI", "epsilonV", "epsilonM", "kcfaSenM", "kobjSenM", "ctxtCpyM",
		"refinableCH", "refinableCI", "refinableM", "refinableV" },
	namesOfTypes = { "C" },
	types = { DomC.class }
)
public class CtxtsAnalysis extends JavaAnalysis {
	private static final boolean percy = System.getProperty("percy", "false").equals("true");

	private static final Set<Ctxt> emptyCtxtSet = Collections.emptySet();
	private static final Set<jq_Method> emptyMethSet = Collections.emptySet();
	private static final Quad[] emptyElems = new Quad[0];

	// includes all methods in domain
	private Set<Ctxt>[] methToCtxts;
	
	// ctxt kind is KCFASEN
	private TIntArrayList[] methToClrSites;
	// ctxt kind is KOBJSEN
	private TIntArrayList[] methToRcvSites;
	// ctxt kind is CTXTCPY
	private Set<jq_Method>[] methToClrMeths;
	
	private Set<Ctxt> epsilonCtxtSet;

	public static final int CTXTINS = 0;  // abbr ci; must be 0
	public static final int KOBJSEN = 1;  // abbr co
	public static final int KCFASEN = 2;  // abbr cs
	public static final int CTXTCPY = 3;  // abbr cc

	private int[] ItoM;
	private int[] HtoM;
	private Quad[] ItoQ;
	private Quad[] HtoQ;

	private jq_Method mainMeth;
	private boolean[] isCtxtSenV;	// indexed by domV
	private int[] methKind;		 // indexed by domM
	private int[] kobjValue;		// indexed by domH
	private int[] kcfaValue;		// indexed by domI

	private int kobjK;
	private int kcfaK;
	private int instCtxtKind;
	private int statCtxtKind;

	private int maxIters;
	private int currIter;
	
	private boolean isInitialized = false;

	private DomV domV;
	private DomM domM;
	private DomI domI;
	private DomH domH;
	private DomC domC;

	private ProgramRel relIM;
	private ProgramRel relVH;

	private ProgramRel relCC;
	private ProgramRel relCH;
	private ProgramRel relCI;

	private ProgramRel relRefineH;
	private ProgramRel relRefineM;
	private ProgramRel relRefineI;
	private ProgramRel relRefineV;
	
	private ProgramRel relRefinableM;
	private ProgramRel relRefinableV;
	private ProgramRel relRefinableCI;
	private ProgramRel relRefinableCH;
	
	private ProgramRel relEpsilonM;
	private ProgramRel relKcfaSenM;
	private ProgramRel relKobjSenM;
	private ProgramRel relCtxtCpyM;
	private ProgramRel relEpsilonV;

	private Execution X;

	public static int[] global_kobjValue; // indexed by domH
	public static int[] global_kcfaValue; // indexed by domI

	private void init() {
		if (isInitialized) return;
		domV = (DomV) ClassicProject.g().getTrgt("V");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		domC = (DomC) ClassicProject.g().getTrgt("C");

		relIM = (ProgramRel) ClassicProject.g().getTrgt("IM");
		relVH = (ProgramRel) ClassicProject.g().getTrgt("VH");
		
		relRefineH = (ProgramRel) ClassicProject.g().getTrgt("refineH");
		relRefineI = (ProgramRel) ClassicProject.g().getTrgt("refineI");
		relRefineM = (ProgramRel) ClassicProject.g().getTrgt("refineM");
		relRefineV = (ProgramRel) ClassicProject.g().getTrgt("refineV");
		relRefinableM = (ProgramRel) ClassicProject.g().getTrgt("refinableM");
		relRefinableV = (ProgramRel) ClassicProject.g().getTrgt("refinableV");
		relRefinableCH = (ProgramRel) ClassicProject.g().getTrgt("refinableCH");
		relRefinableCI = (ProgramRel) ClassicProject.g().getTrgt("refinableCI");
		
		relCC = (ProgramRel) ClassicProject.g().getTrgt("CC");
		relCH = (ProgramRel) ClassicProject.g().getTrgt("CH");
		relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relEpsilonM = (ProgramRel) ClassicProject.g().getTrgt("epsilonM");
		relKcfaSenM = (ProgramRel) ClassicProject.g().getTrgt("kcfaSenM");
		relKobjSenM = (ProgramRel) ClassicProject.g().getTrgt("kobjSenM");
		relCtxtCpyM = (ProgramRel) ClassicProject.g().getTrgt("ctxtCpyM");
		relEpsilonV = (ProgramRel) ClassicProject.g().getTrgt("epsilonV");

		mainMeth = Program.g().getMainMethod();
		
		maxIters = Integer.getInteger("chord.max.iters", 0);
		
		String ctxtKindStr = System.getProperty("chord.ctxt.kind", "ci");
		String instCtxtKindStr = System.getProperty(
			"chord.inst.ctxt.kind", ctxtKindStr);
		String statCtxtKindStr = System.getProperty(
			"chord.stat.ctxt.kind", ctxtKindStr);
		if (instCtxtKindStr.equals("ci")) {
			instCtxtKind = CTXTINS;
		} else if (instCtxtKindStr.equals("cs")) {
			instCtxtKind = KCFASEN;
		} else if (instCtxtKindStr.equals("co")) {
			instCtxtKind = KOBJSEN;
		} else
			assert false;
		if (statCtxtKindStr.equals("ci")) {
			statCtxtKind = CTXTINS;
		} else if (statCtxtKindStr.equals("cs")) {
			statCtxtKind = KCFASEN;
		} else if (statCtxtKindStr.equals("cc")) {
			statCtxtKind = CTXTCPY;
		} else
			assert false;

		kobjK = Integer.getInteger("chord.kobj.k", 1);
		assert (kobjK > 0);
		kcfaK = Integer.getInteger("chord.kcfa.k", 1);
		// assert (kobjK <= kcfaK+1)
		
		if (maxIters > 0) {
			assert (instCtxtKind == KOBJSEN ||
				instCtxtKind == KCFASEN ||
				statCtxtKind == KCFASEN);
		}
		isInitialized  = true;
	}
	
	private int getCtxtKind(jq_Method m) {
		if (m == mainMeth || m instanceof jq_ClassInitializer ||
				m.isAbstract())
			return CTXTINS;
		return m.isStatic() ? statCtxtKind : instCtxtKind;
	}

	// {04/19/10} Percy: experiment with different values of k
	private void setAdaptiveValues() {
		double senProb = X.getDoubleArg("senProb", 0);
		int randSeed = X.getIntArg("randSeed", 1);
		int kobjRange = X.getIntArg("kobjRange", 1);
		int kcfaRange = X.getIntArg("kcfaRange", 1);
		String inValuesPath = X.getStringArg("inValuesPath", null); // Specifies which values to use
		boolean keepOnlyReachable = X.getBooleanArg("keepOnlyReachable", false);

		// Link back results to where the in values came from
		if (inValuesPath != null) X.symlinkPath = inValuesPath+".results";

		// Save options
		X.putOption("version", 1);
		X.putOption("program", System.getProperty("chord.work.dir"));
		X.putOption("senProb", senProb);
		X.putOption("randSeed", randSeed);
		X.putOption("kobj", kobjK);
		X.putOption("kcfa", kcfaK);
		X.putOption("minH", kobjK);
		X.putOption("minI", kcfaK);
		X.putOption("kobjRange", kobjRange);
		X.putOption("kcfaRange", kcfaRange);
		X.putOption("numRefineIters", System.getProperty("chord.max.iters"));
		X.putOption("inValuesPath", inValuesPath);
		X.putOption("initK", kobjK+","+kcfaK);

		boolean useObjectSensitivity = "co".equals(System.getProperty("chord.inst.ctxt.kind", null));
		X.putOption("useObjectSensitivity", useObjectSensitivity);
			
		X.flushOptions();

		Random random = randSeed != 0 ? new Random(randSeed) : new Random();
		kobjValue = new int[domH.size()];
		kcfaValue = new int[domI.size()];

		// Only modify k values of sites in reachable methods
		ProgramRel relReachableM = (ProgramRel) ClassicProject.g().getTrgt("reachableM");
		Set<jq_Method> reachableMethods = new HashSet<jq_Method>();
		relReachableM.load();
		final Iterable<jq_Method> tuples = relReachableM.getAry1ValTuples();
		for (jq_Method m : tuples)
			reachableMethods.add(m);
		relReachableM.close();

		// The sites we actually care about
		Set<Inst> hSet = new HashSet<Inst>();
		Set<Inst> iSet = new HashSet<Inst>();
		for (Object inst : domH) {
			if (inst == null) continue; // Skip null
			if (keepOnlyReachable && !reachableMethods.contains(((Inst)inst).getMethod())) continue;
			hSet.add((Inst)inst);
		}
		for (Inst inst : domI) {
			if (keepOnlyReachable && !reachableMethods.contains(inst.getMethod())) continue;
			iSet.add(inst);
		}

		if (inValuesPath != null) {
			System.out.println("Reading k values from "+inValuesPath);
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inValuesPath)));
				String line;
				while ((line = in.readLine()) != null) {
					// Format: H32 2 or I3 5
					String[] tokens = line.split(" ");		 
					assert tokens.length == 2;
					int idx = Integer.parseInt(tokens[0].substring(1));
					int value = Integer.parseInt(tokens[1]);
					switch (tokens[0].charAt(0)) {
					case 'H': kobjValue[idx] = value; break;
					case 'I': kcfaValue[idx] = value; break;
					default: assert false;
					}
				}
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			System.out.println("Generating k values with senProb="+senProb);
			for (Inst inst : hSet) {
				int h = domH.indexOf(inst);
				kobjValue[h] = kobjK + sampleBinomial(random, kobjRange, senProb);
			}
			for (Inst inst : iSet) {
				int i = domI.indexOf(inst);
				kcfaValue[i] = kcfaK + sampleBinomial(random, kcfaRange, senProb);
			}
		}

		// Output k-values and strings
		PrintWriter datOut = OutDirUtils.newPrintWriter("inputs.dat");
		PrintWriter strOut = OutDirUtils.newPrintWriter("inputs.strings");
		for (Inst inst : hSet) {
			int h = domH.indexOf(inst);
			datOut.println("H"+h+" " + kobjValue[h]);
			strOut.println("H"+h+" " + inst.toVerboseStr());
		}
		for (Inst inst : iSet) {
			int i = domI.indexOf(inst);
			datOut.println("I"+i+" " + kcfaValue[i]);
			strOut.println("I"+i+" " + inst.toVerboseStr());
		}
		datOut.close();
		strOut.close();

		// Compute statistics on the k values actually used
		StatFig kobjFig = new StatFig();
		StatFig kcfaFig = new StatFig();
		for (Inst inst : hSet) {
			int h = domH.indexOf(inst);
			kobjFig.add(kobjValue[h]);
		}
		for (Inst inst : iSet) {
			int i = domI.indexOf(inst);
			kcfaFig.add(kcfaValue[i]);
		}
		X.output.put("avg.kobj", kobjFig.mean());
		X.output.put("avg.kcfa", kcfaFig.mean());
	}

	private int sampleBinomial(Random random, int n, double p) {
		int c = 0;
		for (int i = 0; i < n; i++)
			c += random.nextDouble() < p ? 1 : 0;
		return c;
	}

	public void run() {
		if (percy) X = Execution.v();	
		init();

		int numV = domV.size();
		int numM = domM.size();
		int numA = domH.getLastI() + 1;
		int numH = domH.size();
		int numI = domI.size();

		if (currIter == 0 || (global_kcfaValue != null || global_kobjValue != null)) {
			isCtxtSenV = new boolean[numV];
			// Set the context-sensitivity of various methods
			methKind = new int[numM];
			for (int mIdx = 0; mIdx < numM; mIdx++) {
				jq_Method mVal = domM.get(mIdx);
				methKind[mIdx] = (maxIters > 0) ? CTXTINS : getCtxtKind(mVal);
			}
			// Based on context-sensitivity of methods, set the context-sensitivity of variables inside the method
			for (int mIdx = 0; mIdx < numM; mIdx++) {
				if (methKind[mIdx] != CTXTINS) {
					jq_Method m = domM.get(mIdx);
					ControlFlowGraph cfg = m.getCFG();
					RegisterFactory rf = cfg.getRegisterFactory();
					for (Object o : rf) {
						Register v = (Register) o;
						if (v.getType().isReferenceType()) {
							int vIdx = domV.indexOf(v);
							// locals unused by any quad in cfg are not in domain V
							if (vIdx != -1)
								isCtxtSenV[vIdx] = true;
						}
					}
				}
			}
			kobjValue = new int[numA];
			HtoM = new int[numA]; // Which method is h located in?
			HtoQ = new Quad[numA];
			for (int i = 1; i < numA; i++) {
				kobjValue[i] = kobjK;
				Quad site = (Quad) domH.get(i);
				jq_Method m = site.getMethod();
				HtoM[i] = domM.indexOf(m);
				HtoQ[i] = site;
			}
			kcfaValue = new int[numI];
			ItoM = new int[numI]; // Which method is i located in?
			ItoQ = new Quad[numI];
			for (int i = 0; i < numI; i++) {
				kcfaValue[i] = kcfaK;
				Quad invk = domI.get(i);
				jq_Method m = invk.getMethod();
				ItoM[i] = domM.indexOf(m);
				ItoQ[i] = invk;
			}

			if (percy) {
				setAdaptiveValues();

				if (global_kcfaValue != null) {
					System.out.println("Using global_kcfaValue");
					System.arraycopy(global_kcfaValue, 0, kcfaValue, 0, kcfaValue.length);
				}
				if (global_kobjValue != null) {
					System.out.println("Using global_kobjValue");
					System.arraycopy(global_kobjValue, 0, kobjValue, 0, kobjValue.length);
				}
			}
		} else {
			refine();
		}

		validate();

		relIM.load();
		relVH.load();

		Ctxt epsilon = domC.setCtxt(emptyElems);
		epsilonCtxtSet = new ArraySet<Ctxt>(1);
		epsilonCtxtSet.add(epsilon);

		methToCtxts = new Set[numM];

		methToClrSites = new TIntArrayList[numM];
		methToRcvSites = new TIntArrayList[numM];
		methToClrMeths = new Set[numM];

		if (maxIters > 0) {
			int[] histogramI = new int[maxIters + 1];
			for (int i = 0; i < numI; i++) {
				histogramI[kcfaValue[i]]++;
			}
			for (int i = 0; i <= maxIters; i++) {
				System.out.println("I " + i + " " + histogramI[i]);
			}
		
			int[] histogramH = new int[maxIters + 1];
			for (int i = 0; i < numH; i++) {
				histogramH[kobjValue[i]]++;
			}
			for (int i = 0; i <= maxIters; i++) {
				Messages.log("H " + i + " " + histogramH[i]);
			}

			// Output values
			PrintWriter out = OutDirUtils.newPrintWriter("inputs.dat");
			for (int h = 0; h < numH; h++)
				if (kobjValue[h] > 0) out.println("H"+h+" " + kobjValue[h]);
			for (int i = 0; i < numI; i++)
				if (kcfaValue[i] > 0) out.println("I"+i+" " + kcfaValue[i]);
			out.close();
		}
		
		// Do the heavy crunching
		doAnalysis();

		relIM.close();
		relVH.close();

		// Populate domC
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad invk = (Quad) domI.get(iIdx);
			jq_Method meth = invk.getMethod();
			int mIdx = domM.indexOf(meth);
			Set<Ctxt> ctxts = methToCtxts[mIdx];
			int k = kcfaValue[iIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, invk, oldElems);
				domC.setCtxt(newElems);
			}
		}
		for (int hIdx = 1; hIdx < numA; hIdx++) {
			Quad inst = (Quad) domH.get(hIdx);
			jq_Method meth = inst.getMethod();
			int mIdx = domM.indexOf(meth);
			Set<Ctxt> ctxts = methToCtxts[mIdx];
			int k = kobjValue[hIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, inst, oldElems);
				domC.setCtxt(newElems);
			}
		}
		domC.save();

		int numC = domC.size();

		boolean isLastIter = (currIter == maxIters);
		
		relCC.zero();
		relCI.zero();
		if (!isLastIter)
			relRefinableCI.zero();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad invk = (Quad) domI.get(iIdx);
			jq_Method meth = invk.getMethod();
			Set<Ctxt> ctxts = methToCtxts[domM.indexOf(meth)];
			int k = kcfaValue[iIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, invk, oldElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCC.add(oldCtxt, newCtxt);
				if (!isLastIter && newElems.length < oldElems.length + 1) {
					relRefinableCI.add(oldCtxt, invk);
				}
				relCI.add(newCtxt, invk);
			}
		}
		relCI.save();
		if (!isLastIter)
			relRefinableCI.save();

		assert (domC.size() == numC);

		relCH.zero();
		if (!isLastIter)
			relRefinableCH.zero();
		for (int hIdx = 1; hIdx < numA; hIdx++) {
			Quad inst = (Quad) domH.get(hIdx);
			jq_Method meth = inst.getMethod();
			int mIdx = domM.indexOf(meth);
			Set<Ctxt> ctxts = methToCtxts[mIdx];
			int k = kobjValue[hIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, inst, oldElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCC.add(oldCtxt, newCtxt);
				if (!isLastIter && newElems.length < oldElems.length + 1) {
					relRefinableCH.add(oldCtxt, inst);
				}
				relCH.add(newCtxt, inst);
			}
		}
		relCH.save();
		if (!isLastIter)
			relRefinableCH.save();

		assert (domC.size() == numC);

		relCC.save();

		relEpsilonM.zero();
		relKcfaSenM.zero();
		relKobjSenM.zero();
		relCtxtCpyM.zero();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			int kind = methKind[mIdx];
			switch (kind) {
			case CTXTINS:
				relEpsilonM.add(mIdx);
				break;
			case KOBJSEN:
				relKobjSenM.add(mIdx);
				break;
			case KCFASEN:
				relKcfaSenM.add(mIdx);
				break;
			case CTXTCPY:
				relCtxtCpyM.add(mIdx);
				break;
			default:
				assert false;
			}
		}
		relEpsilonM.save();
		relKcfaSenM.save();
		relKobjSenM.save();
		relCtxtCpyM.save();

		relEpsilonV.zero();
		for (int v = 0; v < numV; v++) {
			if (!isCtxtSenV[v])
				relEpsilonV.add(v);
		}
		relEpsilonV.save();
		
		relRefinableV.zero();
		for (int v = 0; v < numV; v++) {
			if (!isCtxtSenV[v]) {
				Register var = domV.get(v);
				jq_Method meth = domV.getMethod(var);
				if (getCtxtKind(meth) != CTXTINS) {
					relRefinableV.add(var);
				}
			}
		}
		relRefinableV.save();
		
		relRefinableM.zero();
		for (int m = 0; m < numM; m++) {
			if (methKind[m] == CTXTINS) {
				jq_Method meth = domM.get(m);
				if (getCtxtKind(meth) != CTXTINS) {
					relRefinableM.add(m);
				}
			}
		}
		relRefinableM.save();

		currIter++;
	}

	private void refine() {
		relRefineH.load();
		Iterable<Quad> heapInsts =
			relRefineH.getAry1ValTuples();
		for (Quad inst : heapInsts) {
			int hIdx = domH.indexOf(inst);
			kobjValue[hIdx]++;
		}
		relRefineH.close();
		relRefineI.load();
		Iterable<Quad> invkInsts =
			relRefineI.getAry1ValTuples();
		for (Quad inst : invkInsts) {
			int iIdx = domI.indexOf(inst);
			kcfaValue[iIdx]++;
		}
		relRefineI.close();
		relRefineV.load();
		Iterable<Register> vars = relRefineV.getAry1ValTuples();
		for (Register var : vars) {
			int v = domV.indexOf(var);
			assert (!isCtxtSenV[v]);
			isCtxtSenV[v] = true;
		}
		relRefineV.close();
		relRefineM.load();
		Iterable<jq_Method> meths = relRefineM.getAry1ValTuples();
		for (jq_Method meth : meths) {
			int m = domM.indexOf(meth);
			assert (methKind[m] == CTXTINS);
			methKind[m] = getCtxtKind(meth);
			assert (methKind[m] != CTXTINS);
		}
		relRefineM.close();
	}
	
	private static boolean contains(Quad[] elems, Quad q) {
		for (Quad e : elems) {
			if (e == q)
				return true;
		}
		return false;
	}

	private void validate() {
		// check that the main jq_Method and each class initializer method
		// and each method without a body is not asked to be analyzed
		// context sensitively.
		int numM = domM.size();
		for (int m = 0; m < numM; m++) {
			int kind = methKind[m];
			if (kind != CTXTINS) {
				jq_Method meth = domM.get(m);
				assert (meth != mainMeth);
				assert (!(meth instanceof jq_ClassInitializer));
				if (kind == KOBJSEN) {
					assert (!meth.isStatic());
				} else if (kind == CTXTCPY) {
					assert (meth.isStatic());
				}
			}
		}
		// check that each variable in a context insensitive method is
		// not asked to be treated context sensitively.
		int numV = domV.size();
		for (int v = 0; v < numV; v++) {
			if (isCtxtSenV[v]) {
				Register var = domV.get(v);
				jq_Method meth = domV.getMethod(var);
				int m = domM.indexOf(meth);
				int kind = methKind[m];
				assert (kind != CTXTINS);
			}
		}
	}

	private void doAnalysis() {
		Set<jq_Method> roots = new HashSet<jq_Method>();
		Map<jq_Method, Set<jq_Method>> methToPredsMap = new HashMap<jq_Method, Set<jq_Method>>();
		for (int mIdx = 0; mIdx < domM.size(); mIdx++) { // For each method...
			jq_Method meth = domM.get(mIdx);
			int kind = methKind[mIdx];
			switch (kind) {
			case CTXTINS:
			{
				roots.add(meth);
				methToPredsMap.put(meth, emptyMethSet);
				methToCtxts[mIdx] = epsilonCtxtSet;
				break;
			}
			case KCFASEN:
			{
				Set<jq_Method> predMeths = new HashSet<jq_Method>();
				TIntArrayList clrSites = new TIntArrayList();
				for (Quad invk : getCallers(meth)) {
					predMeths.add(invk.getMethod()); // Which method can point to this method...?
					int iIdx = domI.indexOf(invk);
					clrSites.add(iIdx); // sites that can call me
				}
				methToClrSites[mIdx] = clrSites;
				methToPredsMap.put(meth, predMeths);
				methToCtxts[mIdx] = emptyCtxtSet;
				break;
			}
			case KOBJSEN:
			{
				Set<jq_Method> predMeths = new HashSet<jq_Method>();
				TIntArrayList rcvSites = new TIntArrayList();
				ControlFlowGraph cfg = meth.getCFG();
				Register thisVar = cfg.getRegisterFactory().get(0);
				Iterable<Quad> pts = getPointsTo(thisVar);
				for (Quad inst : pts) {
					predMeths.add(inst.getMethod());
					int hIdx = domH.indexOf(inst);
					rcvSites.add(hIdx);
				}
				methToRcvSites[mIdx] = rcvSites;
				methToPredsMap.put(meth, predMeths);
				methToCtxts[mIdx] = emptyCtxtSet;
				break;
			}
			case CTXTCPY:
			{
				Set<jq_Method> predMeths = new HashSet<jq_Method>();
				for (Quad invk : getCallers(meth)) {
					predMeths.add(invk.getMethod());
				}
				methToClrMeths[mIdx] = predMeths;
				methToPredsMap.put(meth, predMeths);
				methToCtxts[mIdx] = emptyCtxtSet;
				break;
			}
			default:
				assert false;
			}
		}
		process(roots, methToPredsMap);
	}

	// Compute all the contexts that each method can be called in
	private void process(Set<jq_Method> roots,
			Map<jq_Method, Set<jq_Method>> methToPredsMap) {
		IGraph<jq_Method> graph = new MutableGraph<jq_Method>(roots, methToPredsMap, null);
		List<Set<jq_Method>> sccList = graph.getTopSortedSCCs();
		if (Config.verbose >= 2) System.out.println("numSCCs: " + sccList.size());
		for (int i = 0; i < sccList.size(); i++) { // For each SCC...
			Set<jq_Method> scc = sccList.get(i);
			if (Config.verbose >= 2)
				System.out.println("Processing SCC #" + i + " of size: " + scc.size());
			if (scc.size() == 1) { // Singleton
				jq_Method cle = scc.iterator().next();
				if (roots.contains(cle))
					continue;
				if (!graph.hasEdge(cle, cle)) {
					int cleIdx = domM.indexOf(cle);
					methToCtxts[cleIdx] = getNewCtxts(cleIdx);
					continue;
				}
			}
			for (jq_Method cle : scc) {
				assert (!roots.contains(cle));
			}
			boolean changed = true;
			for (int count = 0; changed; count++) { // Iterate...
				if (Config.verbose >= 2)
					System.out.println("\tIteration  #" + count);
				changed = false;
				for (jq_Method cle : scc) { // For each node (method) in SCC
					int mIdx = domM.indexOf(cle);
					Set<Ctxt> newCtxts = getNewCtxts(mIdx);
					if (!changed) {
						Set<Ctxt> oldCtxts = methToCtxts[mIdx];
						if (newCtxts.size() > oldCtxts.size())
							changed = true;
						else {
							for (Ctxt ctxt : newCtxts) {
								if (!oldCtxts.contains(ctxt)) {
									changed = true;
									break;
								}
							}
						}
					}
					methToCtxts[mIdx] = newCtxts;
				}
			}
		}
	}

	private Iterable<Quad> getPointsTo(Register var) {
		RelView view = relVH.getView();
		view.selectAndDelete(0, var);
		return view.getAry1ValTuples();
	}

	private Iterable<Quad> getCallers(jq_Method meth) {
		RelView view = relIM.getView();
		view.selectAndDelete(1, meth);
		return view.getAry1ValTuples();
	}

	private Quad[] combine(int k, Quad inst, Quad[] elems) {
		int oldLen = elems.length;
		int newLen = Math.min(k - 1, oldLen) + 1;
		Quad[] newElems = new Quad[newLen];
		if (newLen > 0) newElems[0] = inst;
		if (newLen > 1)
			System.arraycopy(elems, 0, newElems, 1, newLen - 1);
		return newElems;
	}

	private Set<Ctxt> getNewCtxts(int cleIdx) { // Update contexts for this method (callee)
		final Set<Ctxt> newCtxts = new HashSet<Ctxt>();
		int kind = methKind[cleIdx];
		switch (kind) {
		case KCFASEN:
		{
			TIntArrayList invks = methToClrSites[cleIdx]; // which call sites point to me
			int n = invks.size();
			for (int i = 0; i < n; i++) {
				int iIdx = invks.get(i);
				Quad invk = ItoQ[iIdx];
				int k = kcfaValue[iIdx];
				int clrIdx = ItoM[iIdx];
				Set<Ctxt> clrCtxts = methToCtxts[clrIdx]; // method of caller
				for (Ctxt oldCtxt : clrCtxts) {
					Quad[] oldElems = oldCtxt.getElems();
					Quad[] newElems = combine(k, invk, oldElems); // Append
					Ctxt newCtxt = domC.setCtxt(newElems);
					newCtxts.add(newCtxt);
				}
			}
			break;
		}
		case KOBJSEN:
		{
			TIntArrayList rcvs = methToRcvSites[cleIdx];
			int n = rcvs.size();
			for (int i = 0; i < n; i++) {
				int hIdx = rcvs.get(i);
				Quad rcv = HtoQ[hIdx];
				int k = kobjValue[hIdx];
				int clrIdx = HtoM[hIdx];
				Set<Ctxt> rcvCtxts = methToCtxts[clrIdx];
				for (Ctxt oldCtxt : rcvCtxts) {
					Quad[] oldElems = oldCtxt.getElems();
					Quad[] newElems = combine(k, rcv, oldElems);
					Ctxt newCtxt = domC.setCtxt(newElems);
					newCtxts.add(newCtxt);
				}
			}
			break;
		}
		case CTXTCPY:
		{
			Set<jq_Method> clrs = methToClrMeths[cleIdx];
			for (jq_Method clr : clrs) {
				int clrIdx = domM.indexOf(clr);
				Set<Ctxt> clrCtxts = methToCtxts[clrIdx];
				newCtxts.addAll(clrCtxts);
			}
			break;
		}
		default:
			assert false;
		}
		return newCtxts;
	}

	public static String getCspaKind() {
		String ctxtKindStr = System.getProperty("chord.ctxt.kind", "ci");
		String instCtxtKindStr = System.getProperty("chord.inst.ctxt.kind", ctxtKindStr);
		String statCtxtKindStr = System.getProperty("chord.stat.ctxt.kind", ctxtKindStr);
		int instCtxtKind, statCtxtKind;
		if (instCtxtKindStr.equals("ci")) {
			instCtxtKind = CtxtsAnalysis.CTXTINS;
		} else if (instCtxtKindStr.equals("cs")) {
			instCtxtKind = CtxtsAnalysis.KCFASEN;
		} else if (instCtxtKindStr.equals("co")) {
			instCtxtKind = CtxtsAnalysis.KOBJSEN;
		} else
			throw new ChordRuntimeException();
		if (statCtxtKindStr.equals("ci")) {
			statCtxtKind = CtxtsAnalysis.CTXTINS;
		} else if (statCtxtKindStr.equals("cs")) {
			statCtxtKind = CtxtsAnalysis.KCFASEN;
		} else if (statCtxtKindStr.equals("cc")) {
			statCtxtKind = CtxtsAnalysis.CTXTCPY;
		} else
			throw new ChordRuntimeException();
		String cspaKind;
		if (instCtxtKind == CtxtsAnalysis.CTXTINS &&
			statCtxtKind == CtxtsAnalysis.CTXTINS)
			cspaKind = "cspa-0cfa-dlog";
		else if (instCtxtKind == CtxtsAnalysis.KOBJSEN &&
			statCtxtKind == CtxtsAnalysis.CTXTCPY)
			cspaKind = "cspa-kobj-dlog";
		else if (instCtxtKind == CtxtsAnalysis.KCFASEN &&
			statCtxtKind == CtxtsAnalysis.KCFASEN)
			cspaKind = "cspa-kcfa-dlog";
		else
			cspaKind = "cspa-hybrid-dlog";
		return cspaKind;
	}

	jq_Type h2t(Quad h) {
		Operator op = h.getOperator();
		if (op instanceof New) 
			return New.getType(h).getType();
		else if (op instanceof NewArray)
			return NewArray.getType(h).getType();
		else if (op instanceof MultiNewArray)
			return MultiNewArray.getType(h).getType();
		else
			return null;
	}
	String hstr(Quad h) {
		String path = new File(h.toJavaLocStr()).getName();
		jq_Type t = h2t(h);
		return path+"("+(t == null ? "?" : t.shortName())+")";
	}
	String istr(Quad i) {
		String path = new File(i.toJavaLocStr()).getName();
		jq_Method m = InvokeStatic.getMethod(i).getMethod();
		return path+"("+m.getName()+")";
	}
	String jstr(Quad j) { return isAlloc(j) ? hstr(j) : istr(j); }
	String estr(Quad e) {
		String path = new File(e.toJavaLocStr()).getName();
		Operator op = e.getOperator();
		return path+"("+op+")";
	}
	String cstr(Ctxt c) {
		StringBuilder buf = new StringBuilder();
		buf.append('{');
		for (int i = 0; i < c.length(); i++) {
			if (i > 0) buf.append(" | ");
			Quad q = c.get(i);
			buf.append(isAlloc(q) ? hstr(q) : istr(q));
		}
		buf.append('}');
		return buf.toString();
	}
	String fstr(jq_Field f) { return f.getDeclaringClass()+"."+f.getName(); }
	String vstr(Register v) { return v+"@"+mstr(domV.getMethod(v)); }
	String mstr(jq_Method m) { return m.getDeclaringClass().shortName()+"."+m.getName(); }
	boolean isAlloc(Quad q) { return domH.indexOf(q) != -1; }
}
