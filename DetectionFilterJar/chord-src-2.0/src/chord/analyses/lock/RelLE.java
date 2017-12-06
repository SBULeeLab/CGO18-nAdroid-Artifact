/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.lock;

import gnu.trove.TIntArrayList;

import java.util.HashSet;
import java.util.Set;


import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.Monitor.MONITORENTER;
import chord.analyses.heapacc.DomE;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/*
 * Xinwei modified this class to add the exception basic block into scope.
 */

/**
 * Relation containing each tuple (e,l) such that statement e
 * that accesses (reads or writes) an instance field, a
 * static field, or an array element is lexically enclosed in
 * the synchronized block or synchronized method that acquires
 * the lock at point l.
 * <p>
 * A statement may be lexically enclosed in multiple synchronized
 * blocks but in at most one synchronized method (i.e. its
 * containing method).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "LE",
	sign = "L0,E0:L0_E0"
)
public class RelLE extends ProgramRel implements IMethodVisitor {
	private Set<BasicBlock> visited = new HashSet<BasicBlock>();
	private DomE domE;
	private DomL domL;
	public void init() {
		domL = (DomL) doms[0];
		domE = (DomE) doms[1];
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		BasicBlock entry = cfg.entry();
		TIntArrayList locks = new TIntArrayList();
		if (m.isSynchronized()) {
			int lIdx = domL.indexOf(entry);
			assert (lIdx >= 0);
			locks.add(lIdx);
		}
		process(entry, locks);
		visited.clear();
	}
	private void process(BasicBlock bb, TIntArrayList locks) {
		int n = bb.size();
		int k = locks.size();
		
		// store the locks into oldLocks
		// because program may enter an exception basic block
		// without go through all the instructions of the current basic block
		TIntArrayList oldLocks = new TIntArrayList(k);
		for (int j = 0; j < k; j++)
			oldLocks.add(locks.get(j));
		
		for (int i = 0; i < n; i++) {
			Quad q = bb.getQuad(i);
			Operator op = q.getOperator();
			if (op instanceof Monitor) {
				if (op instanceof MONITORENTER) {
					TIntArrayList locks2 = new TIntArrayList(k + 1);
					for (int j = 0; j < k; j++)
						locks2.add(locks.get(j));
					int lIdx = domL.indexOf(q);
					assert (lIdx >= 0);
					locks2.add(lIdx);
					locks = locks2;
					k++;
				} else {
					k--;
					TIntArrayList locks2 = new TIntArrayList(k);
					for (int j = 0; j < k; j++)
						locks2.add(locks.get(j));
					locks = locks2;
				}
			} else if (op.isHeapInst() && k > 0) {
				int eIdx = domE.indexOf(q);
				assert (eIdx >= 0);
				add(locks.get(k - 1), eIdx);
			}
		}
		
		for (Object o : bb.getSuccessors()) {
			BasicBlock bb2 = (BasicBlock) o;
			if (!visited.contains(bb2)) {
				visited.add(bb2);
				process(bb2, locks);
			}
		}
		
		// traverse the exception handler basic block using oldLocks
		// because program may enter an exception basic block
		// without go through all the instructions of the current basic block
		for (Object o : bb.getExceptionHandlerEntries()) {
			BasicBlock bb2 = (BasicBlock) o;
			if (!visited.contains(bb2)) {
				visited.add(bb2);
				process(bb2, oldLocks);
			}
		}
	}
}
