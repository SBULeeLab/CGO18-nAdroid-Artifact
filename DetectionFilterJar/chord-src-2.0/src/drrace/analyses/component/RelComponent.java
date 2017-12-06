package drrace.analyses.component;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import drrace.analyses.component.ComponentAnalysis.ComponentType;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;

/*
 * This relation component(k,t)
 * t is the type of an android component
 * component(0,t): Activity
 * component(1,t): Service
 * component(2,t): ContentProvider
 * component(3,t): BroadcastReceiver
 * component(4,t): Application
 */

@Chord(
	    name = "component",
	    sign = "K0,T0:T0_K0"
	)

public class RelComponent extends ProgramRel{
	@Override
    public void fill() {
		System.out.println("Enter Building RelComponent");
		
		ComponentAnalysis componentAnalysis = ComponentAnalysis.getInstance();
		
		final ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("componentCallbackPair");
		rel.load();
		
		PairIterable<jq_Type, jq_Method> tuples = rel.getAry2ValTuples();
		for(Pair<jq_Type, jq_Method> tuple : tuples){
			jq_Class componentClass = (jq_Class) tuple.val0;
			ComponentType componentType = componentAnalysis.getComponentType(componentClass);
			
			switch(componentType){
			case Activity:
				super.add(0,componentClass);
				break;
			case Service:
				super.add(1,componentClass);
				break;
			case ContentProvider:
				super.add(2,componentClass);
				break;
			case BroadcastReceiver:
				super.add(3,componentClass);
				break;
			case Application:
				super.add(4,componentClass);
				break;
			default:
				System.err.println("can not find a component type for: " + componentClass);
				System.exit(1);
				break;
			}
			
		}
		
		rel.close();
		
		System.out.println("Leave Building RelComponent");
	}
}
