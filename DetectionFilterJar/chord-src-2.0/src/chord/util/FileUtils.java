/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

/**
 * File related utilities.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class FileUtils {

	/**
	 * Just disables an instance creation of this utility class.
	 *
	 * @throws UnsupportedOperationException always.
	 */
	private FileUtils() {
		throw new IllegalArgumentException();
	}

	public static String getAbsolutePath(String parent, String child) {
		return (new File(parent, child)).getAbsolutePath();
	}

	public static void copy(String fromFileName, String toFileName) {
		try {
			FileInputStream fis = new FileInputStream(fromFileName);
			FileOutputStream fos = new FileOutputStream(toFileName);
			byte[] buf = new byte[1024];
			int i = 0;
			while((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
			fis.close();
			fos.close();
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static boolean mkdirs(String dirName) {
		return mkdirs(new File(dirName));
	}
	public static boolean mkdirs(String parentName, String childName) {
		return mkdirs(new File(parentName, childName));
	}
	public static boolean mkdirs(File file) {
		if (file.exists()) {
			if (!file.isDirectory()) {
				throw new ChordRuntimeException(
					"File '" + file + "' is not a directory.");
			}
			return false;
		}
		if (file.mkdirs())
			return true;
		throw new ChordRuntimeException("Failed to create directory '" +
			file + "'");
	}
	public static PrintWriter newPrintWriter(String fileName) {
		try {
			return new PrintWriter(fileName);
		} catch (FileNotFoundException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static Object readSerialFile(String serialFileName) {
		try {
			FileInputStream fs = new FileInputStream(serialFileName);
			ObjectInputStream os = new ObjectInputStream(fs);
			Object o = os.readObject();
			os.close();
			return o;
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void writeSerialFile(Object o, String serialFileName) {
		try {
			FileOutputStream fs = new FileOutputStream(serialFileName);
			ObjectOutputStream os = new ObjectOutputStream(fs);
			os.writeObject(o);
			os.close();
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void readFileToList(String fileName, List<String> list) {
		readFileToList(new File(fileName), list);
	}
	public static void readFileToList(File file, List<String> list) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String s;
			while ((s = in.readLine()) != null) {
				list.add(s);
			}
			in.close();
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static List<String> readFileToList(String fileName) {
		return readFileToList(new File(fileName));
	}
	public static List<String> readFileToList(File file) {
		List<String> list = new ArrayList<String>();
		readFileToList(file, list);
		return list;
	}
	public static IndexMap<String> readFileToMap(String fileName) {
		return readFileToMap(new File(fileName));
	}
	public static IndexMap<String> readFileToMap(File file) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			IndexMap<String> map = new IndexMap<String>();
			String s;
			while ((s = in.readLine()) != null) {
				map.getOrAdd(s);
			}
			in.close();
			return map;
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void writeListToFile(List<String> list, String fileName) {
		writeListToFile(list, new File(fileName));
	}
	public static void writeListToFile(List<String> list, File file) {
		try {
			PrintWriter out = new PrintWriter(file);
			for (String s : list) {
				out.println(s);
			}
			out.close();
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void writeMapToFile(IndexMap<String> map, String fileName) {
		writeMapToFile(map, new File(fileName));
	}
	public static void writeMapToFile(IndexMap<String> map, File file) {
		try {
			PrintWriter out = new PrintWriter(file);
			for (String s : map) {
				out.println(s);
			}
			out.close();
		} catch (Exception ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public static void deleteFile(String fileName) {
		deleteFile(new File(fileName));
	}
	public static void deleteFile(File file) {
		if (file.exists())
			delete(file);
	}
	// file is assumed to exist
	private static void delete(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File file2 : files)
				delete(file2);
		}
		if (!file.delete())
			throw new ChordRuntimeException("Failed to delete file: " + file);
	}
	public static boolean exists(String fileName) {
		return (new File(fileName)).exists();
	}
}
