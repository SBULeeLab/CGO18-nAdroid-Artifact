package drrace.filters.sound.intraAllocation;

import java.util.Map.Entry;
import java.util.Set;

import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
 * This relation allocE(k,e)
 * allocE(0,e0,e1): 
 * e0 : allocation putField or putStatic (sound)
 * e1 : getField or getStatic protected by allocation (sound)
 * Lock+allocE filter needs to know e0.
 * 
 * allocE(1,e0,e1): 
 * e0: may-be-allocation putField or putStatic (unsound)
 * e1: getField or getStatic protected by may-be-allocation (unsound)
 * Lock+allocE filter needs to know e0.
 * 
 * allocE(2,e0,e0): 
 * e0 : allocation putField or putStatic (sound)
 * This is used for Resume Happen Before filters.
 * 
 * allocE(3,e0,e0):
 * e0 : may-be-allocation putField or putStatic (unsound)
 * This can be used for Resume Happen Before filters. (We don't use it now).
 */

@Chord(
		name = "allocE",
		sign = "K0,E0,E1:E0xE1_K0"
	)

public class RelAllocE extends ProgramRel implements IMethodVisitor{
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
		
		AllocEGenerator mAllocEGenerator = new AllocEGenerator(m);
		for(Entry<Quad, Set<Quad>> entry : mAllocEGenerator.getAllocEMap().entrySet()){
			super.add(2, entry.getKey(), entry.getKey());
			for(Quad allocE : entry.getValue()){
				super.add(0, entry.getKey(), allocE);
			}
		}
		
		for(Entry<Quad, Set<Quad>> entry : mAllocEGenerator.getAllocEAggressiveMap().entrySet()){
			super.add(3, entry.getKey(), entry.getKey());
			for(Quad allocEAggressive : entry.getValue()){
				super.add(1, entry.getKey(), allocEAggressive);
			}
		}
	}
}
