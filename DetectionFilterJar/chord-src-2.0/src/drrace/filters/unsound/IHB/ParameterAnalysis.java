package drrace.filters.unsound.IHB;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Util.Templates.ListIterator;

/*
 * This class is used to check whether the parameter of removeCallbacksAndMessages is null
 */

public class ParameterAnalysis {
	// mQ is the removeCallbacksAndMessages invoke statement
	private Quad mQ;
	// mMethod is the method to be analyzed
	private jq_Method mMethod;
	// cfg is the Control Flow Graph of mMethoed
	private ControlFlowGraph cfg;
	// mReachingDefs is used for reaching definition data-flow analysis
	private ReachingDefs mReachingDefs;
	
	// quadBBMap is a Map<Quad,BasicBlock>
	// given a Quad, we can know the BasicBlock the Quad belonging to 
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	private boolean parameterIsNull = false;
	
	ParameterAnalysis(Quad q){
		mQ = q;
		mMethod = q.getMethod();
		cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		genQuadBBMap();
		parameterDataFlow();
	}
	
	public boolean parameterIsNull(){
		return parameterIsNull;
		
	}
	
	// generate quadBBMap
	private void genQuadBBMap(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
			}
		}
	}
	
	
	// use mReachingDefs to get the parameter define statement
	// if the define statement is: MOVE, R, Null
	// we can say that the parameter of removeCallbacksAndMessages is null
	private void parameterDataFlow(){
		assert(mQ.getOperator() instanceof Invoke);
		
		Register paraRegister = Invoke.getParam(mQ, 1).getRegister();
		assert(paraRegister != null);
		
		@SuppressWarnings("unchecked")
		Set<Quad> dataFlowQs = mReachingDefs.getReachingDefs(quadBBMap.get(mQ), mQ, paraRegister);
		
		if(dataFlowQs != null && 
				dataFlowQs.size() == 1){
			Quad q = null;
			for(Quad dataFlowQ : dataFlowQs)
				q = dataFlowQ;
			
			assert(q != null);
			
			if(q.getOperator() instanceof Move){
				Operand srcOperand = Move.getSrc(q);
				if(srcOperand != null &&
						srcOperand instanceof AConstOperand){
					AConstOperand mAConstOperand = (AConstOperand) srcOperand;
					if(mAConstOperand.toString().equals("AConst: null")){
						parameterIsNull = true;
					}
				}
			}
		} 
		
	}
	
}
