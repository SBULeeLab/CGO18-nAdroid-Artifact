package drrace.analyses.thead.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import drrace.analyses.component.ComponentAnalysis;
import drrace.analyses.component.ComponentAnalysis.ComponentType;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * lifeCycleCallback(k0,k1,m)
 * 
 * (1) Activity:
 * lifeCycleCallback(0,0,m) - onCreate
 * lifeCycleCallback(0,1,m) - onRestart
 * lifeCycleCallback(0,2,m) - onStart
 * lifeCycleCallback(0,3,m) - onResume
 * lifeCycleCallback(0,4,m) - onPause
 * lifeCycleCallback(0,5,m) - onStop
 * lifeCycleCallback(0,6,m) - onDestroy
 * 
 * (Happens between onStop and onDestroy)
 * lifeCycleCallback(0,7,m) - onRetainNonConfigurationInstance
 * 
 * (2) Service
 * lifeCycleCallback(1,0,m) - onCreate
 * lifeCycleCallback(1,1,m) - onDestroy
 */

@Chord(
	    name = "lifeCycleCallback",
	    sign = "K0,K1,M0:K0xK1_M0"
	)

public class RelLifeCycleCallback extends ProgramRel{
	
	ComponentAnalysis mComponentAnalysis = ComponentAnalysis.getInstance();
	
	private Map<String, HashMap<String,jq_Method>> overrideCallbacks = new HashMap<String, HashMap<String,jq_Method>>();
	
	private final int activityIndex=0;
	private final int serviceIndex=1;
	
	private List<String> activityLifeCycleCallbackNDList = new ArrayList<String>();
	private final String activityOnCreateND = "onCreate(Landroid/os/Bundle;)V";
	private final String activityOnRestartND = "onRestart()V";
	private final String activityOnStartND = "onStart()V";
	private final String activityOnResumeND = "onResume()V";
	private final String activityOnPauseND = "onPause()V";
	private final String activityOnStopND = "onStop()V";
	private final String activityOnDestroyND = "onDestroy()V";
	
	private final String activityOnRetainND = "onRetainNonConfigurationInstance()Ljava/lang/Object;";
	
	private List<String> serviceLifeCycleCallbackNDList = new ArrayList<String>();
	private final String serviceOnCreateND = "onCreate()V";
	private final String serviceOnDestroyND = "onDestroy()V";
	
	@Override
    public void fill() {
		System.out.println("Enter Building RelActivityLifeCycleCallback");
		
		lifeCycleCallbackNDListInit();
		
		overrideCallbacks = mComponentAnalysis.getOverrideCallbacks();
		for(Entry<String, HashMap<String, jq_Method>> entry : overrideCallbacks.entrySet()){
			String componentClassName = entry.getKey();
			
			jq_Class componentClass = (jq_Class) Program.g().getClass(componentClassName);
		    System.out.println("EventClass: " + componentClass);
		    
		    ComponentType componentType = mComponentAnalysis.getComponentType(componentClass);
		    System.out.println("ComponentType: " + componentType);
		    
		    switch(componentType){
		    	case Activity:
		    		addToRel(activityIndex, activityLifeCycleCallbackNDList, entry.getValue());
		    		break;
		    	case Service:
		    		addToRel(serviceIndex, serviceLifeCycleCallbackNDList, entry.getValue());
		    		break;
		    	default:
		    		break;
		    }
		}
		
		System.out.println("Leave Building RelActivityLifeCycleCallback");
	}
	
	private void lifeCycleCallbackNDListInit(){
		activityLifeCycleCallbackNDList.add(activityOnCreateND);
		activityLifeCycleCallbackNDList.add(activityOnRestartND);
		activityLifeCycleCallbackNDList.add(activityOnStartND);
		activityLifeCycleCallbackNDList.add(activityOnResumeND);
		activityLifeCycleCallbackNDList.add(activityOnPauseND);
		activityLifeCycleCallbackNDList.add(activityOnStopND);
		activityLifeCycleCallbackNDList.add(activityOnDestroyND);
		
		activityLifeCycleCallbackNDList.add(activityOnRetainND);
		
		serviceLifeCycleCallbackNDList.add(serviceOnCreateND);
		serviceLifeCycleCallbackNDList.add(serviceOnDestroyND);
	}
	
	private void addToRel(int index, List<String> mNDList, Map<String, jq_Method> callbackMap){
		for(String lifeCycleCallbackND : mNDList){
			if(callbackMap.containsKey(lifeCycleCallbackND)){
				super.add(index, 
							mNDList.indexOf(lifeCycleCallbackND), 
							callbackMap.get(lifeCycleCallbackND));
				System.out.println("LifeCycleCallback: " + callbackMap.get(lifeCycleCallbackND));
			}
		}
	}
}
