package drrace.detector;

import java.util.Arrays;
import java.util.List;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import drrace.epilogue.path.PathPrinter;
import drrace.epilogue.printer.DrRacePrinter_dlog;
import drrace.epilogue.rel.RelCounter;

@Chord(
	name="drrace-epilogue-java"
)

/*
 * print result
 * print classification result
 * print classification of threads
 * print Path
 */

public class DrRaceEpilogue extends JavaAnalysis{
	
	private final boolean printResult = 
			Config.buildBoolProperty("drrace.epilogue.printResult", true);
	private final boolean printClassficationResult = 
			Config.buildBoolProperty("drrace.epilogue.printClassficationResult", true);
	private final boolean printClassficationThread = 
			Config.buildBoolProperty("drrace.epilogue.printClassficationThread", true);
	private final boolean printPath = 
			Config.buildBoolProperty("drrace.epilogue.printPath", true);
	
	@Override
	public void run() {
		// print result
		if(printResult)
			printResult();
		
		// print classification result
		if(printClassficationResult)
			printClassficationResult();
		
		// print classification of threads
		if(printClassficationThread)
			printClassficationThread();
		
		// print Path
		if(printPath)
			printPath();
	}
	
	private void printResult(){
		// using dlog to generate F_MM_MM
		// using relView to generate F_MM_MM in DrRacePrinter needs lots of memory and is slow
		DrRacePrinter_dlog mDrRacePrinter_dlog = new DrRacePrinter_dlog("Use_after_Free_Races");
		mDrRacePrinter_dlog.print("uaf_TInOneComponent");
		
		/*
		 * Sound filters results
		 */
		// filter: use for comparison
		mDrRacePrinter_dlog.print("uaf_UsedForCmp");
		mDrRacePrinter_dlog.print("uaf_UsedForCmp_Excluded");
		// filter: if guard
		mDrRacePrinter_dlog.print("uaf_IfGuardE");
		mDrRacePrinter_dlog.print("uaf_IfGuardE_Excluded");
		// filter: intra-Allocation
		mDrRacePrinter_dlog.print("uaf_AllocE");
		mDrRacePrinter_dlog.print("uaf_AllocE_Excluded");
		// filter: MHB
		mDrRacePrinter_dlog.print("uaf_MHB");
		mDrRacePrinter_dlog.print("uaf_MHB_Excluded");
		// filter: lock with ifGuard and intra_allocation
		mDrRacePrinter_dlog.print("uaf_lock");
		mDrRacePrinter_dlog.print("uaf_lock_Excluded");
		
		/*
		 * Unsound filters results
		 */
		// filter: use for return
		mDrRacePrinter_dlog.print("uaf_UsedForRtn");
		mDrRacePrinter_dlog.print("uaf_UsedForRtn_Excluded");
		// filter: use for parameter
		mDrRacePrinter_dlog.print("uaf_UsedForParam");
		mDrRacePrinter_dlog.print("uaf_UsedForParam_Excluded");
		// filter: intra-Maybe-Allocation
		mDrRacePrinter_dlog.print("uaf_maybeAllocE");
		mDrRacePrinter_dlog.print("uaf_maybeAllocE_Excluded");
		// filter: PHB
		mDrRacePrinter_dlog.print("uaf_PHB");
		mDrRacePrinter_dlog.print("uaf_PHB_Excluded");
		// filter: RHB
		mDrRacePrinter_dlog.print("uaf_RHB");
		mDrRacePrinter_dlog.print("uaf_RHB_Excluded");
		// filter: FHB
		mDrRacePrinter_dlog.print("uaf_FHB");
		mDrRacePrinter_dlog.print("uaf_FHB_Excluded");
		// filter: IHB
		mDrRacePrinter_dlog.print("uaf_IHB");
		mDrRacePrinter_dlog.print("uaf_IHB_Excluded");
		
		/*
		 * Applying all the sound and unsound filers
		 */
		mDrRacePrinter_dlog.print("uaf_sound_filters_applied");
		mDrRacePrinter_dlog.print("uaf_ToBeReported");
		
		/*
		 * Gathering all outputs
		 */
		String[] relNameArray = {
				"uaf_TInOneComponent",
				
				"uaf_UsedForCmp_Excluded",
				"uaf_IfGuardE_Excluded",
				"uaf_AllocE_Excluded",
				"uaf_MHB_Excluded",
				"uaf_lock_Excluded",
				
				"uaf_UsedForRtn_Excluded",
				"uaf_UsedForParam_Excluded",
				"uaf_maybeAllocE_Excluded",
				"uaf_PHB_Excluded",
				"uaf_RHB_Excluded",
				"uaf_FHB_Excluded",
				"uaf_IHB_Excluded",
				
				"uaf_sound_filters_applied",
				"uaf_ToBeReported"
		};
		List<String> relNameList = Arrays.asList(relNameArray);
		mDrRacePrinter_dlog.print(relNameList);
	}
	
	private void printClassficationResult(){
		// classification dlog
		ClassicProject.g().runTask("drrace-classification-dlog");
		
		// using dlog to generate F_MM_MM
		// using relView to generate F_MM_MM in DrRacePrinter needs lots of memory and is slow
		DrRacePrinter_dlog mDrRacePrinter_dlog = new DrRacePrinter_dlog("Use_after_Free_Races_Classfication");
		
		// filter: use for comparison
		mDrRacePrinter_dlog.print("uaf_EntryEvent_EntryEvent");
		mDrRacePrinter_dlog.print("uaf_EntryEvent_PostEvent");
		mDrRacePrinter_dlog.print("uaf_PostEvent_PostEvent");
		mDrRacePrinter_dlog.print("uaf_Event_Thread_Reach");
		mDrRacePrinter_dlog.print("uaf_Event_Thread_NonReach");
		mDrRacePrinter_dlog.print("uaf_Thread_Thread");
		
		/*
		 * Gathering all outputs
		 */
		String[] relNameArray = {
				"uaf_EntryEvent_EntryEvent",
				"uaf_EntryEvent_PostEvent",
				"uaf_PostEvent_PostEvent",
				"uaf_Event_Thread_Reach",
				"uaf_Event_Thread_NonReach",
				"uaf_Thread_Thread",
		};
		List<String> relNameList = Arrays.asList(relNameArray);
		mDrRacePrinter_dlog.print(relNameList);
	}
	
	
	private void printPath(){
		// classification dlog
		ClassicProject.g().runTask("drrace-classification-dlog");
		
		PathPrinter mPathPrinter = new PathPrinter();
		
		mPathPrinter.init("uaf_EntryEvent_EntryEvent");
		mPathPrinter.printThrOblPath();
		
		mPathPrinter.init("uaf_EntryEvent_PostEvent");
		mPathPrinter.printThrOblPath();
		
		mPathPrinter.init("uaf_PostEvent_PostEvent");
		mPathPrinter.printThrOblPath();
		
		mPathPrinter.init("uaf_Event_Thread_Reach");
		mPathPrinter.printThrOblPath();
		
		mPathPrinter.init("uaf_Event_Thread_NonReach");
		mPathPrinter.printThrOblPath();
		
		mPathPrinter.init("uaf_Thread_Thread");
		mPathPrinter.printThrOblPath();
	}
	
	private void printClassficationThread(){
		RelCounter mRelCounter = new RelCounter();
		
		String[] relNameArray = {
				"threadEntryEvent",
				"threadPostEvent",
				"threadNative",
		};
		List<String> relNameList = Arrays.asList(relNameArray);
		mRelCounter.printRelSize(relNameList, "ThreadClassfication");
		
	}
	
}
