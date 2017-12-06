package drrace.analyses.thead.modelMethod;

import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.type.AndroidTypeAnalysis;
import joeq.Class.jq_Method;

/*
 * handlePostRunnableM(m) - handlePostRunnable:(Ljava/lang/Runnable;)V@PostRunnable
 * This method is used to handle the runnable posted to looper thread
 */

@Chord(
	    name = "handlePostRunnableM",
	    sign = "M0:M0"
	)

public class RelHandlePostRunnableM extends ProgramRel{
	private AndroidTypeAnalysis androidTypeAnalysis = AndroidTypeAnalysis.getInstance();
	
	@Override
	public void fill(){
		// When the apk does not post events to looper thread
		// handlePostRunnableM == null
		// handlePostRunnable:(Ljava/lang/Runnable;)V@PostRunnable
		jq_Method handlePostRunnableM = androidTypeAnalysis.getHandlePostRunnable();
		if(handlePostRunnableM != null)
			super.add(handlePostRunnableM);
	}
}
