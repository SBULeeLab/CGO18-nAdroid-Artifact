package drrace.filters.sound.ifGuard;

import java.util.Set;
import java.util.Map.Entry;

import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This relation ifGuardE(e0,e1)
 * e0: getField or getStatic used for IFCMP_A
 * e1: getField or getStatic protected by e0
 * 
 * Lock+ifGuardE filter needs to know e0.
 * 
 * When e0 == e1, e0 and e1 are both the getField or getStatic used for IFCMP_A
 * In this situation, there is no statement in IfGuardNonProtectedBBSet use the register for IFCMP_A.
 * Example:
 * Object tempObj = fieldObj;
 * if(tempObj != null){
 * 		tempObj.use();
 * }
 * ### tempObj.use(); (if there is a use here, the getField will not be protected)
 */

@Chord(
		name = "ifGuardE",
		sign = "E0,E1:E0xE1"
	)

public class RelIfGuardE extends ProgramRel implements IMethodVisitor{
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
		
		IfGuardEGenerator mIfGuardEGenerator = new IfGuardEGenerator(m);
		for(Entry<Quad, Set<Quad>> entry : 
				mIfGuardEGenerator.getIfGuardEMap().entrySet()){
			for(Quad ifGuardE : entry.getValue()){
				super.add(entry.getKey(), ifGuardE);
			}
		}
	}
}
