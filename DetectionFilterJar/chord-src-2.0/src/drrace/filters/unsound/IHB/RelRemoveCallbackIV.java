package drrace.filters.unsound.IHB;

import chord.program.Program;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

/*
 * removeCallbackInvoke(k,i,v):
 * 
 * removeCallbackInvoke(0,i,v):
 * i: unbindService
 * v: ServiceConnection Register
 *
 * removeCallbackInvoke(1,i,v):
 * i: unregisterReceiver
 * v: BroadcastReceiver Register
 * 
 * removeCallbackInvoke(2,i,v):
 * i: removeCallbacksAndMessages(null)
 * v: Handler Register
 */

@Chord(
	    name = "removeCallbackIV",
	    sign = "K0,I0,V0:V0_I0_K0"
	)

public class RelRemoveCallbackIV extends ProgramRel implements IInvokeInstVisitor{
	
	private jq_Class mClass;
	
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	private jq_Method unbindService = 
			Program.g().getMethod("unbindService:(Landroid/content/ServiceConnection;)V@android.content.ContextWrapper");
	
	private jq_Method unregisterReceiver0 = 
			Program.g().getMethod("unregisterReceiver:(Landroid/content/BroadcastReceiver;)V@android.content.ContextWrapper");
	private jq_Method unregisterReceiver1 = 
			Program.g().getMethod("unregisterReceiver:(Landroid/content/BroadcastReceiver;)V@android.support.v4.content.LocalBroadcastManager");
	
	private jq_Method removeCallbacksAndMessages = 
			Program.g().getMethod("removeCallbacksAndMessages:(Ljava/lang/Object;)V@android.os.Handler");
	
	@Override
	public void visit(jq_Class c) {
		mClass = c;
	}
	
	@Override
	public void visit(jq_Method m) {}
	
	@Override
	public void visitInvokeInst(Quad q) {
		// ignore this relation when all the selected invoke methods are not in RTA scope 
		if(unbindService == null &&
				unregisterReceiver0 == null &&
				unregisterReceiver1 == null &&
				removeCallbacksAndMessages == null)
			return;
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		
		jq_Method invokeMethod = Invoke.getMethod(q).getMethod();
		
		if(unbindService != null
				&& invokeMethod == unbindService){
			Register mRegister = Invoke.getParam(q, 1).getRegister();
			super.add(0,q, mRegister);
			return;
		}
		
		if(unregisterReceiver0 != null
				&& invokeMethod == unregisterReceiver0){
			Register mRegister = Invoke.getParam(q, 1).getRegister();
			super.add(1,q, mRegister);
			return;
		}
		
		if(unregisterReceiver1 != null
				&& invokeMethod == unregisterReceiver1){
			Register mRegister = Invoke.getParam(q, 1).getRegister();
			super.add(1,q, mRegister);
			return;
		}
		
		if(removeCallbacksAndMessages != null
				&& invokeMethod == removeCallbacksAndMessages){
			ParameterAnalysis parameterAnalysis = new ParameterAnalysis(q);
			if(parameterAnalysis.parameterIsNull()){
				Register mRegister = Invoke.getParam(q, 0).getRegister();
				super.add(2,q, mRegister);
			}
			return;
		}
	}
}
