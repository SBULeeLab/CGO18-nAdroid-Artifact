package drrace.filters.unsound.useForRtn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Util.Templates.ListIterator;

public class UseForRtnGenerator {
	private jq_Method mMethod;
	private ControlFlowGraph cfg;
	private ReachingDefs mReachingDefs;
	
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	private Set<Quad> returnQSet = new HashSet<Quad>();
	
	private Set<Quad> rdForRtnSet = new HashSet<Quad>();
	
	UseForRtnGenerator(jq_Method m){
		mMethod = m;
		cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		findReturnQuad();
		returnQuadDataFlow();
	}
	
	public Set<Quad> getRdForRtnSet(){
		return rdForRtnSet;
	}
	
	private void findReturnQuad(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
				
				if(q.getOperator() instanceof Return)
					returnQSet.add(q);
			}
		}
	}
	
	private void returnQuadDataFlow(){
		for(Quad returnQuad : returnQSet){
			Quad rdForRtnQuad = returnQuadDataFlow(returnQuad);
			
			if(rdForRtnQuad == null)
				continue;
			
			rdForRtnSet.add(rdForRtnQuad);
			System.out.println("method: " + rdForRtnQuad.getMethod());
			System.out.println("rdForRtnQuad: " + rdForRtnQuad);
			System.out.println("returnQuad: " + returnQuad);
		}
	}
	
	private Quad returnQuadDataFlow(Quad q){
		
		Operator mOperator = q.getOperator();
		
		Register mRegister = null;
		
		if(mOperator instanceof Getfield){
			Operand mOperand = Getfield.getBase(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				if(mRegisterOperand.getRegister().getNumber() == 0)
					return q;
			}
		}
		
		if(mOperator instanceof Getstatic){
			return q;
		}
		
		if(mOperator instanceof Return){
			Operand mOperand = Return.getSrc(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				mRegister = mRegisterOperand.getRegister();
			}
		}
		
		if(mOperator instanceof Move){
			Operand mOperand = Move.getSrc(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				mRegister = mRegisterOperand.getRegister();
			}
		}
		
		if(mRegister == null){
			System.out.println("DataFlow breaks here:");
			System.out.println(q.getMethod());
			System.out.println(q);
			return null;
		}
		
		@SuppressWarnings("unchecked")
		Set<Quad> dataFlowQs = mReachingDefs.getReachingDefs(quadBBMap.get(q), q, mRegister);
		
		if(dataFlowQs == null){
			System.out.println("DataFlow breaks here: dataFlowQs = null");
			System.out.println(q.getMethod());
			System.out.println(q);
			System.out.println(dataFlowQs);
			return null;
		}
		
		if(dataFlowQs.size() == 1){
			Quad mQ = null;
			for(Quad dataFlowQ : dataFlowQs)
				mQ = dataFlowQ;
			assert(mQ != null);
			return returnQuadDataFlow(mQ);
		} else{
			System.out.println("DataFlow breaks here: dataFlowQs size > 1");
			System.out.println(q.getMethod());
			System.out.println(q);
			System.out.println(dataFlowQs);
			return null;
		}
	}
	
}
