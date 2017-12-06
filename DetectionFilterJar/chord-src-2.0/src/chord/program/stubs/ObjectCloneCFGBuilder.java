/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program.stubs;

import joeq.Compiler.Quad.ICFGBuilder;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.CheckCast.CHECKCAST;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.ALoad.ALOAD_A;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.AStore.ASTORE_A;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.RETURN_V;
import joeq.Compiler.Quad.Operator.Return.RETURN_A;
import joeq.Compiler.Quad.SSA.EnterSSA;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand;

/**
 * Stub for instance method "Object clone()" in class java.lang.Object.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ObjectCloneCFGBuilder implements ICFGBuilder {
	@Override
	public ControlFlowGraph run(jq_Method m) {
		jq_Class c = m.getDeclaringClass();
		RegisterFactory rf = new RegisterFactory(0, 1);
		Register r = rf.getOrCreateLocal(0, c);
		ControlFlowGraph cfg = new ControlFlowGraph(m, 1, 0, rf);
		RegisterOperand ro = new RegisterOperand(r, c);
		Quad q = Return.create(0, m, RETURN_A.INSTANCE);
		Return.setSrc(q, ro);
		BasicBlock bb = cfg.createBasicBlock(1, 1, 1, null);
		bb.appendQuad(q);
		BasicBlock entry = cfg.entry();
		BasicBlock exit = cfg.exit();
		bb.addPredecessor(entry);
		bb.addSuccessor(exit);
		entry.addSuccessor(bb);
		exit.addPredecessor(bb);
		return cfg;
	}
}
