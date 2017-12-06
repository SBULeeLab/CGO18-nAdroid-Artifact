package drrace.analyses.component;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * This relation componentCallbackPair(t,m)
 * t is the type of an android component
 * m is a entry call back of this android component
 */

@Chord(
	    name = "componentCallbackPair",
	    sign = "T0,M0:T0_M0"
	)

public class RelComponentCallbackPair extends ProgramRel{
	@Override
    public void fill() {
		System.out.println("Enter Building RelComponentCallbackPair");
		
		ComponentAnalysis componentAnalysis = ComponentAnalysis.getInstance();
		Map<String, HashSet<jq_Method>> allCallbacks = componentAnalysis.getAllCallbacks();
		for(Entry<String, HashSet<jq_Method>> entry : allCallbacks.entrySet()){
			String componentClassName = entry.getKey();
			jq_Class componentClass = (jq_Class) Program.g().getClass(componentClassName);
			HashSet<jq_Method> callbacks = entry.getValue();
			for(jq_Method callback : callbacks){
				super.add(componentClass,callback);
			}
		}
		
		System.out.println("Leave Building RelComponentCallbackPair");
	}
}
