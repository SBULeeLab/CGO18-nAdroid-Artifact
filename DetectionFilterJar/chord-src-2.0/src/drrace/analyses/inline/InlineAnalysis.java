package drrace.analyses.inline;


import java.util.HashMap;
import java.util.Map;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Util.Templates.ListIterator;

/*
 * This is Inline Analysis for access$ methods 
 * (used by inner class to access outer class's method, field...)
 * 
 * With this analysis we can filter false positive (inner class ifGuard or intra-allocation).
 * 
 * We only analysis access$ for Objects.(PUTFIELD_A, PUTSTATIC_A, GETFIELD_A, GETSTATIC_A).
 * 
 * Basic idea:
 * (1) INVOKESTATIC_A T5, access$002, (R0, T3) 	-> PUTFIELD_A 	R0, .f, T3
 * (2) INVOKESTATIC_A T7, access$102, (T6)		-> PUTSTATIC_A 	T6, .f
 * (3) INVOKESTATIC_A T5, access$000, (R4)		-> GETFIELD_A 	T5, R4, .f
 * (4) INVOKESTATIC_A T8, access$100, ()		-> GETSTATIC_A 	T8, .f
 */

@Chord(
	name="drrace-inline"
)

public class InlineAnalysis extends JavaAnalysis {
	
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	// Map for putField Access Method: method -> field
	private Map<jq_Method, jq_Field> putFieldAccessMethodMap = new HashMap<jq_Method, jq_Field>();
	// Map for putStatic Access Method: method -> field
	private Map<jq_Method, jq_Field> putStaticAccessMethodMap = new HashMap<jq_Method, jq_Field>();
	// Map for getField Access Method: method -> field
	private Map<jq_Method, jq_Field> getFieldAccessMethodMap = new HashMap<jq_Method, jq_Field>();
	// Map for getStatic Access Method: method -> field
	private Map<jq_Method, jq_Field> getStaticAccessMethodMap = new HashMap<jq_Method, jq_Field>();
	
	@Override
	public void run(){
		// identify access$ methods and generate maps
		accessMethodAnalysis();
		
		// replace the access$ invoke statement to corresponding put or get
		accessMethodInvokeAnalysis();
	}
	
	private void accessMethodAnalysis(){
		for(jq_Method mMethod : Program.g().getMethods()){
			// ignore abstract methods
			if(mMethod.isAbstract())
				continue;
			
			// access$ are all static methods
			if(!mMethod.isStatic())
				continue;
			
			// ignore library classes
			jq_Class mClass = mMethod.getDeclaringClass();
			if(androidTypeAnalysis.isLibClass(mClass))
				continue;
			
			// identify access$ method by name
			String methodName = mMethod.getName().toString();
			if(!methodName.startsWith("access$"))
				continue;
			
			// identify access$ method by cfg structure
			ControlFlowGraph cfg = mMethod.getCFG();
			if(!(cfg.getNumberOfBasicBlocks() == 3))
				continue;
			
			EntryOrExitBasicBlock entryBB = cfg.entry();
			EntryOrExitBasicBlock exitBB = cfg.exit();
			
			assert (entryBB.getNumberOfSuccessors() == 1);
			assert (exitBB.getNumberOfPredecessors() == 1);
			
			BasicBlock bb0 = entryBB.getSuccessors().getBasicBlock(0);
			BasicBlock bb1 = exitBB.getPredecessors().getBasicBlock(0);
			
			assert(bb0 == bb1);
			
			BasicBlock bb = bb0;
			
			if(bb.size() == 2){
				putAcceccMethodAnalysis(mMethod, bb);
			}else if(bb.size() == 3){
				getAcceccMethodAnalysis(mMethod, bb);
			}
		}
	}
	
	
	/*
	 * PutField AccessMethod Example
	 * BB0 (ENTRY)	(in: <none>, out: BB2)
	 * 
	 * BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
	 * 1: PUTFIELD_A R0, .f, R1
	 * 2: RETURN_A R1
	 * 
	 * BB1 (EXIT)	(in: BB2, out: <none>)
	 */
	/*
	 * PutStatic AccessMethod Example
	 * BB0 (ENTRY)	(in: <none>, out: BB2)
	 * 
	 * BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
	 * 1: PUTSTATIC_A R0, .f
	 * 2: RETURN_A R0
	 * 
	 * 
	 * BB1 (EXIT)	(in: BB2, out: <none>)
	 */
	private void putAcceccMethodAnalysis(jq_Method mMethod, BasicBlock bb){
		Quad firstQuad = bb.getQuad(0);
		Quad lastQuad = bb.getQuad(1);
		
		Operator firstQuadOp = firstQuad.getOperator();
		Operator lastQuadOp = lastQuad.getOperator();
		
		Register rtnRegister = null;
		if(lastQuadOp instanceof Return.RETURN_A){
			Operand rtnOperand = Return.getSrc(lastQuad);
			if(rtnOperand instanceof RegisterOperand){
				rtnRegister = ((RegisterOperand) rtnOperand).getRegister();
			}
		}
		
		if(rtnRegister == null)
			return;
		
		if(firstQuadOp instanceof Putfield.PUTFIELD_A){
			jq_Field accessField = Putfield.getField(firstQuad).getField();
			
			Register putRegister = null;
			Operand rtnOperand = Putfield.getSrc(firstQuad);
			if(rtnOperand instanceof RegisterOperand){
				putRegister = ((RegisterOperand) rtnOperand).getRegister();
			}
			
			if(putRegister != null && 
					putRegister == rtnRegister){
				putFieldAccessMethodMap.put(mMethod, accessField);
				System.out.println("putFieldAcceccMethod: " + mMethod);
				System.out.println("accessField: " + accessField);
				System.out.println(bb.fullDump());
			}
		} else if(firstQuadOp instanceof Putstatic.PUTSTATIC_A){
			jq_Field accessField = Putstatic.getField(firstQuad).getField();
			
			Register putRegister = null;
			Operand rtnOperand = Putstatic.getSrc(firstQuad);
			if(rtnOperand instanceof RegisterOperand){
				putRegister = ((RegisterOperand) rtnOperand).getRegister();
			}
			
			if(putRegister != null && 
					putRegister == rtnRegister){
				putStaticAccessMethodMap.put(mMethod, accessField);
				System.out.println("putStaticAcceccMethod: " + mMethod);
				System.out.println("accessField: " + accessField);
				System.out.println(bb.fullDump());
			}
		}	
	}
	
	
	/*
	 * GetField AccessMethod Example
	 * BB0 (ENTRY)	(in: <none>, out: BB2)
	 * 
	 * BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
	 * 1: GETFIELD_A T1, R0, .f
	 * 2: MOVE_A R2, T1
	 * 3: RETURN_A R2
	 * 
	 * BB1 (EXIT)	(in: BB2, out: <none>)
	 */
	/*
	 * GetStatic AccessMethod Example
	 * BB0 (ENTRY)	(in: <none>, out: BB2)
	 * 
	 * BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
	 * 1: GETSTATIC_A T0, .f
	 * 2: MOVE_A R1, T0
	 * 3: RETURN_A R1
	 * 	 * 
	 * BB1 (EXIT)	(in: BB2, out: <none>)
	 */
	
	private void getAcceccMethodAnalysis(jq_Method mMethod, BasicBlock bb){
		Quad firstQuad = bb.getQuad(0);
		Quad secondQuad = bb.getQuad(1);
		Quad lastQuad = bb.getQuad(2);
		
		Operator firstQuadOp = firstQuad.getOperator();
		Operator secondQuadOp = secondQuad.getOperator();
		Operator lastQuadOp = lastQuad.getOperator();
		
		Register rtnRegister = null;
		if(lastQuadOp instanceof Return.RETURN_A){
			Operand rtnOperand = Return.getSrc(lastQuad);
			if(rtnOperand instanceof RegisterOperand){
				rtnRegister = ((RegisterOperand) rtnOperand).getRegister();
			}
		}
		if(rtnRegister == null)
			return;
		
		Register moveSrcRegister = null;
		if(secondQuadOp instanceof Move.MOVE_A){
			Register moveDestRegister = Move.getDest(secondQuad).getRegister();
			if(moveDestRegister == rtnRegister){
				Operand moveSrcOperand = Move.getSrc(secondQuad);
				if(moveSrcOperand instanceof RegisterOperand){
					moveSrcRegister = ((RegisterOperand) moveSrcOperand).getRegister();
				}
			}
		}
		if(moveSrcRegister == null)
			return;
		
		if(firstQuadOp instanceof Getfield.GETFIELD_A){
			jq_Field accessField = Getfield.getField(firstQuad).getField();
			
			Register getRegister = null;
			Operand getOperand = Getfield.getDest(firstQuad);
			if(getOperand instanceof RegisterOperand){
				getRegister = ((RegisterOperand) getOperand).getRegister();
			}
			
			if(getRegister != null && 
					getRegister == moveSrcRegister){
				getFieldAccessMethodMap.put(mMethod, accessField);
				System.out.println("getFieldAcceccMethod: " + mMethod);
				System.out.println("accessField: " + accessField);
				System.out.println(bb.fullDump());
			}
		} else if(firstQuadOp instanceof Getstatic.GETSTATIC_A){
			jq_Field accessField = Getstatic.getField(firstQuad).getField();
			
			Register getRegister = null;
			Operand getOperand = Getstatic.getDest(firstQuad);
			if(getOperand instanceof RegisterOperand){
				getRegister = ((RegisterOperand) getOperand).getRegister();
			}
			
			if(getRegister != null && 
					getRegister == moveSrcRegister){
				getStaticAccessMethodMap.put(mMethod, accessField);
				System.out.println("getStaticAcceccMethod: " + mMethod);
				System.out.println("accessField: " + accessField);
				System.out.println(bb.fullDump());
			}
		}	
	}
	
	
	// replace the access$ invoke statement to corresponding put or get
	private void accessMethodInvokeAnalysis(){
		for(jq_Method mMethod : Program.g().getMethods()){
			// ignore abstract methods
			if(mMethod.isAbstract())
				continue;
			
			// ignore library classes
			jq_Class mClass = mMethod.getDeclaringClass();
			if(androidTypeAnalysis.isLibClass(mClass))
				continue;
			
			// traverse the cfg
			ControlFlowGraph cfg = mMethod.getCFG();
			ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
			while(it.hasNext()){
				BasicBlock bb = it.nextBasicBlock();
				for(int i = 0; i< bb.size(); i++){
					Quad q = bb.getQuad(i);
					accessMethodInvokeAnalysis(q);
				}
			}
		}
	}
	
	// replace the access$ invoke statement to corresponding put or get
	private void accessMethodInvokeAnalysis(Quad q){
		if(!(q.getOperator() instanceof InvokeStatic))
			return;
		
		jq_Method invokeMethod = InvokeStatic.getMethod(q).getMethod();
		ParamListOperand params = InvokeStatic.getParamList(q);
		int paramsLen = params.length();
		
		// INVOKESTATIC_A T5, access$002, (R0, T3) 	-> PUTFIELD_A 	R0, .f, T3
		if(putFieldAccessMethodMap.containsKey(invokeMethod)){
			jq_Field mField = putFieldAccessMethodMap.get(invokeMethod);
			FieldOperand mFieldOperand = new FieldOperand(mField);
			
			assert(paramsLen == 2);
			Operand baseRegisterOperand = params.get(0);
			Operand srcRegisterOperand = params.get(1);
			
			q.setOperator(Putfield.PUTFIELD_A.INSTANCE);
			q.setOp1(null);
			q.setOp2(null);
			q.setOp3(null);
			q.setOp4(null);
			Putfield.setBase(q, baseRegisterOperand);
			Putfield.setField(q, mFieldOperand);
			Putfield.setSrc(q, srcRegisterOperand);
			Putfield.setGuard(q, null);
			
			return;
		}
		
		// INVOKESTATIC_A T7, access$102, (T6)		-> PUTSTATIC_A 	T6, .f
		if(putStaticAccessMethodMap.containsKey(invokeMethod)){
			jq_Field mField = putStaticAccessMethodMap.get(invokeMethod);
			FieldOperand mFieldOperand = new FieldOperand(mField);
			
			assert(paramsLen == 1);
			Operand srcRegisterOperand = params.get(0);
			
			q.setOperator(Putstatic.PUTSTATIC_A.INSTANCE);
			q.setOp1(null);
			q.setOp2(null);
			q.setOp3(null);
			q.setOp4(null);
			Putstatic.setField(q, mFieldOperand);
			Putstatic.setSrc(q, srcRegisterOperand);
			
			return;
		}
		
		// INVOKESTATIC_A T5, access$000, (R4)		-> GETFIELD_A 	T5, R4, .f
		if(getFieldAccessMethodMap.containsKey(invokeMethod)){
			jq_Field mField = getFieldAccessMethodMap.get(invokeMethod);
			FieldOperand mFieldOperand = new FieldOperand(mField);
			
			assert(paramsLen == 1);
			Operand baseRegisterOperand = params.get(0);
			
			RegisterOperand destRegisterOperand = InvokeStatic.getDest(q);
			
			q.setOperator(Getfield.GETFIELD_A.INSTANCE);
			q.setOp1(null);
			q.setOp2(null);
			q.setOp3(null);
			q.setOp4(null);
			Getfield.setBase(q, baseRegisterOperand);
			Getfield.setDest(q, destRegisterOperand);
			Getfield.setField(q, mFieldOperand);
			Getfield.setGuard(q, null);
			
			return;
		}
		
		// INVOKESTATIC_A T8, access$100, ()		-> GETSTATIC_A 	T8, .f
		if(getStaticAccessMethodMap.containsKey(invokeMethod)){
			jq_Field mField = getStaticAccessMethodMap.get(invokeMethod);
			FieldOperand mFieldOperand = new FieldOperand(mField);
			
			assert(paramsLen == 0);
			
			RegisterOperand destRegisterOperand = InvokeStatic.getDest(q);
			
			q.setOperator(Getstatic.GETSTATIC_A.INSTANCE);
			q.setOp1(null);
			q.setOp2(null);
			q.setOp3(null);
			q.setOp4(null);
			Getstatic.setDest(q, destRegisterOperand);
			Getstatic.setField(q, mFieldOperand);
			
			return;
		}
	}
}
