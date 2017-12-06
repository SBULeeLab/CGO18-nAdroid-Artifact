package drrace.filters.unsound.useForParam;

import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This relation EForParam(e)
 * e is the getFiled or getStatic statement that is only used as parameter for invoke statement.
 */

@Chord(
		name = "EForParam",
		sign = "E0"
	)

public class RelEForParam extends ProgramRel implements IMethodVisitor{
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
		
		UseForParamGenerator mUseForParamGenerator = new UseForParamGenerator(m);
		for(Quad q : mUseForParamGenerator.getRdForParamSet()){
			super.add(q);
		}
	}
}