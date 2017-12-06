package drrace.filters.sound.ifGuard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chord.util.tuple.object.Pair;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.ConditionOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.TargetOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.IntIfCmp.IFCMP_A;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Util.Templates.ListIterator;

public class IfGuardEGenerator {
	// mMethod is the method to be analyzed
	private jq_Method mMethod;
	// cfg is the Control Flow Graph of mMethoed
	private ControlFlowGraph cfg;
	// mReachingDefs is used for reaching definition data-flow analysis
	private ReachingDefs mReachingDefs;
	
	// quadBBMap is a Map<Quad,BasicBlock>
	// given a Quad, we can know the BasicBlock the Quad belonging to 
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	// Set of IFCMP_A(null) Quads
	private Set<Quad> ifGuards = new HashSet<Quad>();
	
	// ifGuardEMap<Q1, Set<Q2>>
	// Q1 is the getField or getStatic used for IFCMP_A(null)
	// Q2 is the getField or getStitic protected by Q1
	private Map<Quad, Set<Quad>> ifGuardEMap = new HashMap<Quad, Set<Quad>>();
	
	IfGuardEGenerator(jq_Method m){
		mMethod = m;
		cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		// generate quadBBMap and ifGuards
		findIfGuard();
		
		// generate ifGuardEMap
		ifGuardDataFlow();
	}
	
	public Map<Quad, Set<Quad>> getIfGuardEMap(){
		return ifGuardEMap;
	}
	
	private void findIfGuard(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
			}
			Quad q = b.getLastQuad();
			if(q != null)
				if(isIfGuard(q))
					ifGuards.add(q);
		}
		
	}
	
	private boolean isIfGuard(Quad q){
		if(q.getOperator() instanceof IFCMP_A){
			if(q.getOp2().toString().equals("AConst: null"))
				return true;
		}
		return false;
	}
	
	private void ifGuardDataFlow(){
		for(Quad ifGuard : ifGuards){
			System.out.println("method: " + ifGuard.getMethod());
			System.out.println("ifGuard: " + ifGuard);
			Quad ifGuardGetFieldQuad = ifGuardDataFlow(ifGuard);
			
			if(ifGuardGetFieldQuad == null)
				continue;
			
			ifGuardEMap.put(ifGuardGetFieldQuad, new HashSet<Quad>());
			System.out.println("method: " + ifGuardGetFieldQuad.getMethod());
			System.out.println("ifGuardGetFieldQuad: " + ifGuardGetFieldQuad);
			
			FieldOperand mFieldOperand = null;
			if(ifGuardGetFieldQuad.getOperator() instanceof Getfield)
				mFieldOperand = (FieldOperand) ifGuardGetFieldQuad.getOp3();
			if(ifGuardGetFieldQuad.getOperator() instanceof Getstatic)
				mFieldOperand = (FieldOperand) ifGuardGetFieldQuad.getOp2();
			jq_Field mIfGuardField = mFieldOperand.getField();
			
			Pair<Set<BasicBlock>, Set<BasicBlock>> BBSetPair = findBBSet(ifGuard);
			
			// find the  protected getField and getStatic in IfGuardProtectedBBSet
			Set<BasicBlock> IfGuardProtectedBBSet = BBSetPair.val0;
			for(BasicBlock IfGuardProtectedBB : IfGuardProtectedBBSet){
				Set<Quad> ifGuardESet = findIfGuardEs(IfGuardProtectedBB, mIfGuardField);
				ifGuardEMap.get(ifGuardGetFieldQuad).addAll(ifGuardESet);
			}
			
			// check whether there is any statement in IfGuardNonProtectedBBSet use
			// the register for comparing
			// if there is not, the ifGuard should be protected
			Set<BasicBlock> IfGuardNonProtectedBBSet = BBSetPair.val1;
			boolean useIfGuardRegister = false;
			Register ifGuardRegister = ((RegisterOperand) ifGuard.getOp1()).getRegister();
			for(BasicBlock IfGuardNonProtectedBB : IfGuardNonProtectedBBSet){
				if(useIfGuardRegister(IfGuardNonProtectedBB, ifGuardRegister)){
					useIfGuardRegister = true;
					break;
				}
			}
			if(!useIfGuardRegister){
				ifGuardEMap.get(ifGuardGetFieldQuad).add(ifGuardGetFieldQuad);
			}
		}
	}
	
	private Quad ifGuardDataFlow(Quad q){
		
		Operator mOperator = q.getOperator();
		
		RegisterOperand mRegisterOperand = null;
		
		if(mOperator instanceof Getfield){
			return q;
		}
		
		if(mOperator instanceof Getstatic){
			return q;
		}
		
		if(mOperator instanceof IntIfCmp){
			if(q.getOp1() instanceof RegisterOperand)
				mRegisterOperand = (RegisterOperand) q.getOp1();
		}
			
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
			return ifGuardDataFlow(mQ);
		} else{
			System.out.println("DataFlow breaks here: dataFlowQs size > 1");
			System.out.println(q.getMethod());
			System.out.println(q);
			System.out.println(dataFlowQs);
			return null;
		}
	}
	
	private Pair<Set<BasicBlock>, Set<BasicBlock>> findBBSet(Quad ifGuard){
		Set<BasicBlock> IfGuardProtectedBBSet = new HashSet<BasicBlock>();
		Set<BasicBlock> IfGuardNonProtectedBBSet = new HashSet<BasicBlock>();
		
		ConditionOperand mConditionOperand = (ConditionOperand) ifGuard.getOp3();
		TargetOperand mTargetOperand = (TargetOperand) ifGuard.getOp4();
		
		BasicBlock targetBB = mTargetOperand.getTarget();
		
		BasicBlock bb = quadBBMap.get(ifGuard);
		
		@SuppressWarnings("unchecked")
		List<BasicBlock> mSuccessorsList = bb.getSuccessorsList();
		
		assert(mSuccessorsList.size() == 2);
		assert(mSuccessorsList.contains(targetBB));
		BasicBlock nonTargetBB;
		if(mSuccessorsList.indexOf(targetBB) == 0)
			nonTargetBB = mSuccessorsList.get(1);
		else
			nonTargetBB = mSuccessorsList.get(0);
			
		BasicBlock protectedStartBB;
		BasicBlock notProtectedStartBB;
		if(mConditionOperand.getCondition() == 1){
			protectedStartBB = targetBB;
			notProtectedStartBB = nonTargetBB;
		}else{
			protectedStartBB = nonTargetBB;
			notProtectedStartBB = targetBB;
		}
		
		@SuppressWarnings("unchecked")
		List<BasicBlock> protectedPath = cfg.reversePostOrder(protectedStartBB);
		@SuppressWarnings("unchecked")
		List<BasicBlock> notProtectedPath = cfg.reversePostOrder(notProtectedStartBB);
		
		for(BasicBlock mBB : protectedPath){
			if(!notProtectedPath.contains(mBB))
				IfGuardProtectedBBSet.add(mBB);
		}
		
		for(BasicBlock mBB : notProtectedPath){
			IfGuardNonProtectedBBSet.add(mBB);
		}
		
		return new Pair<Set<BasicBlock>, Set<BasicBlock>>(IfGuardProtectedBBSet, IfGuardNonProtectedBBSet);
	}
	
	private Set<Quad> findIfGuardEs(BasicBlock bb, jq_Field mField){
		Set<Quad> ifGuardESet = new HashSet<Quad>();
		
		for (int i = 0; i < bb.size(); i++){
			Quad q = bb.getQuad(i);
			
			FieldOperand mFieldOperand = null;
			if(q.getOperator() instanceof Getfield)
				mFieldOperand = (FieldOperand) q.getOp3();
			if(q.getOperator() instanceof Getstatic)
				mFieldOperand = (FieldOperand) q.getOp2();
			if(mFieldOperand != null){
				if(mFieldOperand.getField().equals(mField)){
					ifGuardESet.add(q);
					System.out.println("method: " + q.getMethod());
					System.out.println("IfGuardE: " + q);
				}
			}
		}
		
		return ifGuardESet;
	}
	
	public boolean useIfGuardRegister(BasicBlock bb, Register r){
		for (int i = 0; i < bb.size(); i++){
			Quad q = bb.getQuad(i);
			
			joeq.Util.Templates.ListIterator.RegisterOperand 
				it = q.getUsedRegisters().registerOperandIterator();
			
			while(it.hasNext()){
				Register register = it.nextRegisterOperand().getRegister();
				if(register == r)
					return true;
			}
		}
		
		return false;
	}
}
