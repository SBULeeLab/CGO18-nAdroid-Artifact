/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.inst;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v,h) such that method m contains
 * object allocation statement h which assigns to local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MobjValAsgnInst",
	sign = "M0,V0,H0:M0_V0_H0"
)
public class RelMobjValAsgnInst extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomV domV = (DomV) doms[1];
		DomH domH = (DomH) doms[2];
		int numA = domH.getLastA() + 1;
		for (int hIdx = 1; hIdx < numA; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			jq_Method m = q.getMethod();
			int mIdx = domM.indexOf(m);
			assert (mIdx >= 0);
			Operator op = q.getOperator();
			RegisterOperand vo;
			if (op instanceof New)
				vo = New.getDest(q);
			else if(op instanceof NewArray)
				vo = NewArray.getDest(q);
			else if(op instanceof Invoke)
			  vo = Invoke.getDest(q);
		 else if(op instanceof MultiNewArray)
			vo = NewArray.getDest(q);
			else {
			  System.err.println("WARN: in RelMobjValAsgnInst saw H element with op type " + op.toString());
			  vo = null;
			}
			Register v = vo.getRegister();

			int vIdx = domV.indexOf(v);
//	assert (vIdx >= 0);

			if(vIdx >= 0)
		add(mIdx, vIdx, hIdx);
	}
	}
}
