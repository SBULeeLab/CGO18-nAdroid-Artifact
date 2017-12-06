package drrace.analyses.thead.modelMethod;

import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * modelMethod(0,m) - onServiceConnected
 * modelMethod(1,m) - onServiceDisconnected
 * modelMethod(2,m) - onReceive
 * modelMethod(3,m) - handleMessage
 */

@Chord(
	    name = "modelMethod",
	    sign = "K0,M0:K0_M0"
	)

public class RelModelMethod extends ProgramRel implements IClassVisitor{
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	@Override
	public void visit(jq_Class mClass) {
		// ignore library classes
		if(androidTypeAnalysis.isLibClass(mClass))
			return;
		
		// ServiceConnection Implementation
		if(androidTypeAnalysis.impServiceConnection(mClass)){
			jq_Method onServiceConnected = androidTypeAnalysis.getOnServiceConnected(mClass);
			jq_Method onServiceDisconnected = androidTypeAnalysis.getOnServiceDisconnected(mClass);
			
			if(onServiceConnected != null)
				super.add(0,onServiceConnected);
			if(onServiceDisconnected != null)
				super.add(1,onServiceDisconnected);
		}
		
		// BroadcastReceiver Extension
		if(androidTypeAnalysis.isBroadcastReceiverExt(mClass)){
			jq_Method onReceive = androidTypeAnalysis.getOnReceive(mClass);
			if(onReceive != null)
				super.add(2,onReceive);
			return;
		}
		
		// Handler Extension
		if(androidTypeAnalysis.isHandlerExt(mClass)){
			jq_Method handleMessage = androidTypeAnalysis.getHandleMessage(mClass);
			if(handleMessage != null)
				super.add(3,handleMessage);
			return;
		}
	}
}
