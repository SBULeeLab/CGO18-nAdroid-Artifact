/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.instr;

import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.FilenameFilter;

import chord.project.Messages;

import chord.util.Constants;
import javassist.NotFoundException;
import javassist.ClassPool;
import javassist.CtClass;

/**
 * Class pool specifying program classpath for Javassist bytecode instrumentor.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class JavassistPool {
	private static final String IGNORE_PATH_ELEMENT =
		"WARN: Instrumentor: Ignoring path element %s from %s";
	private final Set<String> bootClassPathResourceNames;
	private final Set<String> userClassPathResourceNames;
	private final ClassPool pool;
	public JavassistPool() {
		pool = new ClassPool();

		bootClassPathResourceNames = new HashSet<String>();
		String bootClassPathName = System.getProperty("sun.boot.class.path");
		String[] bootClassPathElems = bootClassPathName.split(Constants.PATH_SEPARATOR);
		for (String pathElem : bootClassPathElems) {
			bootClassPathResourceNames.add(pathElem);
			try {
				pool.appendClassPath(pathElem);
			} catch (NotFoundException ex) {
				Messages.log(IGNORE_PATH_ELEMENT, pathElem, "boot classpath");
			}
		}

		userClassPathResourceNames = new HashSet<String>();
		String javaHomeDir = System.getProperty("java.home");
		assert (javaHomeDir != null);
		File libExtDir = new File(javaHomeDir,
			File.separator + "lib" + File.separator + "ext");
		if (libExtDir.exists()) {
			final FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith(".jar"))
						return true;
					return false;
				}
			};
			File[] subFiles = libExtDir.listFiles(filter);
			for (File file : subFiles) {
				String fileName = file.getAbsolutePath();
				userClassPathResourceNames.add(fileName);
				try {
					pool.appendClassPath(fileName);
				} catch (NotFoundException ex) {
					Messages.log(IGNORE_PATH_ELEMENT, fileName,
						libExtDir.getAbsolutePath());
				}
			}
		}

		String userClassPathName = System.getProperty("java.class.path");
		String[] userClassPathElems = userClassPathName.split(Constants.PATH_SEPARATOR);
		for (String pathElem : userClassPathElems) {
			userClassPathResourceNames.add(pathElem);
			try {
				   pool.appendClassPath(pathElem);
			} catch (NotFoundException ex) {
				Messages.log(IGNORE_PATH_ELEMENT, pathElem, "user classpath");
			}
		}
	}

	// never returns null
	public CtClass get(String cName) throws NotFoundException {
		return pool.get(cName);
	}

	public String getResource(String cName) {
		return pool.getResource(cName);
	}

	public boolean isBootResource(String rName) {
		return bootClassPathResourceNames.contains(rName);
	}

	public boolean isUserResource(String rName) {
		return userClassPathResourceNames.contains(rName);
	}

	public ClassPool getPool() {
		return pool;
	}

}
