package drrace.analyses.type;

import chord.program.Program;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/*
 * This is a singleton class for android type analysis.
 */

public class AndroidTypeAnalysis {
	private static AndroidTypeAnalysis instance;
		
	public static AndroidTypeAnalysis getInstance(){
		if (instance == null)
			instance = new AndroidTypeAnalysis();
		return instance;
	}
		
	/*----------------------------------------------------------------------------------------
	 * AsyncTask Begins
	/*---------------------------------------------------------------------------------------*/
	private final jq_Class asyncTaskClass = (jq_Class) Program.g().getClass("android.os.AsyncTask");
	
	private final String dIBMethodND = "doInBackground:([Ljava/lang/Object;)Ljava/lang/Object;";
	private final String onPreExeMethodND ="onPreExecute:()V";
	private final String onPostExeMethodND ="onPostExecute:(Ljava/lang/Object;)V";
	private final String onUpdateMethodND ="onProgressUpdate:([Ljava/lang/Object;)V";
	
	private final String dIBMethod2ND = "doInBackground2:([Ljava/lang/Object;)V";
	private final String onUpdateMethod2ND ="onProgressUpdate2:([Ljava/lang/Object;)V";
	
	public boolean existAsyncTaskClass(){
		return asyncTaskClass != null;
	}
	
	public boolean isAsyncTaskExt(jq_Class mClass){
		return isExtof(mClass, asyncTaskClass);
	}
	
	public jq_Method getDoInBackground2(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(dIBMethod2ND + "@" + mClass.toString());
	}
	
	public jq_Method getOnProgressUpdate2(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(onUpdateMethod2ND + "@" + mClass.toString());
	}
	
	public jq_Method getDoInBackground(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(dIBMethodND + "@" + mClass.toString());
	}
	
	public jq_Method getOnPreExecute(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(onPreExeMethodND + "@" + mClass.toString());
	}
	
	public jq_Method getOnPostExecute(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(onPostExeMethodND + "@" + mClass.toString());
	}
	
	public jq_Method getOnProgressUpdate(jq_Class mClass){
		assert(isAsyncTaskExt(mClass));
		return Program.g().getMethod(onUpdateMethodND + "@" + mClass.toString());
	}
	
	/*----------------------------------------------------------------------------------------
	 * AsyncTask Ends
	/*---------------------------------------------------------------------------------------*/
		
	/*----------------------------------------------------------------------------------------
	 * AsyncTask Runnable Begins
	/*---------------------------------------------------------------------------------------*/
	
	public boolean isAsyncTaskRunnable(jq_Class mClass){
		return isDoInBackgroundRunnable(mClass)
				|| isOnProgressUpdateRunnable(mClass)
				|| isOnPreExecuteRunnable(mClass)
				|| isOnPostExecuteRunnable(mClass);
	}
	
	public boolean isDoInBackgroundRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$doInBackgroundRunnable"))
			return true;
		else
			return false;
	}
	
	public boolean isOnProgressUpdateRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$onProgressUpdateRunnable"))
			return true;
		else
			return false;
	}
	
	public boolean isOnPreExecuteRunnable(jq_Class mClass){
		if(impRunnable(mClass)
				&& mClass.getName().endsWith("$onPreExecuteRunnable"))
			return true;
		else
			return false;
	}
	
	public boolean isOnPostExecuteRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$onPostExecuteRunnable"))
			return true;
		else
			return false;
	}
	
	/*----------------------------------------------------------------------------------------
	 * AsyncTask Runnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * Handler Begins
	/*---------------------------------------------------------------------------------------*/
	
	private final jq_Class handlerClass = (jq_Class) Program.g().getClass("android.os.Handler");
	
	private final String handleMessageND = "handleMessage:(Landroid/os/Message;)V";
	
	private final String handleMessage2ND = "handleMessage2:(Landroid/os/Message;)Ljava/lang/Runnable;";
	
	public boolean isHandlerExt(jq_Class mClass){
		return isExtof(mClass, handlerClass);
	}
	
	public jq_Method getHandleMessage2(jq_Class mClass){
		assert(isHandlerExt(mClass));
		return Program.g().getMethod(handleMessage2ND + "@" + mClass.toString());
	}
	
	public jq_Method getHandleMessage(jq_Class mClass){
		assert(isHandlerExt(mClass));
		return Program.g().getMethod(handleMessageND + "@" + mClass.toString());
	}
	
	/*----------------------------------------------------------------------------------------
	 * Handler Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * Handler Runnable Begins
	/*---------------------------------------------------------------------------------------*/
	
	public boolean isHandleMessageRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$handleMessageRunnable"))
			return true;
		else
			return false;
	}
	
	/*----------------------------------------------------------------------------------------
	 * Handler Runnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * ServiceConnection Begins
	/*---------------------------------------------------------------------------------------*/
	
	private final jq_Class serviceConnectionClass = 
			(jq_Class) Program.g().getClass("android.content.ServiceConnection");
	
	private final String onServiceConnectedND = 
			"onServiceConnected:(Landroid/content/ComponentName;Landroid/os/IBinder;)V";
	private final String onServiceDisconnectedND = 
			"onServiceDisconnected:(Landroid/content/ComponentName;)V";
	
	private final String onServiceConnected2ND = 
			"onServiceConnected2:(Landroid/content/ComponentName;Landroid/os/IBinder;)V";
	private final String onServiceDisconnected2ND = 
			"onServiceDisconnected2:(Landroid/content/ComponentName;)V";
	
	public boolean impServiceConnection(jq_Class mClass){
		return impInterface(mClass, serviceConnectionClass);
	}
	
	public jq_Method getOnServiceConnected2(jq_Class mClass){
		assert(impServiceConnection(mClass));
		return Program.g().getMethod(onServiceConnected2ND + "@" + mClass.toString());
	}
	
	public jq_Method getOnServiceDisconnected2(jq_Class mClass){
		assert(impServiceConnection(mClass));
		return Program.g().getMethod(onServiceDisconnected2ND + "@" + mClass.toString());
	}
	
	public jq_Method getOnServiceConnected(jq_Class mClass){
		assert(impServiceConnection(mClass));
		return Program.g().getMethod(onServiceConnectedND + "@" + mClass.toString());
	}
	
	public jq_Method getOnServiceDisconnected(jq_Class mClass){
		assert(impServiceConnection(mClass));
		return Program.g().getMethod(onServiceDisconnectedND + "@" + mClass.toString());
	}
	
	/*----------------------------------------------------------------------------------------
	 * ServiceConnection Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * ServiceConnection Runnable Begins
	/*---------------------------------------------------------------------------------------*/
	
	public boolean isOnServiceConnectedRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$onServiceConnectedRunnable"))
			return true;
		else
			return false;
	}
	
	public boolean isOnServiceDisconnectedRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$onServiceDisconnectedRunnable"))
			return true;
		else
			return false;
	}
	
	/*----------------------------------------------------------------------------------------
	 * ServiceConnection Runnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * BroadcastReceiver Begins
	/*---------------------------------------------------------------------------------------*/
	
	private final jq_Class broadcastReceiverClass = 
			(jq_Class) Program.g().getClass("android.content.BroadcastReceiver");
	
	private final String onReceiveND = 
			"onReceive:(Landroid/content/Context;Landroid/content/Intent;)V";
	
	private final String onReceive2ND = 
			"onReceive2:(Landroid/content/Context;Landroid/content/Intent;)V";
	
	public boolean isBroadcastReceiverExt(jq_Class mClass){
		return isExtof(mClass, broadcastReceiverClass);
	}
	
	public jq_Method getOnReceive2(jq_Class mClass){
		assert(isBroadcastReceiverExt(mClass));
		return Program.g().getMethod(onReceive2ND + "@" + mClass.toString());
	}
	
	public jq_Method getOnReceive(jq_Class mClass){
		assert(isBroadcastReceiverExt(mClass));
		return Program.g().getMethod(onReceiveND + "@" + mClass.toString());
	}
	
	/*----------------------------------------------------------------------------------------
	 * BroadcastReceiver Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * BroadcastReceiver Runnable Begins
	/*---------------------------------------------------------------------------------------*/
	
	public boolean isOnReceiveRunnable(jq_Class mClass){
		if(impRunnable(mClass) 
				&& mClass.getName().endsWith("$onReceiveRunnable"))
			return true;
		else
			return false;
	}
	
	/*----------------------------------------------------------------------------------------
	 * BroadcastReceiver Runnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * PostRunnable Begins
	/*---------------------------------------------------------------------------------------*/
	
	private final jq_Class postRunnableClass = 
			(jq_Class) Program.g().getClass("PostRunnable");
	
	private final String handlePostRunnableND = 
			"handlePostRunnable:(Ljava/lang/Runnable;)V";
	
	public jq_Method getHandlePostRunnable(){
		// When the apk does not post events to looper thread
		// postRunnableClass == null
		//assert(postRunnableClass != null);
		if(postRunnableClass != null)
			return Program.g().getMethod(handlePostRunnableND + "@" + postRunnableClass.toString());
		else
			return null;
	}
	
	/*----------------------------------------------------------------------------------------
	 * PostRunnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	/*----------------------------------------------------------------------------------------
	 * Runnable Begins
	/*---------------------------------------------------------------------------------------*/
	private final jq_Class runnableClass = (jq_Class) Program.g().getClass("java.lang.Runnable");
	private final String runMethodND = "run:()V";
	
	public boolean impRunnable(jq_Class mClass){
		return impInterface(mClass, runnableClass);
	}
	
	public jq_Method getRunMethod(jq_Class mClass){
		assert(impRunnable(mClass));
		return Program.g().getMethod(runMethodND + "@" + mClass);
	}
	
	/*----------------------------------------------------------------------------------------
	 * Runnable Ends
	/*---------------------------------------------------------------------------------------*/
	
	public boolean isExtof(jq_Class child, jq_Class parent){
		if(child == null || parent == null)
			return false;
		
		jq_Class mSuperClass = child.getSuperclass();
		while(!mSuperClass.getName().startsWith("java.")){
			if(mSuperClass.equals(parent)){
				return true;
			}
			mSuperClass = mSuperClass.getSuperclass();
		}
		return false;
	}
	
	public boolean impInterface(jq_Class implementation, jq_Class interfaceClass){
		return implementation.implementsInterface(interfaceClass);
	}
	
	public boolean isImpof(jq_Class implementation, jq_Class interfaceClass){
		if(impInterface(implementation, interfaceClass))
			return true;
		
		jq_Class mSuperClass = implementation.getSuperclass();
		while(!mSuperClass.getName().startsWith("java.")){
			if(impInterface(mSuperClass, interfaceClass)){
				return true;
			}
			mSuperClass = mSuperClass.getSuperclass();
		}
		return false;
	}

	public boolean isLibClass(jq_Class mClass) {
		String mClassName = mClass.getName();
		if(mClassName.startsWith("android.") ||
				mClassName.startsWith("com.google.api") ||
				mClassName.startsWith("java.") ||
				mClassName.startsWith("javax.") ||
				mClassName.startsWith("sun."))
			return true;
		else
			return false;
	}
}
