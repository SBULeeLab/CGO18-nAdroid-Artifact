package drrace.analyses.component;

import java.util.Map;
import java.util.Map.Entry;

import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * This relation componentHandleMethodPair(t,m)
 * t is the type of an android component
 * m is the handle method in dummy main method of this component
 */

@Chord(
	    name = "componentHandleMethodPair",
	    sign = "T0,M0:T0_M0"
	)

public class RelComponentHandleMethodPair extends ProgramRel{
	@Override
    public void fill() {
		System.out.println("Enter Building RelComponentHandleMethodPair");
		
		ComponentAnalysis componentAnalysis = ComponentAnalysis.getInstance();
		Map<jq_Class, jq_Method> componentHandleMethodMap = componentAnalysis.getComponentHandleMethodMap();
		for(Entry<jq_Class, jq_Method> entry : componentHandleMethodMap.entrySet()){
			super.add(entry.getKey(),entry.getValue());
		}
		
		System.out.println("Leave Building RelComponentHandleMethodPair");
	}
}
