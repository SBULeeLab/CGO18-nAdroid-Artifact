package drrace.filters.unsound.useForRtn;

import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This relation EForRtn(e)
 * e is the getFiled or getStatic statement that is used for return.
 */

@Chord(
		name = "EForRtn",
		sign = "E0"
	)

public class RelEForRtn extends ProgramRel implements IMethodVisitor{
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
		// ignore abstract methods
		if(m.isAbstract())
			return;
		// ignore methods whose return type is not reference type
		if(!m.getReturnType().isReferenceType())
			return;
		// ignore inner class access methods
		if(m.getName().toString().startsWith("access$"))
			return;
		
		UseForRtnGenerator mUseForRtnGenerator = new UseForRtnGenerator(m);
		for(Quad q : mUseForRtnGenerator.getRdForRtnSet()){
			super.add(q);
		}
	}
}