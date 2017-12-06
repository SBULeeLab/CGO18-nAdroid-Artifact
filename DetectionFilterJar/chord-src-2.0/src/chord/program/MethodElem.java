/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

/**
 * Representation of the unique location of any bytecode in any method
 * declared in any class in the program.
 * 
 * Its format is <tt>offset!mName:mDesc@cName</tt> where:
 * <ul>
 * <li><tt>offset</tt> denotes the offset of the bytecode in its
 * containing method,</li>
 * <li><tt>mName</tt> denotes the name of the containing method,</li>
 * <li><tt>mDesc</tt> denotes the descriptor of the containing
 * method, and</li>
 * <li><tt>cName</tt> denotes the name of the class declaring the
 * method.</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class MethodElem extends MethodSign {
	public final int offset;
	/**
	 * Creates the representation of the specified bytecode location.
	 * 
	 * @param	offset	The offset of the bytecode in its containing
	 * method.
	 * @param	mName	The name of the containing method.
	 * @param	mDesc	The descriptor of the containing method.
	 * @param	cName	The name of the class declaring the method.
	 */
	public MethodElem(int offset, String mName, String mDesc, String cName) {
		super(mName, mDesc, cName);
		this.offset = offset;
	}
	/**
	 * Creates the representation of the specified bytecode location.
	 * 
	 * @param	s	A string of the form <tt>offset!mName:mDesc@cName</tt>
	 * specifying a unique bytecode location.
	 * @return	The representation of the specified bytecode location.
	 */
	public static MethodElem parse(String s) {
		int exclIdx = s.indexOf('!');
		int colonIdx  = s.indexOf(':');
		int atIdx = s.indexOf('@');
		int num = Integer.parseInt(s.substring(0, exclIdx));
		String mName = s.substring(exclIdx + 1, colonIdx);
		String mDesc = s.substring(colonIdx + 1, atIdx);
		String cName = s.substring(atIdx + 1);
		return new MethodElem(num, mName, mDesc, cName);
	}
	public String toString() {
		return offset + "!" + mName + ":" + mDesc + "@" + cName;
	}
}
