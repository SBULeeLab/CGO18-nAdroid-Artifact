/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

/**
 * A basic runtime exception for Chord.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ChordRuntimeException extends RuntimeException {

	/**
	 * Creates a new instance.
	 */
	public ChordRuntimeException() {
		super();
	}

	/**
	 * Creates a new instance with given message.
	 *
	 * @param message an exception message.
	 */
	public ChordRuntimeException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance with given cause.
	 *
	 * @param cause an exception cause.
	 */
	public ChordRuntimeException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new instance with given message and cause.
	 *
	 * @param message an exception message.
	 * @param cause   an exception cause.
	 */
	public ChordRuntimeException(final String message, final Throwable cause) {
		super(message, cause);
	}

}

