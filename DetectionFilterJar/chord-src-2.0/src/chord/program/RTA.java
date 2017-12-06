/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Array;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Field;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;
import joeq.Class.Classpath;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Main.HostedVM;
import joeq.Util.Templates.ListIterator;

import chord.project.Messages;
import chord.project.Config;
import chord.program.reflect.CastBasedStaticReflect;
import chord.program.reflect.DynamicReflectResolver;
import chord.program.reflect.StaticReflectResolver;
import chord.analyses.method.RelExtraEntryPoints;
import chord.util.IndexSet;
import chord.util.Timer;
import chord.util.ArraySet;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

/**
 * Rapid Type Analysis algorithm for computing program scope
 * (reachable classes and methods).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Omer Tripp (omertripp@post.tau.ac.il)
 */
public class RTA {
	private static final String MAIN_CLASS_NOT_DEFINED =
		"ERROR: Property chord.main.class must be set to specify the main class of program to be analyzed.";
	private static final String MAIN_METHOD_NOT_FOUND =
		"ERROR: Could not find main class '%s' or main method in that class.";
	private static final String METHOD_NOT_FOUND_IN_SUBTYPE =
		"WARN: Expected instance method %s in class %s implementing/extending interface/class %s.";

	public static final boolean DEBUG = false;

	private final String reflectKind; // [none|static|dynamic]

	/////////////////////////

	/*
	 * Data structures used only if reflectKind == dynamic
	 */

	private List<Pair<String, List<String>>> dynamicResolvedClsForNameSites;
	private List<Pair<String, List<String>>> dynamicResolvedObjNewInstSites;
	private List<Pair<String, List<String>>> dynamicResolvedConNewInstSites;
	private List<Pair<String, List<String>>> dynamicResolvedAryNewInstSites;

	/////////////////////////

	/*
	 * Data structures used only if reflectKind == static
	 */

	// enable intra-procedural analysis for inferring the class
	// loaded by calls to <code>Class.forName(s)</code> and the class of
	// objects allocated by calls to <code>v.newInstance()</code>.
	// The analysis achieves this by intra-procedurally tracking flow of
	// string constants to "s" and flow of class constants to "v".

	private StaticReflectResolver staticReflectResolver;

	// methods in which forName/newInstance sites have already been analyzed
	private Set<jq_Method> staticReflectResolved;

	/////////////////////////

	/*
	 * Data structures reset after every iteration.
	 */

	// Set of all classes whose clinits and super class/interface clinits
	// have been processed so far in current interation; this set is kept
	// to avoid repeatedly visiting super classes/interfaces within an
	// iteration (which incurs a huge runtime penalty) only to find that
	 // all their clinits have already been processed in that iteration.
	private Set<jq_Class> classesVisitedForClinit;

	// Set of all methods deemed reachable so far in current iteration.
	private IndexSet<jq_Method> methods;

	/////////////////////////

	/*
	 * Persistent data structures (not reset after iterations).
	 */

	private Reflect reflect;

	// set of all classes deemed reachable so far
	private IndexSet<jq_Reference> classes;

	// set of all (concrete) classes deemed instantiated so far either
	// by a reachable new/newarray statement or due to reflection
	private IndexSet<jq_Reference> reachableAllocClasses;

	// worklist for methods seen so far in current iteration but whose
	// CFGs haven't been processed yet
	private List<jq_Method> methodWorklist;

	// handle to the representation of class java.lang.Object
	private jq_Class javaLangObject;

	// flag indicating that another iteration is needed; it is set if
	// set reachableAllocClasses grows in the current iteration
	private boolean repeat = true;
	
	private String[] appCodePrefixes;

	public RTA(String reflectKind) {
		this.reflectKind = reflectKind;
		String fullscan = System.getProperty("chord.scope.fullscan");
		if(fullscan == null)
		  appCodePrefixes = new String[0];
		else
		  appCodePrefixes = Config.toArray(fullscan);
	}

	public IndexSet<jq_Method> getMethods() {
		if (methods == null)
			build();
		return methods;
	}

	public Reflect getReflect() {
		if (reflect == null)
			build();
		return reflect;
	}

	private static void print(List<Pair<String, List<String>>> l) {
		for (Pair<String, List<String>> p : l) {
			System.out.println(p.val0 + " -> ");
			for (String s : p.val1)
				System.out.println("\t" + s);
		}
	}

	private void build() {
		classes = new IndexSet<jq_Reference>();
		classesVisitedForClinit = new HashSet<jq_Class>();
		reachableAllocClasses = new IndexSet<jq_Reference>();
		methods = new IndexSet<jq_Method>();
		methodWorklist = new ArrayList<jq_Method>();
	
		if (Config.verbose >= 1) System.out.println("ENTER: RTA");
		Timer timer = new Timer();
		timer.init();
		if (reflectKind.equals("static")) {
			staticReflectResolver = new StaticReflectResolver();
			staticReflectResolved = new HashSet<jq_Method>();
		} else if(reflectKind.equals("static_cast")) {
			staticReflectResolved = new HashSet<jq_Method>();
			staticReflectResolver = new CastBasedStaticReflect(reachableAllocClasses, staticReflectResolved);
		} else if (reflectKind.equals("dynamic")) {
			DynamicReflectResolver dynamicReflectResolver =
				new DynamicReflectResolver();
			dynamicReflectResolver.run();
			dynamicResolvedClsForNameSites =
				dynamicReflectResolver.getResolvedClsForNameSites();
			dynamicResolvedObjNewInstSites =
				dynamicReflectResolver.getResolvedObjNewInstSites();
			dynamicResolvedConNewInstSites =
				dynamicReflectResolver.getResolvedConNewInstSites();
			dynamicResolvedAryNewInstSites =
				dynamicReflectResolver.getResolvedAryNewInstSites();
		}
		 
		reflect = new Reflect();
		HostedVM.initialize();
		javaLangObject = PrimordialClassLoader.getJavaLangObject();
		String mainClassName = Config.mainClassName;
		if (mainClassName == null)
			Messages.fatal(MAIN_CLASS_NOT_DEFINED);
		   jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
		prepareClass(mainClass);
		jq_Method mainMethod = (jq_Method) mainClass.getDeclaredMember(
			new jq_NameAndDesc("main", "([Ljava/lang/String;)V"));
		if (mainMethod == null)
			Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
		
		prepAdditionalEntrypoints();
		
		for (int i = 0; repeat; i++) {
			if (Config.verbose >= 1) System.out.println("Iteration: " + i);
			repeat = false;
			classesVisitedForClinit.clear();
			methods.clear();
			visitClinits(mainClass);
			visitMethod(mainMethod);

			visitAdditionalEntrypoints();
			while (!methodWorklist.isEmpty()) {
				int n = methodWorklist.size();
				jq_Method m = methodWorklist.remove(n - 1);
				if (DEBUG) System.out.println("Processing CFG of " + m);
				processMethod(m);
			}
			if (staticReflectResolver != null) {
				staticReflectResolver.startedNewIter();
			}
		}
		if (Config.verbose >= 1) System.out.println("LEAVE: RTA");
		timer.done();
		if (Config.verbose >= 1)
			System.out.println("Time: " + timer.getInclusiveTimeStr());
		staticReflectResolver = null; // no longer in use; stop referencing it
	}
	
  
  Iterable<jq_Method> publicMethods = new ArrayList<jq_Method>();

  private void prepAdditionalEntrypoints() {
		 publicMethods = RelExtraEntryPoints.slurpMList(Program.g().getClassHierarchy());
  }

  private void visitAdditionalEntrypoints() {
	  //visit classes just once each
	LinkedHashSet<jq_Class> extraClasses = new LinkedHashSet<jq_Class>();
	for(jq_Method m: publicMethods) {
	  extraClasses.add(m.getDeclaringClass());
	}
	
	for(jq_Class cl: extraClasses) {
	  visitClass(cl);
			jq_Method ctor = cl.getInitializer(new jq_NameAndDesc("<init>", "()V"));
			if (ctor != null)
				visitMethod(ctor);
	}

	for(jq_Method m: publicMethods) {
	  visitMethod(m);
	}
  }


	private void visitMethod(jq_Method m) {
		if (methods.add(m)) {
			if (DEBUG) System.out.println("\tAdding method: " + m);
			if (!m.isAbstract()) {
				methodWorklist.add(m);
			}
		}
	}

	private void processResolvedClsForNameSite(Quad q, jq_Reference r) {
		reflect.addResolvedClsForNameSite(q, r);
		visitClass(r);
	}

	private void processResolvedObjNewInstSite(Quad q, jq_Reference r) {
		reflect.addResolvedObjNewInstSite(q, r);
		visitClass(r);
		if (reachableAllocClasses.add(r) ||
				(staticReflectResolver != null && staticReflectResolver.needNewIter()))
			repeat = true;
		if (r instanceof jq_Class) {
			jq_Class c = (jq_Class) r;
			jq_Method n = c.getInitializer(new jq_NameAndDesc("<init>", "()V"));
			if (n != null)
				visitMethod(n);
		}
	}

	private void processResolvedAryNewInstSite(Quad q, jq_Reference r) {
		reflect.addResolvedAryNewInstSite(q, r);
		visitClass(r);
		if (reachableAllocClasses.add(r))
			repeat = true;
	}

	private void processResolvedConNewInstSite(Quad q, jq_Reference r) {
		reflect.addResolvedConNewInstSite(q, r);
		visitClass(r);
		if (reachableAllocClasses.add(r))
			repeat = true;
		jq_Class c = (jq_Class) r;
		jq_InstanceMethod[] meths = c.getDeclaredInstanceMethods();
		// this is imprecise in that we are visiting all constrs instead of the called one
		// this is also unsound because we are not visiting constrs in superclasses
		for (int i = 0; i < meths.length; i++) {
			jq_InstanceMethod m = meths[i];
			if (m.getName().toString().equals("<init>"))
				visitMethod(m);
		}
	}

	private void processMethod(jq_Method m) {
		if (staticReflectResolved != null && staticReflectResolved.add(m)) {
			staticReflectResolver.run(m);
			Set<Pair<Quad, jq_Reference>> resolvedClsForNameSites =
				staticReflectResolver.getResolvedClsForNameSites();
			Set<Pair<Quad, jq_Reference>> resolvedObjNewInstSites =
				staticReflectResolver.getResolvedObjNewInstSites();
			for (Pair<Quad, jq_Reference> p : resolvedClsForNameSites)
				processResolvedClsForNameSite(p.val0, p.val1);
			for (Pair<Quad, jq_Reference> p : resolvedObjNewInstSites)
				processResolvedObjNewInstSite(p.val0, p.val1);
		}
		ControlFlowGraph cfg = m.getCFG();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				if (DEBUG) System.out.println("Quad: " + q);
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					if (op instanceof InvokeVirtual || op instanceof InvokeInterface)
						processVirtualInvk(m, q);
					else
						processStaticInvk(m, q);
				} else if (op instanceof Getstatic) {
					jq_Field f = Getstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof Putstatic) {
					jq_Field f = Putstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof New) {
					jq_Class c = (jq_Class) New.getType(q).getType();
					visitClass(c);
					if (reachableAllocClasses.add(c))
						repeat = true;
				} else if (op instanceof NewArray) {
					jq_Array a = (jq_Array) NewArray.getType(q).getType();
					visitClass(a);
					if (reachableAllocClasses.add(a))
						repeat = true;
				} else if (op instanceof Move) {
					Operand ro = Move.getSrc(q);
					if (ro instanceof AConstOperand) {
						Object c = ((AConstOperand) ro).getValue();
						if (c instanceof Class) {
							String s = ((Class) c).getName();
							// s is in encoded form only if it is an array type
							// if (s.startsWith("[")) s = Program.typesToStr(s);
							jq_Reference d = (jq_Reference) Program.parseType(s);
							if (d != null)
								visitClass(d);
						}
					}
				}
			}
		}
	}

	// does qStr (in format bci!mName:mDesc@cName) correspond to quad q in method m?
	private static boolean matches(String qStr, jq_Method m, Quad q) {
		MethodElem me = MethodElem.parse(qStr);
		return me.mName.equals(m.getName().toString()) &&
			me.mDesc.equals(m.getDesc().toString()) &&
			me.cName.equals(m.getDeclaringClass().getName()) &&
			q.getBCI() == me.offset;
	}

	private void processVirtualInvk(jq_Method m, Quad q) {
		jq_Method n = Invoke.getMethod(q).getMethod();
		jq_Class c = n.getDeclaringClass();
		visitClass(c);
		visitMethod(n);
		String cName = c.getName();
		if (cName.equals("java.lang.Class")) {
			if (dynamicResolvedObjNewInstSites != null &&
					n.getName().toString().equals("newInstance") &&
					n.getDesc().toString().equals("()Ljava/lang/Object;")) {
				for (Pair<String, List<String>> p : dynamicResolvedObjNewInstSites) {
					if (matches(p.val0, m, q)) {
						for (String s : p.val1) {
							jq_Reference r = (jq_Reference) Program.parseType(s);
							if (r != null)
								processResolvedObjNewInstSite(q, r);
						}
						break;
					}
				}
			}
		} else if (cName.equals("java.lang.reflect.Constructor")) {
			if (dynamicResolvedConNewInstSites != null &&
					n.getName().toString().equals("newInstance") &&
					n.getDesc().toString().equals("([Ljava/lang/Object;)Ljava/lang/Object;")) {
				for (Pair<String, List<String>> p : dynamicResolvedConNewInstSites) {
					if (matches(p.val0, m, q)) {
						for (String s : p.val1) {
							jq_Reference r = (jq_Reference) Program.parseType(s);
							if (r != null)
								processResolvedConNewInstSite(q, r);
						}
						break;
					}
				}
			}
		}
		jq_NameAndDesc nd = n.getNameAndDesc();
		boolean isInterface = c.isInterface();
		for (jq_Reference r : reachableAllocClasses) {
			if (r instanceof jq_Array)
				continue;
			jq_Class d = (jq_Class) r;
			assert (!d.isInterface());
			assert (!d.isAbstract());
			boolean matches = isInterface ? d.implementsInterface(c) : d.extendsClass(c);
			if (matches) {
				jq_InstanceMethod m2 = d.getVirtualMethod(nd);
				if (m2 == null) {
					Messages.log(METHOD_NOT_FOUND_IN_SUBTYPE,
						nd.toString(), d.getName(), c.getName());
				} else {
					visitMethod(m2);
				}
			}
		}
	}

	private void processStaticInvk(jq_Method m, Quad q) {
		jq_Method n = Invoke.getMethod(q).getMethod();
		jq_Class c = n.getDeclaringClass();
		visitClass(c);
		visitMethod(n);
		String cName = c.getName();
		if (cName.equals("java.lang.Class")) {
			if (dynamicResolvedClsForNameSites != null &&
					n.getName().toString().equals("forName") &&
					n.getDesc().toString().equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
				for (Pair<String, List<String>> p : dynamicResolvedClsForNameSites) {
					if (matches(p.val0, m, q)) {
						for (String s : p.val1) {
							jq_Reference r = (jq_Reference) Program.parseType(s);
							if (r != null)
								processResolvedClsForNameSite(q, r);
						}
						break;
					}
				}
			}
		} else if (cName.equals("java.lang.reflect.Array")) {
			if (dynamicResolvedAryNewInstSites != null &&
					n.getName().toString().equals("newInstance") &&
					n.getDesc().toString().equals("(Ljava/lang/Class;I)Ljava/lang/Object;")) {
				for (Pair<String, List<String>> p : dynamicResolvedAryNewInstSites) {
					if (matches(p.val0, m, q)) {
						for (String s : p.val1) {
							jq_Reference r = (jq_Reference) Program.parseType(s);
							if (r != null)
								processResolvedAryNewInstSite(q, r);
						}
						break;
					}
				}
			}
		}
	}

	private void prepareClass(jq_Reference r) {
		if (classes.add(r)) {
			r.prepare();
			if (DEBUG) System.out.println("\tAdding class: " + r);
			if (r instanceof jq_Array)
				return;
			jq_Class c = (jq_Class) r;
			jq_Class d = c.getSuperclass();
			if (d == null)
				assert (c == javaLangObject);
			else
				prepareClass(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				prepareClass(i);
		}
	}
	
	private boolean shouldExpandAggressively(jq_Class c) {
	  return Utils.prefixMatch(c.getName(), appCodePrefixes);
	}

	private void visitClass(jq_Reference r) {
		prepareClass(r);
		if (r instanceof jq_Array)
			return;
		jq_Class c = (jq_Class) r;
		visitClinits(c);
		
		if(shouldExpandAggressively(c)) {
		  for(jq_Method m: c.getDeclaredInstanceMethods()) 
			visitMethod(m);
		  
		  for(jq_Method m: c.getDeclaredStaticMethods())
			visitMethod(m); 
		}
	}

	private void visitClinits(jq_Class c) {
		if (classesVisitedForClinit.add(c)) {
			jq_ClassInitializer m = c.getClassInitializer();
			// m is null for classes without class initializer method
			if (m != null)
				visitMethod(m);
			jq_Class d = c.getSuperclass();
			if (d != null)
				visitClinits(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				visitClinits(i);
		}
	}
}
