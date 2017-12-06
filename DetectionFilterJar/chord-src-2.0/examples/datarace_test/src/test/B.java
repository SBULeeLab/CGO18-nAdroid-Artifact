/*
 * Copyright (c) 2006-07, The Trustees of Stanford University.  All
 * rights reserved.
 * Licensed under the terms of the GNU GPL; see COPYING for details.
 */
package test;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class B {
	private A bf;
	public B() {
		A a = new A();
		this.bf = a;
	}
	public int get() {
		A a = this.bf;
		return a.get();
	}
	public void set(int i) {
		A a = this.bf;
		a.set(i);
	}
}
