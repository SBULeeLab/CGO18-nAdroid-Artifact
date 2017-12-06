/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;

/**
 * Array-based implementation of a set.
 * <p>
 * Provides O(n) membership testing as opposed to O(1) provided by a
 * hash set.
 * <p>
 * This implementation must be used for small sets for which its O(n)
 * membership testing penalty is less than the overhead of creating
 * and maintaining a hash set.
 * 
 * @param	<T>	The type of objects in the set.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ArraySet<T> extends ArrayList<T> implements Set<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 157868873529902483L;
	public ArraySet(Collection<T> s) {
		super(s);
	}
	public ArraySet() { }
	public ArraySet(int initialCapacity) {
		super(initialCapacity);
	}
	public boolean add(T e) {
		if (contains(e)) {
			return false;
		}
		return super.add(e);
	}
	/**
	 * Adds a given value to the set without checking if it already
	 * exists in the set.
	 * 
	 * @param	e	A value to be added to the set.
	 */
	public void addForcibly(T e) {
		super.add(e);
	}
	public boolean addAll(Collection<? extends T> c) {
		boolean modified = false;
		for (T e : c) {
			if (add(e)) {
				modified = true;
			}
		}
		return modified;
	}
	public boolean equals(Object o) {
		   if (o == this)
			return true;
		if (!(o instanceof Set)) {
			return false;
		}
		Collection c = (Collection) o;
		if (c.size() != size()) {
			return false;
		}
		for (Iterator it = c.iterator(); it.hasNext();) {
			Object e = it.next();
			if (!contains(e)) {
				return false;
			}
		}
		return true;
	}
	public int hashCode() {
		int h = 0;
		for (T e : this) {
			if (e != null)
				h += e.hashCode();
		}
		return h;
	}
}

