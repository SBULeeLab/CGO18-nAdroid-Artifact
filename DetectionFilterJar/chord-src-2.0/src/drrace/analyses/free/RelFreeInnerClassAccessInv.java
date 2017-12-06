package drrace.analyses.free;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/*
This ProgramRel freeInnerClassAccessInv is a set of invoke statements
which is used by inner class to free fields of outer class.
*/

@Chord(
	name = "freeInnerClassAccessInv",
	sign = "I0"
)

public class RelFreeInnerClassAccessInv extends ProgramRel {
	public void fill() {
		ClassicProject.g().runTask("innerClassAccessM-dlog");
		
		ProgramRel mRel = (ProgramRel) ClassicProject.g().getTrgt("innerClassAccessPutMInv");
		mRel.load();
		PairIterable<Quad, jq_Method> tuples = mRel.getAry2ValTuples();
		for(Pair<Quad, jq_Method> tuple : tuples){
			Quad invQuad = tuple.val0;
			jq_Method invMethod = tuple.val1;
			
			if(!invMethod.getReturnType().isReferenceType())
				continue;
			
			analyseInvAndMethod(invQuad);
		}
		mRel.close();
	}
	
	private void analyseInvAndMethod(Quad invQuad){
		//Track the register to decide whether this statement put a null to that field.
		FreeInvGenerator mFreeInvGenerator = new FreeInvGenerator(invQuad);
		if(mFreeInvGenerator.isFreeInv()){
			System.out.println("RelFreeInnerClassAccessInv Method: " + invQuad.getMethod());
			System.out.println("RelFreeInnerClassAccessInv: " + invQuad);
			super.add(invQuad);
		}
	}
}