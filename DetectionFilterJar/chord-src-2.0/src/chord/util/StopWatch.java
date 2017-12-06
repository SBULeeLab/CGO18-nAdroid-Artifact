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
 * Simple class for measuring elapsed time.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public class StopWatch
{
	public StopWatch()
	{
	}

	public StopWatch(long ms)
	{
		startTime = 0;
		endTime = ms;
		this.ms = ms;
	}

	public void reset()
	{
		ms = 0;
		isRunning = false;
	}

	public StopWatch start()
	{
	assert !isRunning;
		isRunning = true;
		startTime = System.currentTimeMillis();

		return this;
	}

	public StopWatch stop()
	{
		assert isRunning;
		endTime = System.currentTimeMillis();
		isRunning = false;
		ms = endTime - startTime;
		n = 1;
		return this;
	}

	public StopWatch accumStop()
	{
	// Stop and accumulate time
		assert isRunning;
		endTime = System.currentTimeMillis();
		isRunning = false;
		ms += endTime - startTime;
		n++;
		return this;
	}

  public void add(StopWatch w) {
	assert !isRunning && !w.isRunning;
	ms += w.ms;
	n += w.n;
  }

	public long getCurrTimeLong()
	{
		return ms + (isRunning() ? System.currentTimeMillis() - startTime : 0);
	}

	@Override
	public String toString()
	{
		long msCopy = ms;
		long m = msCopy / 60000;
		msCopy %= 60000;
		long h = m / 60;
		m %= 60;
		long d = h / 24;
		h %= 24;
		long y = d / 365;
		d %= 365;
		long s = msCopy / 1000;

		StringBuilder sb = new StringBuilder();

		if (y > 0)
		{
			sb.append(y);
			sb.append('y');
			sb.append(d);
			sb.append('d');
		}
		if (d > 0)
		{
			sb.append(d);
			sb.append('d');
			sb.append(h);
			sb.append('h');
		}
		else if (h > 0)
		{
			sb.append(h);
			sb.append('h');
			sb.append(m);
			sb.append('m');
		}
		else if (m > 0)
		{
			sb.append(m);
			sb.append('m');
			sb.append(s);
			sb.append('s');
		}
		else if (s > 9)
		{
			sb.append(s);
			sb.append('s');
		}
		else if (s > 0)
		{
			sb.append((ms / 100) / 10.0);
			sb.append('s');
		}
		else
		{
			sb.append(ms / 1000.0);
			sb.append('s');
		}
		return sb.toString();
	}

	public long startTime, endTime, ms;

	public int n;

	private boolean isRunning = false;

	public boolean isRunning()
	{
		return isRunning;
	}
}
