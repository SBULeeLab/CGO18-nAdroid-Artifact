/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import java.util.List;
import java.util.Collections;

import chord.util.IndexSet;
 
import joeq.Main.HostedVM;
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.program.reflect.DynamicReflectResolver;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DynamicBuilder {
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Method> getMethods() {
		if (methods != null)
			return methods;
		List<String> classNames = Program.getDynamicallyLoadedClasses();
		HostedVM.initialize();
		methods = new IndexSet<jq_Method>();
		for (String s : classNames) {
			jq_Class c = (jq_Class) Program.loadClass(s);
			if (c == null)
				continue;
			for (jq_Method m : c.getDeclaredStaticMethods()) {
				if (!m.isAbstract())
					m.getCFG();
				methods.add(m);
			}
			for (jq_Method m : c.getDeclaredInstanceMethods()) {
				if (!m.isAbstract())
					m.getCFG();
				methods.add(m);
			}
		}
		return methods;
	}
}
