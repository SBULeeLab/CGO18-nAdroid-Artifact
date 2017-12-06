/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import chord.util.IndexMap;
import chord.util.ClassUtils;
import chord.util.FileUtils;
import chord.util.ChordRuntimeException;
import chord.util.ProcessExecutor;

/**
 * Common operations on files in the directory specified by system property
 * <tt>chord.out.dir</tt> to which Chord outputs all files.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class OutDirUtils {
	private static final String PROCESS_STARTING = "Starting command: '%s'";
	private static final String PROCESS_FINISHED = "Finished command: '%s'";
	private static final String PROCESS_FAILED = "Command '%s' terminated abnormally: %s";
	private static final String RESOURCE_NOT_FOUND = "Could not find resource '%s'.";

	public static PrintWriter newPrintWriter(String fileName) {
		try {
			return new PrintWriter(new File(Config.outDirName, fileName));
		} catch (FileNotFoundException ex) {
			throw new ChordRuntimeException(ex);
		}
	}

	public static String copyResourceByName(String srcFileName) {
		InputStream is = ClassUtils.getResourceAsStream(srcFileName);
		return copyResource(srcFileName, is, (new File(srcFileName)).getName());
	}

	public static String copyResourceByName(String srcFileName, InputStream is) {
		return copyResource(srcFileName, is, (new File(srcFileName)).getName());
	}

	public static String copyResourceByPath(String srcFileName) {
		InputStream is = ClassUtils.getResourceAsStream(srcFileName);
		return copyResource(srcFileName, is, srcFileName.replace('/', '_'));
	}

	public static String copyResourceByPath(String srcFileName, InputStream is) {
		return copyResource(srcFileName, is, srcFileName.replace('/', '_'));
	}

	public static String copyResource(String srcFileName, InputStream is, String dstFileName) {
		if (is == null)
			Messages.fatal(RESOURCE_NOT_FOUND, srcFileName);
		File dstFile = new File(Config.outDirName, dstFileName);
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			PrintWriter w = new PrintWriter(dstFile);
			String s;
			while ((s = r.readLine()) != null)
				w.println(s);
			r.close();
			w.close();
		} catch (IOException ex) {
			Messages.fatal(ex);
		}
		return dstFile.getAbsolutePath();
	}

	public static void writeMapToFile(IndexMap<String> map, String fileName) {
		FileUtils.writeMapToFile(map, new File(Config.outDirName, fileName));
	}

	public static void runSaxon(String xmlFileName, String xslFileName) {
		String dummyFileName = (new File(Config.outDirName, "dummy")).getAbsolutePath();
		xmlFileName = (new File(Config.outDirName, xmlFileName)).getAbsolutePath();
		xslFileName = (new File(Config.outDirName, xslFileName)).getAbsolutePath();
		try {
			net.sf.saxon.Transform.main(new String[] {
				"-o", dummyFileName, xmlFileName, xslFileName
			});
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static final void executeWithFailOnError(List<String> cmdlist) {
		String[] cmdarray = new String[cmdlist.size()];
		executeWithFailOnError(cmdlist.toArray(cmdarray));
	}

	public static final void executeWithFailOnError(String[] cmdarray) {
		String cmd = "";
		for (String s : cmdarray)
			cmd += s + " ";
		if (Config.verbose >= 1) Messages.log(PROCESS_STARTING, cmd);
		try {
			int result = ProcessExecutor.execute(cmdarray);
			if (result != 0)
				throw new ChordRuntimeException("Return value=" + result);
		} catch (Throwable ex) {
			Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
		}
		if (Config.verbose >= 1) Messages.log(PROCESS_FINISHED, cmd);
	}

	public static final void executeWithWarnOnError(List<String> cmdlist, int timeout) {
		String[] cmdarray = new String[cmdlist.size()];
		executeWithWarnOnError(cmdlist.toArray(cmdarray), timeout);
	}

	public static final void executeWithWarnOnError(String[] cmdarray, int timeout) {
		String cmd = "";
		for (String s : cmdarray)
			cmd += s + " ";
		Messages.log(PROCESS_STARTING, cmd);
		try {
			ProcessExecutor.execute(cmdarray, null, null, timeout);
		} catch (Throwable ex) {
			Messages.fatal(PROCESS_FAILED, cmd, ex.getMessage());
		}
		Messages.log(PROCESS_FINISHED, cmd);
	}
}
