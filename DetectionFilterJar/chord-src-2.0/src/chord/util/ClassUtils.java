/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.scannotation.AnnotationDB;


/**
 * Class related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ClassUtils {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private ClassUtils() { }

	/**
	 * Determines whether a given class is a subclass of another.
	 *
	 * @param	subclass	An intended subclass.
	 * @param	superclass	An intended superclass.
	 * @return	{@code true} iff class {@code subclass} is a subclass of class <tt>superclass</tt>.
	 */
	public static boolean isSubclass(final Class subclass, final Class superclass) {
		try {
			subclass.asSubclass(superclass);
		} catch (final ClassCastException ex) {
			return false;
		}
		return true;
	}

	public static InputStream getResourceAsStream(String resName) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		return cl.getResourceAsStream(resName);
	}

	public static BufferedReader getResourceAsReader(String resName) {
		InputStream is = getResourceAsStream(resName);
		return (is == null) ? null : new BufferedReader(new InputStreamReader(is));
	}

    public static Set<String> getClassNames(final String classPath) {
        if (classPath == null) {
            throw new IllegalArgumentException();
        }
        final List<URL> list = new ArrayList<URL>();
        for (final String fileName : classPath.split(Constants.PATH_SEPARATOR)) {
            final File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("WARNING: Ignoring: " + fileName);
                continue;
            }
            try {
                list.add(file.toURL());
            } catch (final MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
        final AnnotationDB db = new AnnotationDB();
        db.setIgnoredPackages(EMPTY_STRING_ARRAY);
        try {
            db.scanArchives(list.toArray(new URL[list.size()]));
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
        return db.getClassIndex().keySet();
    }
}
