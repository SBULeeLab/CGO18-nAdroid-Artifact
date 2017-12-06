package drrace.epilogue.path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chord.analyses.alias.Ctxt;
import chord.bddbddb.Rel.QuadIterable;
import chord.project.analyses.ProgramRel;
import chord.util.SetUtils;
import chord.util.graph.AbstractGraph;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Method;

/*
 * This is a context-sensitive call graph 
 * implemented by using Map (not using bdd file)
 */

@SuppressWarnings("serial")
public class CSCG extends AbstractGraph<Pair<Ctxt, jq_Method>>{
	
	private final Set<Pair<Ctxt, jq_Method>> nodes;
	private final Set<Pair<Ctxt, jq_Method>> roots;
	private final Set<CSEdge> edges = new HashSet<CSEdge>();
	
	/*
	private final Map<Pair<Ctxt, jq_Method>, Set<CSEdge>> callerToEdgesMap = 
			new HashMap<Pair<Ctxt, jq_Method>, Set<CSEdge>>();
	private final Map<Pair<Ctxt, jq_Method>, Set<CSEdge>> calleeToEdgesMap = 
			new HashMap<Pair<Ctxt, jq_Method>, Set<CSEdge>>();
	*/
	
	private final Map<Pair<Ctxt, jq_Method>, Set<Pair<Ctxt, jq_Method>>> callerToCalleesMap = 
			new HashMap<Pair<Ctxt, jq_Method>, Set<Pair<Ctxt, jq_Method>>>();
	private final Map<Pair<Ctxt, jq_Method>, Set<Pair<Ctxt, jq_Method>>> calleeToCallersMap = 
			new HashMap<Pair<Ctxt, jq_Method>, Set<Pair<Ctxt, jq_Method>>>();
	
	
	CSCG(ProgramRel relRootCM,
			ProgramRel relReachableCM,
			ProgramRel relCMCM){
		relRootCM.load();
		Iterable<Pair<Ctxt, jq_Method>> resRoots = relRootCM.getAry2ValTuples();
		roots =  SetUtils.iterableToSet(resRoots, relRootCM.size());
		relRootCM.close();
		
		relReachableCM.load();
		Iterable<Pair<Ctxt, jq_Method>> resNodes = relReachableCM.getAry2ValTuples();
		nodes = SetUtils.iterableToSet(resNodes, relReachableCM.size());
		relReachableCM.close();
		
		relCMCM.load();
		QuadIterable<Ctxt, jq_Method, Ctxt, jq_Method> CMCMs = relCMCM.getAry4ValTuples();
		for(chord.util.tuple.object.Quad<Ctxt, jq_Method, Ctxt, jq_Method> CMCM : CMCMs){
			Ctxt callerCtxt = CMCM.val0;
			jq_Method callerMethod = CMCM.val1;
			Pair<Ctxt, jq_Method> caller = new Pair<Ctxt, jq_Method>(callerCtxt, callerMethod);
			
			Ctxt calleeCtxt = CMCM.val2;
			jq_Method calleeMethod = CMCM.val3;
			Pair<Ctxt, jq_Method> callee = new Pair<Ctxt, jq_Method>(calleeCtxt, calleeMethod);
			
			CSEdge edge = new CSEdge(caller, callee);
			
			edges.add(edge);
			
			/*
			if(!callerToEdgesMap.containsKey(caller))
				callerToEdgesMap.put(caller, new HashSet<CSEdge>());
			callerToEdgesMap.get(caller).add(edge);
			
			if(!calleeToEdgesMap.containsKey(callee))
				calleeToEdgesMap.put(callee, new HashSet<CSEdge>());
			calleeToEdgesMap.get(callee).add(edge);
			*/
			
			if(!callerToCalleesMap.containsKey(caller))
				callerToCalleesMap.put(caller, new HashSet<Pair<Ctxt, jq_Method>>());
			callerToCalleesMap.get(caller).add(callee);
			
			if(!calleeToCallersMap.containsKey(callee))
				calleeToCallersMap.put(callee, new HashSet<Pair<Ctxt, jq_Method>>());
			calleeToCallersMap.get(callee).add(caller);
		}
		relCMCM.close();
	}
	
	@Override
	public boolean hasRoot(Pair<Ctxt, jq_Method> node) {
		return roots.contains(node);
	}

	@Override
	public boolean hasNode(Pair<Ctxt, jq_Method> node) {
		return nodes.contains(node);
	}

	@Override
	public boolean hasEdge(Pair<Ctxt, jq_Method> caller, Pair<Ctxt, jq_Method> callee) {
		CSEdge edge = new CSEdge(caller, callee);
		return edges.contains(edge);
	}

	@Override
	public int numRoots() {
		return roots.size();
	}

	@Override
	public int numNodes() {
		return nodes.size();
	}

	@Override
	public int numPreds(Pair<Ctxt, jq_Method> callee) {
		//return calleeToEdgesMap.get(callee).size();
		if(calleeToCallersMap.containsKey(callee))
			return calleeToCallersMap.get(callee).size();
		else
			return 0;
	}

	@Override
	public int numSuccs(Pair<Ctxt, jq_Method> caller) {
		//return callerToEdgesMap.get(caller).size();
		if(callerToCalleesMap.containsKey(caller))
			return callerToCalleesMap.get(caller).size();
		else
			return 0;
	}

	@Override
	public Set<Pair<Ctxt, jq_Method>> getRoots() {
		return roots;
	}

	@Override
	public Set<Pair<Ctxt, jq_Method>> getNodes() {
		return nodes;
	}

	@Override
	public Set<Pair<Ctxt, jq_Method>> getPreds(Pair<Ctxt, jq_Method> callee) {
		/*
		Set<Pair<Ctxt, jq_Method>> callers = new HashSet<Pair<Ctxt, jq_Method>>();
		Set<CSEdge> edges = calleeToEdgesMap.get(callee);
		for(CSEdge edge : edges){
			callers.add(edge.getCaller());
		}
		return callers;
		*/
		if(calleeToCallersMap.containsKey(callee))
			return calleeToCallersMap.get(callee);
		else
			return new HashSet<Pair<Ctxt, jq_Method>>();
	}

	@Override
	public Set<Pair<Ctxt, jq_Method>> getSuccs(Pair<Ctxt, jq_Method> caller) {
		/*
		Set<Pair<Ctxt, jq_Method>> callees = new HashSet<Pair<Ctxt, jq_Method>>();
		
		if(callerToEdgesMap.containsKey(caller)){
			Set<CSEdge> edges = callerToEdgesMap.get(caller);
			for(CSEdge edge : edges){
				callees.add(edge.getCallee());
			}
		}
		
		return callees;
		*/
		if(callerToCalleesMap.containsKey(caller))
			return callerToCalleesMap.get(caller);
		else
			return new HashSet<Pair<Ctxt, jq_Method>>();
	}
}
