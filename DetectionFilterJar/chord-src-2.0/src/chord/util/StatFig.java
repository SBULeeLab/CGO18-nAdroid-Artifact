/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chord.project.Config;

/**
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public class StatFig {
  public double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, sum = 0;
  public int n = 0;

  public double mean() { return sum/n; }

  public void add(double x) {
	sum += x;
	n += 1;
	min = Math.min(min, x);
	max = Math.max(max, x);
  }

  @Override public String toString() {
	return String.format("%.2f / %.2f / %.2f (%d)", min, sum/n, max, n);
  }
}
