/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.inst;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand.ParamListOperand;

import chord.program.visitors.IMoveInstVisitor;
import chord.program.visitors.IPhiInstVisitor;
import chord.program.visitors.ICastInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p,v1,v2) such that the statement
 * at program point p is of the form <tt>v1 = v2</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PobjVarAsgnInst",
	sign = "P0,V0,V1:P0_V0xV1"
)
public class RelPobjVarAsgnInst extends ProgramRel
		implements IMoveInstVisitor, IPhiInstVisitor, ICastInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitMoveInst(Quad q) {
		Operand rx = Move.getSrc(q);
		if (rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			if (ro.getType().isReferenceType()) {
				Register r = ro.getRegister();
				RegisterOperand lo = Move.getDest(q);
				Register l = lo.getRegister();
				add(q, l, r);
			}
		}
	}
	public void visitPhiInst(Quad q) {
		RegisterOperand lo = Phi.getDest(q);
		jq_Type t = lo.getType();
		if (t == null || t.isReferenceType()) {
			Register l = lo.getRegister();
			ParamListOperand ros = Phi.getSrcs(q);
			int n = ros.length();
			for (int i = 0; i < n; i++) {
				RegisterOperand ro = ros.get(i);
				if (ro != null) {
					Register r = ro.getRegister();
					add(q, l, r);
				}
			}
		}
	}
	public void visitCastInst(Quad q) {
		Operand rx = CheckCast.getSrc(q);
		if (rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			if (ro.getType().isReferenceType()) {
				Register r = ro.getRegister();
				RegisterOperand lo = CheckCast.getDest(q);
				Register l = lo.getRegister();
				add(q, l, r);
			}
		}
	}
}
