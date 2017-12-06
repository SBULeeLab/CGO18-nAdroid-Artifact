/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import chord.bddbddb.RelSign;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ClassUtils;

/**
 * Parser for Chord annotations on classes defining program analyses.
 *
 * The annotation specifies aspects of the analysis such as its name,
 * its consumed and produced targets, etc.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ChordAnnotParser {
	private static final String ERROR = "ERROR: @Chord annotation of class '%s': %s";
	private final Class type;
	private String name;
	private String prescriber;
	private List<String> consumes;
	private List<String> produces;
	private List<String> controls;
	private Map<String, RelSign> nameToSignMap;
	private Map<String, Class> nameToTypeMap;
	private boolean hasNoErrors;
	/**
	 * Constructor.
	 * 
	 * @param	type	A class annotated with a Chord annotation.
	 */
	public ChordAnnotParser(Class type) {
		this.type = type;
	}
	/**
	 * Parses this Chord annotation.
	 * 
	 * @return	true iff the Chord annotation parses successfully.
	 */
	public boolean parse() {
		Chord chord = (Chord) type.getAnnotation(Chord.class);
		assert (chord != null);
		hasNoErrors = true;
		name = chord.name();

		prescriber = chord.prescriber();
		if (prescriber.equals(""))
			prescriber = name;

		String sign = chord.sign();
		RelSign relSign = null;
		if (ClassUtils.isSubclass(type, ProgramRel.class)) {
			if (sign.equals("")) {
				error("Method sign() cannot return empty string " +
					"for Java analysis '" + type + "'");
			} else {
				relSign = parseRelSign(sign);
			}
		} else if (!sign.equals("")) {
			error("Method sign() cannot return non-empty string " +
				 "for Java analysis '" + type + "'");
		}

		{
			String[] a = chord.consumes();
			consumes = new ArrayList<String>(a.length);
			// NOTE: domains MUST be added BEFORE any declared consumed
			// targets to 'consumes' if this annotation is on a subclass
			// of ProgramRel; ModernProject relies on this invariant.
			if (relSign != null) {
				for (String domName : relSign.getDomKinds())
					consumes.add(domName);
			}
			for (String s : a)
				consumes.add(s);
		}

		{
			String[] a = chord.produces();
			produces = new ArrayList<String>(a.length);
			for (String s : a)
				produces.add(s);
		}

		{
			String[] a = chord.controls();
			controls = new ArrayList<String>(a.length);
			for (String s : a)
				controls.add(s);
		}

		// program rels and doms should not declare any produces/controls
		if (ClassUtils.isSubclass(type, ProgramRel.class) ||
			ClassUtils.isSubclass(type, ProgramDom.class)) {
			if (produces.size() > 0) {
				error("Method produces() cannot return non-empty string " +
					" for Java analysis '" + type + "'");
				produces.clear();
			}
			produces.add(name);
			if (controls.size() > 0) {
				error("Method controls() cannot return non-empty string " +
					" for Java analysis '" + type + "'");
				controls.clear();
			}
		}

		nameToTypeMap = new HashMap<String, Class>();
		nameToTypeMap.put(name, type);
		String[] namesOfTypes = chord.namesOfTypes();
		Class [] types = chord.types();
		if (namesOfTypes.length != types.length) {
			error("Methods namesOfTypes() and types() " +
				"return arrays of different lengths.");
		} else {
			for (int i = 0; i < namesOfTypes.length; i++) {
				String name2 = namesOfTypes[i];
				if (name2.equals(name) || name2.equals(".")) {
					error("Method namesOfTypes() cannot return the same " +
						"name as that returned by name()");
					continue;
				}
				if (nameToTypeMap.containsKey(name2)) {
					error("Method namesOfTypes() cannot return a name ('" +
						name2 + "') multiple times.");
					continue;
				}
				nameToTypeMap.put(name2, types[i]);
			}
		}

		nameToSignMap = new HashMap<String, RelSign>();
		if (relSign != null)
			nameToSignMap.put(this.name, relSign);
		String[] namesOfSigns = chord.namesOfSigns();
		String[] signs = chord.signs();
		if (namesOfSigns.length != signs.length) {
			error("Methods namesOfSigns() and signs() " +
				"return arrays of different lengths.");
		} else {
			for (int i = 0; i < namesOfSigns.length; i++) {
				String name2 = namesOfSigns[i];
				if (name2.equals(name) || name2.equals(".")) {
					error("Method namesOfSigns() cannot return the same " +
						"name as that returned by name(); use sign().");
					continue;
				}
				if (nameToSignMap.containsKey(name2)) {
					error("Method namesOfSigns() cannot return a name ('" +
						name2 + "') multiple times.");
					continue;
				}
				Class type2 = nameToTypeMap.get(name2);
				if (type2 != null) {
					if (!ClassUtils.isSubclass(type2, ProgramRel.class)) {
						error("Method namesOfSigns() implicitly declares " +
							"name '" + name2 + "' as having type '" +
							ProgramRel.class.getName() + "' whereas method " +
							"namesOfTypes() declares it as having " +
							"incompatible type '" + type2.getName() + "'."); 
						continue;
					}
				}
				RelSign relSign2 = parseRelSign(signs[i]);
				if (relSign2 != null)
					nameToSignMap.put(name2, relSign2);
			}
		}

		return hasNoErrors;
	}
	private RelSign parseRelSign(String sign) {
		int i = sign.indexOf(':');
		String domOrder;
		if (i != -1) {
			domOrder = sign.substring(i + 1);
			sign = sign.substring(0, i);
		} else
			domOrder = null;
		 String[] domNamesAry = sign.split(",");
		if (domNamesAry.length == 1)
			domOrder = domNamesAry[0];
		try {
			return new RelSign(domNamesAry, domOrder);
		} catch (RuntimeException ex) {
			error(ex.getMessage());
			return null;
		}
	}
	private void error(String msg) {
		Messages.log(ERROR, type.getName(), msg);
		hasNoErrors = false;
	}
	/**
	 * Provides the name specified by this Chord annotation of the
	 * associated analysis.
	 * 
	 * @return	The name specified by this Chord annotation of the
	 * associated analysis.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Provides the name of the control target specified by this
	 * Chord annotation as prescribing the associated analysis.
	 * 
	 * @return	The name of the control target specified by this
	 * Chord annotation as prescribing the associated analysis.
	 */
	public String getPrescriber() {
		return prescriber;
	}
	/**
	 * Provides the names of data targets specified by this Chord
	 * annotation as consumed by the associated analysis.
	 * 
	 * @return	The names of data targets specified by this Chord
	 * annotation as consumed by the associated analysis.
	 */
	public List<String> getConsumes() {
		return consumes;
	}
	/**
	 * Provides the names of data targets specified by this Chord
	 * annotation as produced by the associated analysis.
	 * 
	 * @return	The names of data targets specified by this Chord
	 * annotation as produced by the associated analysis.
	 */
	public List<String> getProduces() {
		return produces;
	}
	/**
	 * Provides the names of control targets specified by this Chord
	 * annotation as produced by the associated analysis.
	 * 
	 * @return	The names of control targets specified by this Chord
	 * annotation as produced by the associated analysis.
	 */
	public List<String> getControls() {
		return controls;
	}
	/**
	 * Provides a partial map specified by this Chord annotation from
	 * names of program relation targets consumed/produced by the
	 * associated analysis to their signatures.
	 * 
	 * @return	A partial map specified by this Chord annotation from
	 * names of program relation targets consumed/produced by the
	 * associated analysis to their signatures.
	 */
	public Map<String, RelSign> getNameToSignMap() {
		return nameToSignMap;
	}
	/**
	 * Provides a partial map specified by this Chord annotation from
	 * names of data targets consumed/produced by the associated
	 * analysis to their types.
	 * 
	 * @return	A partial map specified by this Chord annotation from
	 * names of data targets consumed/produced by the associated
	 * analysis to their types.
	 */
	public Map<String, Class> getNameToTypeMap() {
		return nameToTypeMap;
	}
};
