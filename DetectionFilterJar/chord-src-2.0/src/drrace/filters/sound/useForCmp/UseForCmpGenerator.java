package drrace.filters.sound.useForCmp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Util.Templates.ListIterator;

public class UseForCmpGenerator {
	// mMethod is the method to be analyzed
	private jq_Method mMethod;
	// cfg is the Control Flow Graph of mMethoed
	private ControlFlowGraph cfg;
	
	// quadBBMap is a Map<Quad,BasicBlock>
	// given a Quad, we can know the BasicBlock the Quad belonging to 
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	// getFieldEs is a set of quad that are getField and get static
	private Set<Quad> getFieldEs = new HashSet<Quad>();
	
	// srcRegisterMoveMap:
	// Quad is a move statement, while the Register is the source register of the Move statement
	private Map<Register,Quad> srcRegisterMoveMap = new HashMap<Register,Quad>();
	
	// registerIfCmpSet is a Set of registers that are inside a IntIfCmp statement
	private Set<Register> registerIfCmpSet = new HashSet<Register>();
	
	// set of registers that can be used as:
	// (1) base of putField and getField
	// (2) parameters (include base) of Invoke
	private Map<Register,Set<Quad>> registerHarmfulUseMap = new HashMap<Register,Set<Quad>>();
	
	private boolean rdForCmpSetDone = false;
	// Set of Quads (getField and getStatic) that are only using for comparing
	private Set<Quad> rdForCmpSet = new HashSet<Quad>();
	
	
	UseForCmpGenerator(jq_Method m){
		mMethod = m;
		cfg = m.getCFG();
		
		traverseMethod();
	}
	
	public Set<Quad> getRdForCmpSet(){
		if(!rdForCmpSetDone){
			genRdForCmpSet();
			rdForCmpSetDone = true;
		}
		return rdForCmpSet;
	}
	
	// traverse the Method to  
	// (1) generate quadBBMap 
	// (2) generate getFieldEs
	// (3) check each register and add to the Set they belongs to 
	private void traverseMethod(){
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
				
				if(isGetField(q))
					getFieldEs.add(q);
				
				checkAndGenMap(q);
			}
		}
	}
	
	private boolean isGetField(Quad q){
		if(q.getOperator() instanceof Getfield){
			jq_Field mField = Getfield.getField(q).getField();
			if(mField.getType().isReferenceType())
				return true;
		}
		if(q.getOperator() instanceof Getstatic){
			jq_Field mField = Getstatic.getField(q).getField();
			if(mField.getType().isReferenceType())
				return true;
		}
		return false;
	}
	
	private void checkAndGenMap(Quad q){
		Operator mOperator = q.getOperator();
		
		if(mOperator instanceof Getfield){
			Operand mOperand = Getfield.getBase(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				Register mRegister = mRegisterOperand.getRegister();
				if(mRegister.getType().isReferenceType())
					addToRegisterHarmfulUseMap(mRegister, q);
			}
			return;
		}
		
		if(mOperator instanceof Putfield){
			Operand mOperand = Putfield.getBase(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				Register mRegister = mRegisterOperand.getRegister();
				if(mRegister.getType().isReferenceType())
					addToRegisterHarmfulUseMap(mRegister, q);
			}
			return;
		}
		
		if(mOperator instanceof Move){
			Operand mOperand = Move.getSrc(q);
			if(mOperand instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) mOperand;
				Register mRegister = mRegisterOperand.getRegister();
				if(mRegister.getType().isReferenceType())
					srcRegisterMoveMap.put(mRegister, q);
			}
			return;
		}
		
		if(mOperator instanceof IntIfCmp){
			Operand src1 = IntIfCmp.getSrc1(q);
			if(src1 instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) src1;
				Register mRegister = mRegisterOperand.getRegister();
				if(mRegister.getType().isReferenceType())
					registerIfCmpSet.add(mRegister);
			}
			Operand src2 = IntIfCmp.getSrc2(q);
			if(src2 instanceof RegisterOperand){
				RegisterOperand mRegisterOperand = (RegisterOperand) src2;
				Register mRegister = mRegisterOperand.getRegister();
				if(mRegister.getType().isReferenceType())
					registerIfCmpSet.add(mRegister);
			}
			return;
		}
		
		if(mOperator instanceof Invoke){
			ParamListOperand mParamList = Invoke.getParamList(q);
			int paramLen = mParamList.length();
			for(int i = 0; i < paramLen; i++){
				Register mRegister = mParamList.get(i).getRegister();
				if(mRegister.getType().isReferenceType())
					addToRegisterHarmfulUseMap(mRegister, q);
			}
			return;
		}
	}
	
	private void addToRegisterHarmfulUseMap(Register mRegister, Quad mQuad){
		if(!registerHarmfulUseMap.containsKey(mRegister))
			registerHarmfulUseMap.put(mRegister, new HashSet<Quad>());
		registerHarmfulUseMap.get(mRegister).add(mQuad);
	}
	
	// generate rdForCmpSet	
	private void genRdForCmpSet(){
		for(Quad getFieldE : getFieldEs){
			Register mRegister = null;
			if(getFieldE.getOperator() instanceof Getfield)
				mRegister = Getfield.getDest(getFieldE).getRegister();
			if(getFieldE.getOperator() instanceof Getstatic)
				mRegister = Getstatic.getDest(getFieldE).getRegister();
			assert(mRegister != null);
			
			if(trackRegister(mRegister, getFieldE)){
				System.out.println("method: " + mMethod);
				System.out.println("rdForCmp: " + getFieldE);
				rdForCmpSet.add(getFieldE);
			}
		}
	}
	
	// track the register to check whether the getField or getStatic
	// is only used for if comparing
	private boolean trackRegister(Register mRegister, Quad mQuad){
		if(srcRegisterMoveMap.containsKey(mRegister)){
			Quad q = srcRegisterMoveMap.get(mRegister);
			assert(q.getOperator() instanceof Move);
			Register destRegister= Move.getDest(q).getRegister();
			return trackRegister(destRegister, mQuad);
		}
		
		if(registerIfCmpSet.contains(mRegister)){
			if(!registerHarmfulUseMap.containsKey(mRegister)){
				return true;
			}else{
				Set<Quad> harmfulUseSet = registerHarmfulUseMap.get(mRegister);
				for(Quad harmfulUse : harmfulUseSet){
					if(canExeAfter(harmfulUse, mQuad))
						return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
	private boolean canExeAfter(Quad after, Quad before){
		Set<Quad> canExeAfterQuadSet = getCanExeAfterQuadSet(before);
		if(canExeAfterQuadSet.contains(after))
			return true;
		else
			return false;
	}
	
	private Set<Quad> getCanExeAfterQuadSet(Quad before){
		Set<Quad> canExeAfterQuadSet = new HashSet<Quad>();
		BasicBlock bb = quadBBMap.get(before);
		@SuppressWarnings("unchecked")
		List<BasicBlock> path = cfg.reversePostOrder(bb);
		for(BasicBlock mBB : path){
			Set<Quad> quadSet;
			if(mBB == bb)
				quadSet = getQuadSetAfterQ(mBB, before);
			else
				quadSet = getQuadSet(mBB);
			canExeAfterQuadSet.addAll(quadSet);
		}
		return canExeAfterQuadSet;
	}
	
	private Set<Quad> getQuadSet(BasicBlock bb){
		return getQuadSetAfterQ(bb, null);
	}
	
	private Set<Quad> getQuadSetAfterQ(BasicBlock bb, Quad mQ){
		Set<Quad> quadSet = new HashSet<Quad>();
		
		int startQ;
		if(mQ != null)
			startQ = bb.getQuadIndex(mQ) + 1;
		else
			startQ = 0;
		
		for (int i = startQ; i < bb.size(); i++)
			quadSet.add(bb.getQuad(i));
		
		return quadSet;
	}
}
