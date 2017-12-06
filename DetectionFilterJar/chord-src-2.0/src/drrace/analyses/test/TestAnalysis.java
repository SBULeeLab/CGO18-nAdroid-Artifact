package drrace.analyses.test;

import java.util.HashSet;
import java.util.Set;

import chord.analyses.heapacc.DomE;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.UTF.Utf8;
import joeq.Util.Templates.List.ExceptionHandler;
import joeq.Util.Templates.ListIterator;

@Chord(name="test-java")
public class TestAnalysis extends JavaAnalysis{
	
	public void run(){
		ClassicProject.g().runTask("componentCallbackPair");
		
		/*
		jq_Class mClass0;
		jq_Method mMethod0;
		mClass0 = (jq_Class) Program.g().getClass("com.google.zxing.client.android.CaptureActivity");
		mMethod0 = mClass0.getDeclaredMethod("initCamera");
		System.out.println(mMethod0.getCFG().fullDump());
		
		//ClassicProject.g().runTask("componentCallbackPair");
		//ClassicProject.g().runTask("drrace-inline");
		//ClassicProject.g().runTask("freeE");
		
		/*
		jq_Class mClass0;
		jq_Method mMethod0;
		mClass0 = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$1");
		mMethod0 = mClass0.getDeclaredMethod("handleMessage");
		
		jq_Class mClass1;
		jq_Method mMethod1;
		mClass1 = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$2");
		mMethod1 = mClass1.getDeclaredMethod("handleMessage");
		
		System.out.println("method: " + mMethod0);
		System.out.println(mMethod0.getCFG().fullDump());
		
		System.out.println("method: " + mMethod1);
		System.out.println(mMethod1.getCFG().fullDump());
		
		ClassicProject.g().runTask("drrace-inline");
		
		System.out.println("method: " + mMethod0);
		System.out.println(mMethod0.getCFG().fullDump());
		
		System.out.println("method: " + mMethod1);
		System.out.println(mMethod1.getCFG().fullDump());
		
		/*
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$1");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$2");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$3");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		ClassicProject.g().runTask("drrace-inline");
		
		/*
		AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
		
		IndexSet<jq_Method> methods = Program.g().getMethods();
		
		for(jq_Method method : methods){
			if(androidTypeAnalysis.isLibClass(method.getDeclaringClass()))
				continue;
			if(method.isAbstract())
				continue;
			
			String methodName = method.getName().toString();
			if(methodName.startsWith("access$")){
				System.out.println("method: " + method);
				System.out.println(method.getCFG().fullDump());
			}
		}
		*/
		//ClassicProject.g().runTask("LE");
		
		
		
		/*
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity");
		mMethod = mClass.getDeclaredMethod("onCreate");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		*/
		/*
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$1");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$2");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		mClass = (jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testinline.MainActivity$3");
		mMethod = mClass.getDeclaredMethod("handleMessage");
		System.out.println("method: " + mMethod);
		System.out.println(mMethod.getCFG().fullDump());
		
		/*
		jq_Class mClass = 
				(jq_Class) Program.g().getClass("com.example.fuxinwei.drrace2testlock.MainActivity");
		
		jq_Method mMethod;
		ControlFlowGraph mCFG;
		
		mMethod = mClass.getDeclaredMethod("run");
		mCFG = mMethod.getCFG();
		System.out.println(mCFG.fullDump());
		
		EntryOrExitBasicBlock entry = mCFG.entry();
		visit(entry);
		*/
	}
	
	
}
