package drrace.analyses.free;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Util.Templates.ListIterator;

//Track the register to decide whether this statement put a null to that field.

public class FreeInvGenerator {
	private Quad invQuad;
	private Register putRegister;
	private jq_Method mMethod;
	private ControlFlowGraph cfg;
	private ReachingDefs mReachingDefs;
	
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	final private boolean isFreeInv;
	
	FreeInvGenerator(Quad invQuad){
		this.invQuad = invQuad;
		putRegister = getPutRegister();
		
		mMethod = invQuad.getMethod();
		cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		initQuadBBMap();
		if(invQuadDataFlow(invQuad))
			isFreeInv = true;
		else
			isFreeInv = false;
	}
	
	public boolean isFreeInv(){
		return isFreeInv;
	}
	
	private void initQuadBBMap(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
			}
		}
	}
	
	private Register getPutRegister(){
		assert(invQuad.getOperator() instanceof InvokeStatic);
		
		ParamListOperand paramList = InvokeStatic.getParamList(invQuad);
		int paramNum = paramList.length();
		
		if(paramNum == 1)
			return InvokeStatic.getParam(invQuad, 0).getRegister();
		else if(paramNum == 2)
			return InvokeStatic.getParam(invQuad, 1).getRegister();
		else
			return null;
	}
	
	private boolean invQuadDataFlow(Quad q){
		Register mRegister = null;
		
		if(q.equals(invQuad)){
			mRegister = putRegister;
		}else if(q.getOperator() instanceof Move){
			Operand srcOperand = Move.getSrc(q);
			if(srcOperand instanceof AConstOperand){
				AConstOperand mAConstOperand = (AConstOperand) srcOperand;
				if(mAConstOperand.toString().equals("AConst: null")){
					return true;
				}
			}
		}
		
		if(mRegister == null){
			System.out.println("DataFlow breaks here:");
			System.out.println(q.getMethod());
			System.out.println(q);
			return false;
		}
		
		@SuppressWarnings("unchecked")
		Set<Quad> dataFlowQs = mReachingDefs.getReachingDefs(quadBBMap.get(q), q, mRegister);
		
		if(dataFlowQs == null){
			System.out.println("DataFlow breaks here: dataFlowQs = null");
			System.out.println(q.getMethod());
			System.out.println(q);
			System.out.println(dataFlowQs);
			return false;
		}
		
		if(dataFlowQs.size() == 1){
			Quad mQ = null;
			for(Quad dataFlowQ : dataFlowQs)
				mQ = dataFlowQ;
			assert(mQ != null);
			return invQuadDataFlow(mQ);
		} else{
			System.out.println("DataFlow breaks here: dataFlowQs size > 1");
			System.out.println(q.getMethod());
			System.out.println(q);
			System.out.println(dataFlowQs);
			return false;
		}
	}
}
