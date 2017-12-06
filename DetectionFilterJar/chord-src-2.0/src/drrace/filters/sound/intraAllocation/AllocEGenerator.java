package drrace.filters.sound.intraAllocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Util.Templates.ListIterator;

public class AllocEGenerator {
	// mMethod is the method to be analyzed
	private jq_Method mMethod;
	// cfg is the Control Flow Graph of mMethoed
	private ControlFlowGraph cfg;
	// mReachingDefs is used for reaching definition data-flow analysis
	private ReachingDefs mReachingDefs;
	
	// quadBBMap is a Map<Quad,BasicBlock>
	// given a Quad, we can know the BasicBlock the Quad belonging to 
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	// Set of putField and putStatic Quads
	private Set<Quad> putFieldEs = new HashSet<Quad>();
	
	// getField or getStatic protected by allocation (sound)
	private Map<Quad,Set<Quad>> allocEMap = new HashMap<Quad, Set<Quad>>();
	// getField or getStatic protected by may-be-allocation (unsound)
	private Map<Quad,Set<Quad>> allocEAggressiveMap = new HashMap<Quad, Set<Quad>>();
	
	AllocEGenerator(jq_Method m){
		mMethod = m;
		cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		// generate quadBBMap and putFieldEs
		findPutField();
		// generate allocESet and allocEAggressiveSet
		putFieldDataFlow();
	}
	
	public Map<Quad,Set<Quad>> getAllocEMap(){
		return allocEMap;
	}
	
	public Map<Quad,Set<Quad>> getAllocEAggressiveMap(){
		return allocEAggressiveMap;
	}
	
	private void findPutField(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
				if(isPutField(q)){
					putFieldEs.add(q);
				}
			}
		}
	}
	
	private boolean isPutField(Quad q){
		if(q.getOperator() instanceof Putfield){
			FieldOperand mFieldOperand = (FieldOperand) q.getOp2();
			jq_Field mField = mFieldOperand.getField();
			if(mField.getType().isReferenceType()
					&& ! q.getOp3().toString().equals("AConst: null"))
				return true;
		}
		if(q.getOperator() instanceof Putstatic){
			FieldOperand mFieldOperand = (FieldOperand) q.getOp2();
			jq_Field mField = mFieldOperand.getField();
			if(mField.getType().isReferenceType()
					&& ! q.getOp1().toString().equals("AConst: null"))
				return true;
		}
		return false;
	}
	
	private void putFieldDataFlow(){
		for(Quad putFieldE : putFieldEs){
			allocEMap.put(putFieldE, new HashSet<Quad>());
			allocEAggressiveMap.put(putFieldE, new HashSet<Quad>());
			
			System.out.println("method: " + mMethod);
			System.out.println("putFieldE: " + putFieldE);
			
			// putFieldEInitQuad is a New statement passing to the putFieldE
			// if (putFieldEInitQuad == null), that is a may-be-allocation (unsound)
			Quad putFieldEInitQuad = putFieldDataFlow(putFieldE);
			
			if(putFieldEInitQuad != null){
				System.out.println("method: " + mMethod);
				System.out.println("putFieldEInitQuad: " + putFieldEInitQuad);
			}
			
			BasicBlock bb = quadBBMap.get(putFieldE);
			
			FieldOperand mFieldOperand = (FieldOperand) putFieldE.getOp2();
			jq_Field mAllocEField = mFieldOperand.getField();
			
			@SuppressWarnings("unchecked")
			List<BasicBlock> path = cfg.reversePostOrder(bb);
			for(BasicBlock mBB : path){
				Set<Quad> allocESet;
				if(mBB == bb)
					allocESet = findAllocEsAfterQ(mBB, mAllocEField, putFieldE);
				else
					allocESet = findAllocEs(mBB, mAllocEField);
				
				if(allocESet.isEmpty())
					continue;
				
				if(putFieldEInitQuad != null){
					allocEMap.get(putFieldE).addAll(allocESet);
					System.out.println("method: " + mMethod);
					System.out.println("allocESet: " + this.allocEMap);
				}else{
					allocEAggressiveMap.get(putFieldE).addAll(allocESet);
					System.out.println("method: " + mMethod);
					System.out.println("allocEAggressiveSet: " + this.allocEAggressiveMap);
				}
			}
		}
	}
	
	private Quad putFieldDataFlow(Quad q){
		Operator mOperator = q.getOperator();
		RegisterOperand mRegisterOperand = null;
		
		if(q.getOperator() instanceof New){
			return q;
		}
		
		if(mOperator instanceof Putfield)
			if(q.getOp3() instanceof RegisterOperand)
				mRegisterOperand = (RegisterOperand) q.getOp3();
		
		if(mOperator instanceof Putstatic)
			if(q.getOp1() instanceof RegisterOperand)
				mRegisterOperand = (RegisterOperand) q.getOp1();
		
		if(mOperator instanceof Move){
			if(q.getOp2() instanceof RegisterOperand)
				mRegisterOperand = (RegisterOperand) q.getOp2();
		}
		
		if(mRegisterOperand == null){
			System.out.println("DataFlow breaks here:");
			System.out.println(q.getMethod());
			System.out.println(q);
			return null;
		}
		
		@SuppressWarnings("unchecked")
		Set<Quad> dataFlowQs = mReachingDefs.getReachingDefs(quadBBMap.get(q), q, mRegisterOperand.getRegister());
		
		if(dataFlowQs == null){
			System.out.println("DataFlow breaks here: dataFlowQs = null");
			System.out.println(mMethod);
			System.out.println(q);
			System.out.println(dataFlowQs);
			return null;
		}
		
		if(dataFlowQs.size() == 1){
			Quad mQ = null;
			for(Quad dataFlowQ : dataFlowQs)
				mQ = dataFlowQ;
			assert(mQ != null);
			return putFieldDataFlow(mQ);
		} else{
			System.out.println("DataFlow breaks here: dataFlowQs size > 1");
			System.out.println(mMethod);
			System.out.println(q);
			System.out.println(dataFlowQs);
			return null;
		}
	}
	
	private Set<Quad> findAllocEs(BasicBlock bb, jq_Field mField){
		return findAllocEsAfterQ(bb, mField, null);
	}
	
	private Set<Quad> findAllocEsAfterQ(BasicBlock bb, jq_Field mField, Quad mQ){
		Set<Quad> allocESet = new HashSet<Quad>();
		
		int startQ;
		if(mQ != null)
			startQ = bb.getQuadIndex(mQ) + 1;
		else
			startQ = 0;
		
		for (int i = startQ; i < bb.size(); i++){
			Quad q = bb.getQuad(i);
			
			FieldOperand mFieldOperand = null;
			if(q.getOperator() instanceof Getfield)
				mFieldOperand = (FieldOperand) q.getOp3();
			if(q.getOperator() instanceof Getstatic)
				mFieldOperand = (FieldOperand) q.getOp2();
			if(mFieldOperand != null){
				if(mFieldOperand.getField().equals(mField)){
					allocESet.add(q);
					//System.out.println("method: " + q.getMethod());
					//System.out.println("allocE: " + q);
				}
			}
		}
		
		return allocESet;
	}
}
