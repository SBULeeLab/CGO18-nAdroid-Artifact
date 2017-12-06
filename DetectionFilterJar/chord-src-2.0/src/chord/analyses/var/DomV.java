/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.var;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Class.jq_Type;
import joeq.Util.Templates.ListIterator;

/**
 * Domain of local variables of reference type.
 * <p>
 * Each local variable declared in each block of each method is
 * represented by a unique element in this domain.  Local variables
 * that have the same name but are declared in different methods
 * or in different blocks of the same method are represented by
 * different elements in this domain.
 * <p>
 * The set of local variables of a method is the disjoint union of
 * its argument variables and its temporary variables.  All local
 * variables of the same method are assigned contiguous indices in
 * this domain.  The argument variables are assigned contiguous
 * indices in order followed by the temporary variables.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "V"
)
public class DomV extends ProgramDom<Register> implements IMethodVisitor {
	private Map<Register, jq_Method> varToMethodMap;
	public void init() {
		varToMethodMap = new HashMap<Register, jq_Method>();
	}
	public jq_Method getMethod(Register v) {
		return varToMethodMap.get(v);
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		List<Register> vars = m.getLiveRefVars();
		for (Register v : vars) {
			varToMethodMap.put(v, m);
			add(v);
		}
	}
	public String toUniqueString(Register v) {
		return v + "!" + getMethod(v);
	}
}
