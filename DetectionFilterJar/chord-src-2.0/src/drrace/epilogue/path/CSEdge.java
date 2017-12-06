package drrace.epilogue.path;

import chord.analyses.alias.Ctxt;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Method;

/*
 * This is a context-sensitive call graph edge
 */

public class CSEdge {
	private Pair<Ctxt, jq_Method> caller;
	private Pair<Ctxt, jq_Method> callee;

	CSEdge(Pair<Ctxt, jq_Method> caller, Pair<Ctxt, jq_Method> callee){
		this.caller = caller;
		this.callee = callee;
		
		assert(caller != null);
		assert(callee != null);
	}
	
	public Pair<Ctxt, jq_Method> getCaller(){
		return caller;
	}
	
	public Pair<Ctxt, jq_Method> getCallee(){
		return callee;
	}
}
