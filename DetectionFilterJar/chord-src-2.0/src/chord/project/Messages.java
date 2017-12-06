/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

/**
 * Utility for logging messages during Chord's execution.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Messages {
	private Messages() { }
	public static void log(String format, Object... args) {
		String msg = String.format(format, args);
		System.out.println(msg);
	}
	public static void fatal(String format, Object... args) {
		log(format, args);
		System.exit(1);
	}
	public static void fatal(Throwable ex) {
		ex.printStackTrace();
		System.exit(1);
	}
}

