/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util.graph;

/**
 * Specification of a visitor over edges in a path of a
 * directed graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IPathVisitor<Node> {
	/**
	 * Method called while visiting each edge in a path of a
	 * directed graph.
	 * 
	 * @param	origNode	The source node of the visited edge.
	 * @param	destNode	The target node of the visited edge.
	 * 
	 * @return	A string-valued result of visiting the edge.
	 */
	public String visit(Node origNode, Node destNode);
}
