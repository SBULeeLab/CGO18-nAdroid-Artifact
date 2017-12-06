package drrace.analyses.free;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Dataflow.ReachingDefs;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.Quad;

/*
 * This ProgramRel RelFreeE is a set of statements that free fields.
 * RelFreeE only contains: ((3) and (4) are used after inline analysis)
 * (1) PUTFIELD_A 	R0, 	.f, 	Null
 * (2) PUTSTATIC_A 	Null, 	.f
 * 
 * (3) MOVE_A 		R1, 	Null
 * 	   PUTFIELD_A 	R0, 	.f, 	R1
 * (4) MOVE_A 		R1, 	Null
 *     PUTSTATIC_A 	R1, 	.f
 * 
 * We can ignore access$ for free with inline analysis
 * Inner class setting fields of outer class to null is not included.
 * (it means: using access$... method to free fields is not included.)
 */

@Chord(
	name = "freeE",
	sign = "E0:E0"
)

public class RelFreeE extends ProgramRel implements IHeapInstVisitor {
	private jq_Class mClass;
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	// mMethod is the method to be analyzed
	private jq_Method mMethod;
	// mReachingDefs is used for reaching definition data-flow analysis
	private ReachingDefs mReachingDefs;
	
	// quadBBMap is a Map<Quad,BasicBlock>
	// given a Quad, we can know the BasicBlock the Quad belonging to 
	private Map<Quad,BasicBlock> quadBBMap = new HashMap<Quad,BasicBlock>();
	
	@Override
	public void visit(jq_Class c) {
		mClass = c;
	}
	
	@Override
	public void visit(jq_Method m) {
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		// ignore abstract method
		if(m.isAbstract())
			return;
		
		mMethod = m;
		ControlFlowGraph cfg = mMethod.getCFG();
		mReachingDefs = ReachingDefs.solve(cfg);
		
		// generate quadBBMap for data flow analysis
		ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
		while(it.hasNext()){
			BasicBlock b = it.nextBasicBlock();
			for (int i = 0; i < b.size(); i++){
				Quad q = b.getQuad(i);
				quadBBMap.put(q, b);
			}
		}
		
	}

	@Override
	public void visitHeapInst(Quad q) {
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		
		Operator op = q.getOperator();
		
		Operand srcOperand = null;
		if(op instanceof Putfield.PUTFIELD_A){
			srcOperand = Putfield.getSrc(q);
		} else if(op instanceof Putstatic.PUTSTATIC_A){
			srcOperand = Putstatic.getSrc(q);
		}
		
		if(srcOperand == null)
			return;
		
		// (1) PUTFIELD_A 	R0, 	.f, 	Null
		// (2) PUTSTATIC_A 	Null, 	.f
		if(srcOperand instanceof AConstOperand){
			AConstOperand mAConstOperand = (AConstOperand) srcOperand;
			if(mAConstOperand.toString().equals("AConst: null")){
				System.out.println(q);
				super.add(q);
				return;
			}
		}
		
		
		// (3) MOVE_A 		R1, 	Null
		// 	   PUTFIELD_A 	R0, 	.f, 	R1
		// (4) MOVE_A 		R1, 	Null
		//     PUTSTATIC_A 	R1, 	.f
		if(srcOperand instanceof RegisterOperand){
			RegisterOperand mRegisterOperand = (RegisterOperand) srcOperand;
			
			@SuppressWarnings("unchecked")
			Set<Quad> dataFlowQs = mReachingDefs.getReachingDefs(quadBBMap.get(q), q, mRegisterOperand.getRegister());
			
			if(dataFlowQs.size() != 1)
				return;
			
			Quad quadDefineRegister = dataFlowQs.iterator().next();
			
			if(quadDefineRegister.getOperator() instanceof Move.MOVE_A){
				Operand moveSrcOperand = Move.getSrc(quadDefineRegister);
				if(moveSrcOperand instanceof AConstOperand){
					AConstOperand mAConstOperand = (AConstOperand) moveSrcOperand;
					if(mAConstOperand.toString().equals("AConst: null")){
						System.out.println(q);
						super.add(q);
						return;
					}
				}
			}
		}
	}
	
}