/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.samples.ariestrader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

public class Log {
	private final static Logger log = LoggerFactory.getLogger(Log.class);
//A general purpose, high performance logging, tracing, statistic service

	public static void log(String message)
	{
        log.debug("AriesTrader Log:" + new java.util.Date() + "------\n\t ");
		log.debug(message);
	}
	public static void log(String msg1, String msg2)
	{
		log(msg1+msg2);
	}
	public static void log(String msg1, String msg2, String msg3)
	{
		log(msg1+msg2+msg3);
	}
	
	public static void error(String message)
	{
		message = "Error: " + message;
		log.error(message);
	}
	public static void error(String message, Throwable e)
	{
		error(message+"\n\t"+e.toString());
		e.printStackTrace(System.out);
	}
	public static void error(String msg1, String msg2, Throwable e)
	{
		error(msg1+"\n"+msg2+"\n\t", e);
	}
	public static void error(String msg1, String msg2, String msg3, Throwable e)
	{
		error(msg1+"\n"+msg2+"\n"+msg3+"\n\t", e);
	}
	public static void error(Throwable e, String message)
	{
		error(message+"\n\t",e);
		e.printStackTrace(System.out);
	}
	public static void error(Throwable e, String msg1, String msg2)
	{
		error(msg1+"\n"+msg2+"\n\t",e);
	}
	public static void error(Throwable e, String msg1, String msg2, String msg3)
	{
		error(msg1+"\n"+msg2+"\n"+msg3+"\n\t", e);
	}
	
	
	public static void trace(String message)
	{
		log.trace(message + " threadID="+ Thread.currentThread());
	}

	public static void trace(String message, Object parm1)
	{
		trace(message+"("+parm1+")");
	}

	public static void trace(String message, Object parm1, Object parm2)
	{
		trace(message+"("+parm1+", "+parm2+")");
	}

	public static void trace(String message, Object parm1, Object parm2, Object parm3)
	{
		trace(message+"("+parm1+", "+parm2+", "+parm3+")");
	}
	public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4)
	{
		trace(message+"("+parm1+", "+parm2+", "+parm3+")"+", "+parm4);
	}
	public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, Object parm5)
	{
		trace(message+"("+parm1+", "+parm2+", "+parm3+")"+", "+parm4+", "+parm5);
	}
	public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, 
								Object parm5, Object parm6)
	{
		trace(message+"("+parm1+", "+parm2+", "+parm3+")"+", "+parm4+", "+parm5+", "+parm6);
	}
	public static void trace(String message, Object parm1, Object parm2, Object parm3, Object parm4, 
	          					Object parm5, Object parm6, Object parm7)
	{
		trace(message+"("+parm1+", "+parm2+", "+parm3+")"+", "+parm4+", "+parm5+", "+parm6+", "+parm7);
	}
	public static void traceEnter(String message)
	{
		log.trace("Method enter --" + message);
	}
	public static void traceExit(String message)
	{
		log.trace("Method exit  --" + message);
	}
	
	
	public static void stat(String message)
	{
		log(message);
	}

	public static void debug(String message)
	{
		log.debug(message);
	}

	public static void print(String message)
	{
		log(message);
	}
	
	public static void printObject(Object o)
	{
		log("\t"+o.toString());
	}
		
	public static void printCollection(Collection c)
	{
		log("\t---Log.printCollection -- collection size=" + c.size());
		Iterator it = c.iterator();
		while ( it.hasNext() )
		{
			log("\t\t"+it.next().toString());
		}
		log("\t---Log.printCollection -- complete");		
	}
	
	public static void printCollection(String message, Collection c)
	{
		log(message);
		printCollection(c);
	}
	
	public static boolean doActionTrace()
	{
		return getTrace() || getActionTrace();
	}

	public static boolean doTrace()
	{
		return getTrace();
	}
	
	public static boolean doDebug()
	{
		return true;
	}
	
	public static boolean doStat()
	{
		return true;
	}		
	
	/**
	 * Gets the trace
	 * @return Returns a boolean
	 */
	public static boolean getTrace() {
		return TradeConfig.getTrace();
	}
	/**
	 * Gets the trace value for Trade actions only
	 * @return Returns a boolean
	 */
	public static boolean getActionTrace() {
		return TradeConfig.getActionTrace();
	}
		
	/**
	 * Sets the trace
	 * @param traceValue The trace to set
	 */
	public static void setTrace(boolean traceValue)
	{
		TradeConfig.setTrace(traceValue);
	}
	/**
	 * Sets the trace value for Trade actions only
	 * @param traceValue The trace to set
	 */
	public static void setActionTrace(boolean traceValue)
	{
		TradeConfig.setActionTrace(traceValue);
	}
}

