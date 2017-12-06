/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

/**
 * Object comparison utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class CompareUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private CompareUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns {@code true} iff given objects are same by meaning of reference
	 * or by meaning of {@code equals()} method. The first condition applies that
	 * this method returns {@code true} also when the both objects are {@code null}.
	 *
	 * @param x the first compared object.
	 * @param y the second compared object.
	 * @return {@code true} iff given objects are same by meaning of reference or by meaning of {@code equals()} method.
	 */
	public static boolean areEqual(final Object x, final Object y) {
		return x == null ? y == null : x.equals(y);
	}

}
