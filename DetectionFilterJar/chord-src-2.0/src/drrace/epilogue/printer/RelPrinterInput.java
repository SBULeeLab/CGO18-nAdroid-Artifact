package drrace.epilogue.printer;

import java.util.HashSet;
import java.util.Set;

import chord.analyses.alias.Ctxt;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Trio;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

@Chord(
	name = "printerInput",
	sign = "A0,C0,E0,A1,C1,E1:E0xE1_C0xC1_A0xA1"
)

/*
 * This relation is the input for dlog generating F_MM_MM
 */

public class RelPrinterInput extends ProgramRel {
	// Set to store TCI_TCE
	private final 
		Set<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				 Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad >> mTCETCE_Set = 
				 new HashSet<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				 				  Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad >>();
	
	// Clear mTCETCE_Set
	public void clearSet(){
		mTCETCE_Set.clear();
	}
	
	// add TCI_TCE element to the Set
	public void addToSet(Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
			 				  Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tuple){
		mTCETCE_Set.add(tuple);
	}
	
	// fill the relation using the Set
	@Override
	public void fill(){
		for(Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad, 
				 Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> mTCETCE : mTCETCE_Set){
			super.add(mTCETCE.val0, mTCETCE.val1, mTCETCE.val2,
					  mTCETCE.val3, mTCETCE.val4, mTCETCE.val5);
		}
	}
}