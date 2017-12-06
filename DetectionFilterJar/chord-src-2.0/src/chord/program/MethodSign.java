/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

/**
 * Representation of the signature of a method.
 * 
 * Its format is <tt>mName:mDesc@cName</tt> where:
 * <ul>
 * <li><tt>mName</tt> denotes the name of the method,</li>
 * <li><tt>mDesc</tt> denotes the descriptor of the method, and</li>
 * <li><tt>cName</tt> denotes the name of the class declaring the
 * method.</li>
 * </ul>

 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class MethodSign {
	public final String mName;
	public final String mDesc;
	public final String cName;
	/**
	 * Creates the representation of the signature of the specified
	 * method.
	 * 
	 * @param	mName	The name of the method.
	 * @param	mDesc	The descriptor of the method.
	 * @param	cName	The name of the class declaring the method.
	 */
	public MethodSign(String mName, String mDesc, String cName) {
		this.mName = mName;
		this.mDesc = mDesc;
		this.cName = cName;
	}
	/**
	 * Creates the representation of the signature of the specified
	 * method.
	 * 
	 * @param	s	A string of the form <tt>mName:mDesc@cName</tt>
	 * specifying a method.
	 * @return	The representation of the signature of the specified
	 * method.
	 */
	public static MethodSign parse(String s) {
		int colonIdx = s.indexOf(':');
		int atIdx = s.indexOf('@');
		String mName = s.substring(0, colonIdx);
		String mDesc = s.substring(colonIdx + 1, atIdx);
		String cName = s.substring(atIdx + 1);
		return new MethodSign(mName, mDesc, cName);
	}
}
