/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

/**
 * Array related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ArrayUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private ArrayUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determines whether a given array contains a given value.
	 *
	 * @return true iff the given array contains the given value.
	 * @param	<T>	The type of the array elements and the value
	 * to be checked for containment in the array.
	 * @param	array	An array.
	 * @param	s	A value to be checked for containment in the
	 * given array.
	 */
	public static <T> boolean contains(final T[] array, final T s) {
		if (array == null) {
			throw new IllegalArgumentException();
		}
		for (final T t : array) {
			if (t == null) {
				if (s == null) {
					return true;
				}
			} else if (t.equals(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a given array contains duplicate values.
	 *
	 * @return true iff the given array contains duplicate values.
	 * @throws IllegalArgumentException if {@code array} is {@code null}.
	 * @param	<T>	The type of the array elements.
	 * @param	array	An array.
	 */
	public static <T> boolean hasDuplicates(final T[] array) {
		if (array == null) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < array.length - 1; i++) {
			final T x = array[i];
			for (int j = i + 1; j < array.length; j++) {
				final T y = array[j];
				if (x.equals(y)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Provides a string representation of the elements in the given array.
	 *
	 * @param array		An array of elements.  It may be null.
	 * @param prefix	String to be used as prefix.
	 * @param sep		String to be used to separate array elements.
	 * @param suffix	String to be used as suffix.
	 * @param <T>	   The type of array elements.
	 * @return			String representation of the elements in the array.
	 */
	public static <T> String toString(T[] array, String prefix, String sep, String suffix) {
		if (array == null || array.length == 0) 
			return prefix + suffix;
		String result = prefix + array[0];
		for (int i = 1; i < array.length; i++) {
			result += sep + array[i];
		}
		return result + suffix;
	}

	/**
	 * Returns string representation of elements in given array.
	 *
	 * @param array an array of elements.
	 * @param <T>   The type of the array elements.
	 * @return string representation of elements in given array.
	 */
	public static <T> String toString(final T[] array) {
		return toString(array, "", ",", "");
	}

	public static String[] concat(String[] a, String[] b) {
		String[] c = new String[a.length + b.length];
		int i = 0;
		for (String s : a)
			c[i++] = s;
		for (String s : b)
			c[i++] = s;
		return c;
	}
}
