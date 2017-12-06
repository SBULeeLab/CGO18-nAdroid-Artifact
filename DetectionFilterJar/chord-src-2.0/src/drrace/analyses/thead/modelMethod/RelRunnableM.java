package drrace.analyses.thead.modelMethod;

import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * runnableM(0,m) - all run methods (implementing Runnable)
 * runnableM(1,m) - doInBackgroundRunnable
 * runnableM(2,m) - onPreExecuteRunnable
 * runnableM(3,m) - onPostExecuteRunnable
 * runnableM(4,m) - onProgressUpdateRunnable
 * runnableM(5,m) - handleMessageRunnable
 * runnableM(6,m) - onServiceConnectedRunnable
 * runnableM(7,m) - onServiceDisconnectedRunnable
 * runnableM(8,m) - onReceiveRunnable
 */

@Chord(
	    name = "runnableM",
	    sign = "K0,M0:K0_M0"
	)

public class RelRunnableM extends ProgramRel implements IClassVisitor{
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	@Override
	public void visit(jq_Class mClass) {
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		
		// ignore the class not implementing Runnable
		if(!androidTypeAnalysis.impRunnable(mClass))
			return;
		
		// m is the implementing Run method.
		jq_Method m = androidTypeAnalysis.getRunMethod(mClass);
		
		// sometimes the run method is not in RTA Scope (in this case, m == null)
		if(m == null)
			return;
		
		// all the Run methods.
		super.add(0,m);
		
		// doInBackgroundRunnable
		if(androidTypeAnalysis.isDoInBackgroundRunnable(mClass)){
			super.add(1,m);
			return;
		}
		// onPreExecuteRunnable
		if(androidTypeAnalysis.isOnPreExecuteRunnable(mClass)){
			super.add(2,m);
			return;
		}
		// onPostExecuteRunnable
		if(androidTypeAnalysis.isOnPostExecuteRunnable(mClass)){
			super.add(3,m);
			return;
		}
		// onProgressUpdateRunnable
		if(androidTypeAnalysis.isOnProgressUpdateRunnable(mClass)){
			super.add(4,m);
			return;
		}
		
		// handleMessageRunnable
		if(androidTypeAnalysis.isHandleMessageRunnable(mClass)){
			super.add(5,m);
			return;
		}
		
		// onServiceConnectedRunnable
		if(androidTypeAnalysis.isOnServiceConnectedRunnable(mClass)){
			super.add(6,m);
			return;
		}
		
		// onServiceDisconnectedRunnable
		if(androidTypeAnalysis.isOnServiceDisconnectedRunnable(mClass)){
			super.add(7,m);
			return;
		}
		
		// onReceiveRunnable
		if(androidTypeAnalysis.isOnReceiveRunnable(mClass)){
			super.add(8,m);
			return;
		}
	}
}
