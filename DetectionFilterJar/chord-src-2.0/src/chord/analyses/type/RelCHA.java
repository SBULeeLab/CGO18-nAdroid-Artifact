/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.type;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Array;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Initializer;
import joeq.Class.jq_NameAndDesc;

import java.util.Set;
import java.util.HashSet;

import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;
import chord.util.tuple.object.Trio;
import chord.project.Messages;

/**
 * Relation containing each tuple (m1,t,m2) such that method m2 is the
 * resolved method of an invokevirtual or invokeinterface call with
 * resolved method m1 on an object of concrete class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "cha",
	sign = "M1,T1,M0:M0xM1_T1"
)
public class RelCHA extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		Program program = Program.g();
		Set<jq_InstanceMethod> objClsInstanceMethods = new HashSet<jq_InstanceMethod>();
		IndexSet<jq_Reference> classes = program.getClasses();
		jq_Class objCls = (jq_Class) program.getClass("java.lang.Object");
		for (jq_InstanceMethod m : objCls.getDeclaredInstanceMethods()) {
			// only add methods deemed reachable
			if (domM.contains(m))
				objClsInstanceMethods.add(m);
		}
		for (jq_Reference r : classes) {
			if (r instanceof jq_Array) {
				for (jq_InstanceMethod m : objClsInstanceMethods)
					add(m, r, m);
				continue;
			}
			jq_Class c = (jq_Class) r;
			for (jq_InstanceMethod m : c.getDeclaredInstanceMethods()) {
				if (m.isPrivate())
					continue;
				if (m instanceof jq_Initializer)
					continue;
				if (!domM.contains(m))
					continue;
				jq_NameAndDesc nd = m.getNameAndDesc();
				if (c.isInterface()) {
					for (jq_Reference s : classes) {
						if (s instanceof jq_Array)
							continue;
						jq_Class d = (jq_Class) s;
						if (d.isInterface() || d.isAbstract())
							continue;
						if (d.implementsInterface(c)) {
							jq_InstanceMethod n = d.getVirtualMethod(nd);
							assert (n != null);
							if (domM.contains(n))
								add(m, d, n);
						}
					}
				} else {
					for (jq_Reference s : classes) {
						if (s instanceof jq_Array) 
							continue;
						jq_Class d = (jq_Class) s;
						if (d.isInterface() || d.isAbstract())
							continue;
						if (d.extendsClass(c)) {
							jq_InstanceMethod n = d.getVirtualMethod(nd);
							assert (n != null);
							if (domM.contains(n))
								add(m, d, n);
						}
					}
				}
			}
		}
	}
}
