package drrace.filters.sound.useForCmp;

import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This relation EForCmp(e)
 * e is the getFiled or getStatic statement that is only used for comparision.
 */

@Chord(
		name = "EForCmp",
		sign = "E0"
	)

public class RelEForCmp extends ProgramRel implements IMethodVisitor{
	private jq_Class mClass;
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	@Override
	public void visit(jq_Class c) {
		mClass = c;
	}

	@Override
	public void visit(jq_Method m) {
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		// ignore abstract method
		if(m.isAbstract())
			return;
		
		UseForCmpGenerator mUseForCmpGenerator = new UseForCmpGenerator(m);
		for(Quad q : mUseForCmpGenerator.getRdForCmpSet()){
			super.add(q);
		}
	}
}