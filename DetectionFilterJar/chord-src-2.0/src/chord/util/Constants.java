/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.File;

/**
 * Commonly-used constants.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */

public class Constants {
	private Constants() { }

    public final static String LIST_SEPARATOR = " |,|:|;";
    public final static String PATH_SEPARATOR = File.pathSeparator + "|;";
}
