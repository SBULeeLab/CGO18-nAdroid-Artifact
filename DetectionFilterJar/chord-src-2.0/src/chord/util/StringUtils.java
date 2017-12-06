/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

/**
 * String related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class StringUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private StringUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Trim the numerical suffix of the given string.  For instance,
	 * converts "abc123xyz456" to "abc123xyz". If given string is
	 * empty, also returns an empty string.
	 *
	 * @return A copy of the given string without any numerical suffix.
	 * @throws IllegalArgumentException if {@code s} is {@code null}.
	 * @param	s	The string whose numerical suffix is to be trimmed.
	 */
	public static String trimNumSuffix(final String s) {
		if (s == null) {
			throw new IllegalArgumentException();
		}
		if (s.length() == 0) {
			return s;
		}
		int i = s.length() - 1;
		while (Character.isDigit(s.charAt(i))) {
			i--;
		}
		return s.substring(0, i + 1);
	}

	/**
	 * Create an array of strings by concatenating two given arrays of strings.
	 *
	 * @return A new array of strings containing those in {@code a} followed by those in {@code b}.
	 * @throws IllegalArgumentException if any of arguments is {@code null}.
	 * @param	a	the first array of strings.
	 * @param	b	the second array of strings.
	 */
	public static String[] concat(final String[] a, final String[] b) {
		if (a == null) {
			throw new IllegalArgumentException();
		}
		if (b == null) {
			throw new IllegalArgumentException();
		}
		final String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public static List<String> tokenize(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<String> l = new ArrayList<String>(st.countTokens());
		for (int i = 0; st.hasMoreTokens(); i++)
			l.add(st.nextToken());
		return l;
	}
}
