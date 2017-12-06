/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses.rhs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator.Invoke;

import chord.util.tuple.object.Pair;
import chord.program.Program;
import chord.program.Location;
import chord.analyses.alias.ICICG;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.util.ArraySet;
import chord.util.Alarm;

/**
 * Implementation of the Reps-Horwitz-Sagiv algorithm for context-sensitive
 * dataflow analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class RHSAnalysis<PE extends IEdge, SE extends IEdge>
		extends JavaAnalysis {
	protected static boolean DEBUG = false;

	protected List<Pair<Location, PE>> workList = new ArrayList<Pair<Location, PE>>();
	protected Map<Inst, Set<PE>> pathEdges = new HashMap<Inst, Set<PE>>();
	protected Map<jq_Method, Set<SE>> summEdges = new HashMap<jq_Method, Set<SE>>();
	protected DomI domI;
	protected DomM domM;
	protected ICICG cicg;
	protected Map<Quad, Location> invkQuadToLoc = new HashMap<Quad, Location>();
	protected Map<jq_Method, Set<Quad>> callersMap = new HashMap<jq_Method, Set<Quad>>();
	protected Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();
	protected boolean isInited;
	protected final boolean doMerge = doMerge();
	protected final boolean isForward = isForward();

	// get the initial set of path edges
	public abstract Set<Pair<Location, PE>> getInitPathEdges();

	// get the path edge(s) in callee for target method m called from call site q
	// with caller path edge pe
	public abstract PE getInitPathEdge(Quad q, jq_Method m, PE pe);

	// get outgoing path edge(s) from q, given incoming path edge pe into q.
	//  q is guaranteed to not be an invoke statement, return statement,
	// entry basic block, or exit basic block
	// the set returned can be reused by client
	public abstract PE getMiscPathEdge(Quad q, PE pe);
 
	// q is an invoke statement and m is the target method.
	// get path edge to successor of q given path edge into q and summary
	// edge of a target method m of the call site q
	// returns null if the path edge into q is not compatible with the
	// summary edge
	public abstract PE getInvkPathEdge(Quad q, PE clrPE, jq_Method m, SE tgtSE);

	public abstract PE getCopy(PE pe);

	// m is a method and pe is a path edge from entry to exit of m
	// (in case of forward analysis) or vice versa (in case of backward analysis)
	// that must be lifted to a summary edge
	public abstract SE getSummaryEdge(jq_Method m, PE pe);

	/**
	 * Determines whether this analysis should merge path edges at each
	 * program point that have the same source state but different
	 * target states and, likewise, summary edges of each method that
	 * have the same source state and different target states.
	 *
	 * @return	true iff (path or summary) edges with the same source
	 *			state and different target states should be merged.
	 */
	public abstract boolean doMerge();

	/**
	 * Determines whether this analysis is a forward analysis (as opposed
	 * to a backward analysis.
	 *
	 * @return	true iff this analysis is a forward analysis.
	 */
	public abstract boolean isForward();

	/**
	 * Provides the call graph to be used by the analysis.
	 *
	 * @return	The call graph to be used by the analysis.
	 */
	public abstract ICICG getCallGraph();

	private Set<Quad> getCallers(jq_Method m) {
		Set<Quad> callers = callersMap.get(m);
		if (callers == null) {
			callers = cicg.getCallers(m);
			callersMap.put(m, callers);
		}
		return callers;
	}

	private Set<jq_Method> getTargets(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets;
	}

	private int timeout = Integer.getInteger("chord.rhs.timeout", 300000);
	private Alarm alarm;

	protected void done() {
		if (timeout > 0)
			alarm.doneAllPasses();
	}

	protected void init() {
		if (isInited)
			return;
		if (timeout > 0) {
			alarm = new Alarm(timeout);
			alarm.initAllPasses();
		}
		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		cicg = getCallGraph();
		isInited = true;
	}

	/**
	 * Run an instance of the analysis afresh.
	 * Clients may call this method multiple times from their {@link #run()}
	 * method.  Clients must override method {@link #getInitPathEdges()} to
	 * return a new "seed" each time they call this method.
	 */
	protected void runPass() throws TimeoutException {
		init();
		if (timeout > 0)
			alarm.initNewPass();
		// clear these sets since client may call this method multiple times
		workList.clear();
		summEdges.clear();
		pathEdges.clear();
		Set<Pair<Location, PE>> initPEs = getInitPathEdges();
		for (Pair<Location, PE> pair : initPEs) {
			Location loc = pair.val0;
			PE pe = pair.val1;
			addPathEdge(loc, pe);
		}
		propagate();
	}

	protected void printSummaries() {
		for (jq_Method m : summEdges.keySet()) {
			System.out.println("Summaries of method " + m);
			Set<SE> seSet = summEdges.get(m);
			if (seSet != null) {
				for (SE se : seSet)
					System.out.println("\t" + se);
			}
		}
		System.out.println();
	}
		
	protected jq_Method currentMethod;
	protected BasicBlock currentBB;

	/**
	 * Propagate forward or backward until fixpoint is reached.
	 */
	private void propagate() throws TimeoutException {
		while (!workList.isEmpty()) {
			if (timeout > 0 && alarm.passTimedOut()) {
				System.out.println("TIMED OUT");
				throw new TimeoutException();
			}
			if (DEBUG) {
				System.out.println("WORKLIST:");
				for (Pair<Location, PE> pair : workList)
					System.out.println("\t" + pair);
			}
			int last = workList.size() - 1;
			Pair<Location, PE> pair = workList.remove(last);
			Location loc = pair.val0;
			PE pe = pair.val1;
			Quad q = loc.q;
			jq_Method m = loc.m;
			BasicBlock bb = loc.bb;
			currentMethod = m;
			currentBB = bb;
			if (DEBUG) System.out.println("Processing loc: " + loc + " PE: " + pe);
			if (q == null) {
				// method entry or method exit
				if (bb.isEntry()) {
					if (isForward) {
						for (Object o : bb.getSuccessorsList()) {
							BasicBlock bb2 = (BasicBlock) o;
							Quad q2;
							int q2Idx;
							if (bb2.size() == 0) {
								q2 = null;
								q2Idx = -1;
							} else {
								q2 = bb2.getQuad(0);
								q2Idx = 0;
							}
							Location loc2 = new Location(m, bb2, q2Idx, q2);
							PE pe2 = doMerge ? getCopy(pe) : pe;
							addPathEdge(loc2, pe2);
						}
					} else {
						processEntry(m, pe);
					}
				} else {
					assert (bb.isExit());
					if (isForward) {
						processExit(m, pe);
					} else {
						for (Object o : bb.getPredecessorsList()) {
							BasicBlock bb2 = (BasicBlock) o;
							Quad q2;
							int q2Idx;
							int n = bb2.size();
							if (n == 0) {
								q2 = null;
								q2Idx = -1;
							} else {
								q2 = bb2.getQuad(n - 1);
								q2Idx = n - 1;
							}
							Location loc2 = new Location(m, bb2, q2Idx, q2);
							PE pe2 = doMerge ? getCopy(pe) : pe;
							addPathEdge(loc2, pe2);
						}
					}
				}
			} else {
				// invoke or misc quad
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					Set<jq_Method> targets = getTargets(q);
					if (targets.isEmpty()) {
						PE pe2 = doMerge ? getCopy(pe) : pe;
						propagatePEtoPE(m, bb, loc.qIdx, pe2);
					} else {
						for (jq_Method m2 : targets) {
							if (DEBUG) System.out.println("\tTarget: " + m2);
							PE pe2 = getInitPathEdge(q, m2, pe);
							ControlFlowGraph cfg2 = m2.getCFG();
							BasicBlock bb2 = isForward ? cfg2.entry() : cfg2.exit();
							Location loc2 = new Location(m2, bb2, -1, null);
							addPathEdge(loc2, pe2);
							Set<SE> seSet = summEdges.get(m2);
							if (seSet == null) {
								if (DEBUG) System.out.println("\tSE set empty");
								continue;
							}
							for (SE se : seSet) {
								if (DEBUG) System.out.println("\tTesting SE: " + se);
								if (propagateSEtoPE(pe, loc, m2, se)) {
									if (DEBUG) System.out.println("\tMatched");
									if (doMerge)
										break;
								} else {
									if (DEBUG) System.out.println("\tDid not match");
								}
							}
						}
					}
				} else {
					PE pe2 = getMiscPathEdge(q, pe);
					propagatePEtoPE(m, bb, loc.qIdx, pe2);
				}
			}
		}
	}

	// called by backward analysis
	private void processEntry(jq_Method m, PE pe) {
		SE se = getSummaryEdge(m, pe);
		Set<SE> seSet = summEdges.get(m);
		if (DEBUG) System.out.println("\tChecking if " + m + " has SE: " + se);
		SE seToAdd = se;
		if (seSet == null) {
			seSet = new HashSet<SE>();
			summEdges.put(m, seSet);
			seSet.add(se);
			if (DEBUG) System.out.println("\tNo, adding it as first SE");
		} else if (doMerge) {
			boolean matched = false;
			for (SE se2 : seSet) {
				if (se2.matchesSrcNodeOf(se)) {
					if (DEBUG) System.out.println("\tNo, but matches SE: " + se2);
					boolean changed = se2.mergeWith(se);
					if (DEBUG) System.out.println("\tNew SE after merge: " + se2);
					if (!changed) {
						if (DEBUG) System.out.println("\tExisting SE did not change");
						return;
					}
					if (DEBUG) System.out.println("\tExisting SE changed");
					// se2 is already in summEdges(m), so no need to add it
					seToAdd = se2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG) System.out.println("\tNo, adding");
				seSet.add(se);
			}
		} else if (!seSet.add(se)) {
			if (DEBUG) System.out.println("\tYes, not adding");
			return;
		}
		for (Quad q2 : getCallers(m)) {
			jq_Method m2 = q2.getMethod();
			if (DEBUG) System.out.println("\tCaller: " + q2 + " in " + m2);
			Set<PE> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			// make a copy because propagateSEtoPE might add a
			// path edge to this set itself
			// TODO: fix this eventually
			peSet = new ArraySet<PE>(peSet);
			Location loc2 = invkQuadToLoc.get(q2);
			for (PE pe2 : peSet) {
				if (DEBUG) System.out.println("\tTesting PE: " + pe2);
				boolean match = propagateSEtoPE(pe2, loc2, m, seToAdd);
				if (match) {
					if (DEBUG) System.out.println("\tMatched");
				} else {
					if (DEBUG) System.out.println("\tDid not match");
				}
			}
		}
	}
	
	// called by forward analysis
	private void processExit(jq_Method m, PE pe) {
		SE se = getSummaryEdge(m, pe);
		Set<SE> seSet = summEdges.get(m);
		if (DEBUG) System.out.println("\tChecking if " + m + " has SE: " + se);
		SE seToAdd = se;
		if (seSet == null) {
			seSet = new HashSet<SE>();
			summEdges.put(m, seSet);
			seSet.add(se);
			if (DEBUG) System.out.println("\tNo, adding it as first SE");
		} else if (doMerge) {
			boolean matched = false;
			for (SE se2 : seSet) {
				if (se2.matchesSrcNodeOf(se)) {
					if (DEBUG) System.out.println("\tNo, but matches SE: " + se2);
					boolean changed = se2.mergeWith(se);
					if (DEBUG) System.out.println("\tNew SE after merge: " + se2);
					if (!changed) {
						if (DEBUG) System.out.println("\tExisting SE did not change");
						return;
					}
					if (DEBUG) System.out.println("\tExisting SE changed");
					// se2 is already in summEdges(m), so no need to add it
					seToAdd = se2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG) System.out.println("\tNo, adding");
				seSet.add(se);
			}
		} else if (!seSet.add(se)) {
			if (DEBUG) System.out.println("\tYes, not adding");
			return;
		}
		Program program = Program.g();
		for (Quad q2 : getCallers(m)) {
			jq_Method m2 = q2.getMethod();
			if (DEBUG) System.out.println("\tCaller: " + q2 + " in " + m2);
			Set<PE> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			// make a copy because propagateSEtoPE might add a
			// path edge to this set itself
			// TODO: fix this eventually
			List<PE> peList = new ArrayList<PE>(peSet);
			Location loc2 = invkQuadToLoc.get(q2);
			for (PE pe2 : peList) {
				if (DEBUG) System.out.println("\tTesting PE: " + pe2);
				boolean match = propagateSEtoPE(pe2, loc2, m, seToAdd);
				if (match) {
					if (DEBUG) System.out.println("\tMatched");
				} else {
					if (DEBUG) System.out.println("\tDid not match");
				}
			}
		}
	}

	private void addPathEdge(Location loc, PE pe) {
		if (DEBUG) System.out.println("\tChecking if " + loc + " has PE: " + pe);
		Quad q = loc.q;
		Inst i = (q != null) ? q : loc.bb;
		Set<PE> peSet = pathEdges.get(i);
		PE peToAdd = pe;
		if (peSet == null) {
			peSet = new HashSet<PE>();
			pathEdges.put(i, peSet);
			if (q != null && (q.getOperator() instanceof Invoke))
				invkQuadToLoc.put(q, loc);
			peSet.add(pe);
			if (DEBUG) System.out.println("\tNo, adding it as first PE");
		} else if (doMerge) {
			boolean matched = false;
			for (PE pe2 : peSet) {
				if (pe2.matchesSrcNodeOf(pe)) {
					if (DEBUG) System.out.println("\tNo, but matches PE: " + pe2);
					boolean changed = pe2.mergeWith(pe);
					if (DEBUG) System.out.println("\tNew PE after merge: " + pe2); 
					if (!changed) {
						if (DEBUG) System.out.println("\tExisting PE did not change");
						return;
					}
					if (DEBUG) System.out.println("\tExisting PE changed");
					// pe2 is already in pathEdges(i), so no need to add it;
					// but it may or may not be in workList
					for (int j = workList.size() - 1; j >= 0; j--) {
						Pair<Location, PE> pair = workList.get(j);
						PE pe3 = pair.val1;
						if (pe3 == pe2)
							return;
					}
					peToAdd = pe2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG) System.out.println("\tNo, adding");
				peSet.add(pe);
			}
		} else if (!peSet.add(pe)) {
			if (DEBUG) System.out.println("\tYes, not adding");
			return;
		}
		assert (peToAdd != null);
		if (DEBUG) System.out.println("\tAlso adding to worklist");
		Pair<Location, PE> pair = new Pair<Location, PE>(loc, peToAdd);
		workList.add(pair);
	}

	private void propagatePEtoPE(jq_Method m, BasicBlock bb, int qIdx, PE pe) {
		if (isForward) {
			// forward propagate
			if (qIdx != bb.size() - 1) {
				int q2Idx = qIdx + 1;
				Quad q2 = bb.getQuad(q2Idx);
				Location loc2 = new Location(m, bb, q2Idx, q2);
				addPathEdge(loc2, pe);
				return;
			}
			boolean isFirst = true;
			for (Object o : bb.getSuccessorsList()) {
				BasicBlock bb2 = (BasicBlock) o;
				int q2Idx;
				Quad q2;
				if (bb2.size() == 0) {
					q2Idx = -1;
					q2 = null;
				} else {
					q2Idx = 0;
					q2 = bb2.getQuad(0);
				}
				Location loc2 = new Location(m, bb2, q2Idx, q2);
				PE pe2;
				if (!doMerge)
					pe2 = pe;
				else {
					if (isFirst) {
						pe2 = pe;
						isFirst = false;
					} else
						pe2 = getCopy(pe);
				}
				addPathEdge(loc2, pe2);
			}
		} else {
			// backward propagate
			if (qIdx != 0) {
				int q2Idx = qIdx - 1;
				Quad q2 = bb.getQuad(q2Idx);
				Location loc2 = new Location(m, bb, q2Idx, q2);
				addPathEdge(loc2, pe);
				return;
			}
			boolean isFirst = true;
			for (Object o : bb.getPredecessorsList()) {
				BasicBlock bb2 = (BasicBlock) o;
				int q2Idx;
				Quad q2;
				int n = bb2.size();
				if (n == 0) {
					q2Idx = -1;
					q2 = null;
				} else {
					q2Idx = n - 1;
					q2 = bb2.getQuad(n - 1);
				}
				Location loc2 = new Location(m, bb2, q2Idx, q2);
				PE pe2;
				if (!doMerge)
					pe2 = pe;
				else {
					if (isFirst) {
						pe2 = pe;
						isFirst = false;
					} else
						pe2 = getCopy(pe);
				}
				addPathEdge(loc2, pe2);
			}
		}
	}

	private boolean propagateSEtoPE(PE clrPE, Location loc, jq_Method tgtM, SE tgtSE) {
		Quad q = loc.q;
		PE pe2 = getInvkPathEdge(q, clrPE, tgtM, tgtSE);
		if (pe2 == null)
			return false;
		   propagatePEtoPE(loc.m, loc.bb, loc.qIdx, pe2);
		return true;
	}
}

