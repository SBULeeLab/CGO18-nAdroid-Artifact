package drrace.filters.unsound.FHB;

import chord.program.Program;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;

/*
 * activityFinishI(i):
 * i is an invoke statement calling finish:()V@android.app.Activity
 */

@Chord(
	    name = "activityFinishI",
	    sign = "I0:I0"
	)

public class RelActivityFinishI extends ProgramRel implements IInvokeInstVisitor{
	
	private jq_Class mClass;
	private jq_Method mMethod;
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	private jq_Method finishM = Program.g().getMethod("finish:()V@android.app.Activity");
	
	@Override
	public void visit(jq_Class c) {
		mClass = c;
	}
	
	@Override
	public void visit(jq_Method m) {
		mMethod = m;
	}
	
	@Override
	public void visitInvokeInst(Quad q) {
		// ignore this relation when finishM is not in RTA scope 
		if(finishM == null)
			return;
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		
		jq_Method invokeMethod = Invoke.getMethod(q).getMethod();
		if(invokeMethod == finishM){
			System.out.println("activityFinishI: " + q);
			System.out.println("activityFinishIMethod: " + mMethod);
			super.add(q);
		}
	}
}
