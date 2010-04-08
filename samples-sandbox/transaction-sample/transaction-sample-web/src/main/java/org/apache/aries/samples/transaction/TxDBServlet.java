/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.samples.transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;

public class TxDBServlet extends HttpServlet {
		
	private static final long serialVersionUID = 1L;
	private static final String OSGI_BUNDLECONTEXT_ATTRIBUTE = "osgi-bundlecontext";
	private static final String CLEAN_TABLE = "delete from txDemo";	
	private static final String SELECT_TABLE = "SELECT * FROM txDemo";
	private static final String INSERT_INTO_TABLE = "INSERT INTO txDemo VALUES(?)";
	private static final String LOGO_HEADER = "<TABLE border='0\' cellpadding=\'0\' cellspacing='0' width='100%'> " +
							"<TR> " +
							"<TD align='left' class='topbardiv' nowrap=''>" +
							"<A href='http://incubator.apache.org/aries/' title='Apache Aries (incubating)'>" +
							"<IMG border='0' src='http://incubator.apache.org/aries/images/Arieslogo_Horizontal.gif'>" +
							"</A>" +
							"</TD>" +

							"<TD align='right' nowrap=''>" + 
							"<A href='http://www.apache.org/' title='The Apache Software Foundation'>" +
							"<IMG border='0' src='http://incubator.apache.org/aries/images/apache-incubator-logo.png'>" +
							"</A>"+
							"</TD>"+

							"</TR>"+
							"</TABLE>";
	
	private PrintWriter pw = null;

	public void init() throws ServletException {
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		pw = response.getWriter();
	    pw.write(LOGO_HEADER);
		// Get the bundle context from ServletContext attributes
		BundleContext ctx = (BundleContext) getServletContext().getAttribute(OSGI_BUNDLECONTEXT_ATTRIBUTE);

		pw.println("<html>");
		pw.println("<head>");
		pw.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"tableTemplate.css\"/>");
		pw.println("<body>");
		pw.println("<h2 align=center><p><font face=\"Tahoma\">Sample Application for JDBC usage in JTA Transactions.</font></h2>");
		pw.println("<p><p>");
		
		ServiceReference tmServiceRef = ctx.getServiceReference("javax.transaction.TransactionManager");
		ServiceReference derbyServiceRef = ctx.getServiceReference(DataSourceFactory.class.getName());

		if(tmServiceRef == null || derbyServiceRef == null){
			pw.println("<font face=\"Tahoma\">TransactionManager or Derby driver are not available in the OSGI registry.</font><br>");
		} else {
			TransactionManager tm = (TransactionManager)ctx.getService(tmServiceRef);
			DataSourceFactory derbyRegistry = (DataSourceFactory)ctx.getService(derbyServiceRef);
			try{
				// Set the needed properties for the database connection
				Properties props = new Properties();
				props.put(DataSourceFactory.JDBC_URL, "jdbc:derby:txDemo");
				props.put(DataSourceFactory.JDBC_DATABASE_NAME, "txDemo");
				props.put(DataSourceFactory.JDBC_USER, "");
				props.put(DataSourceFactory.JDBC_PASSWORD, "");
				XADataSource xaDataSource = derbyRegistry.createXADataSource(props);

				pw.println("<center><form><table><tr><td align='right'>Value: </td><td align=left><input type='text' name='value' value='' size='12'/><input type='submit' name='action' value='InsertAndCommit' size='100'/></td></tr>");
				pw.println("<tr><td align='right'>Value: </td><td align=left><input type='text' name='value' value='' size='12'/><input type='submit' name='action' value='InsertAndRollback' size='100'/></center></td></tr>");
				pw.println("<tr colspan='2' align='center'><td>&nbsp;</td><td><input type='submit' name='action' value='cleanTable' size='100' />&nbsp;<input type='submit' name='action' value='printTable' size='100'/></td><tr></table></form></center>");				
				
				
				String value = request.getParameter("value");
				String action = request.getParameter("action");
				
				if(action != null && action.equals("InsertAndCommit")){
					insertIntoTransaction(xaDataSource, tm, value, true);
				} else if(action != null && action.equals("InsertAndRollback")){
					insertIntoTransaction(xaDataSource, tm, value, false);
				} else if(action != null && action.equals("cleanTable")){
					cleanTable(xaDataSource);
				}
				printTable(xaDataSource);
			} catch (Exception e){
				pw.println("<font face=\"Tahoma\">Unexpected exception occurred "+ e.toString()+".</font><br>");
				e.printStackTrace(pw);
			}

		}
		pw.println("</body>");
		pw.println("</html>");
		pw.flush();
	}// end of doGet(HttpServletRequest request, HttpServletResponse response)

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		doGet(request, response);
	}// end of doPost(HttpServletRequest request, HttpServletResponse response)


	/**
	 * Prints the table 
	 * @param xaDataSource
	 * @throws SQLException
	 */
	private void printTable(XADataSource xaDataSource) throws SQLException {
		XAConnection xaConnection = xaDataSource.getXAConnection();
		Connection connection = xaConnection.getConnection();		
		PreparedStatement selectStatement = connection.prepareStatement(SELECT_TABLE);
		ResultSet set = selectStatement.executeQuery();		
		pw.write("<center><br><table>");
		  pw.write("<tr BGCOLOR=#FFCC33>");
		  pw.write("<td><font face=\"Tahoma\"><b>VALUES</font></td>");
		  pw.write("</tr>");
			  while (set.next()) {
				  pw.write("<tr>");
				  pw.write("<td><font face=\"Tahoma\">");
				  pw.write(set.getString("value"));
				  pw.write("</font></td>");
				  pw.write("</tr>");
			  }
		  pw.write("</table><br></center>");
	}
	
	/**
	 * This method demonstrates how to enlist JDBC connection into Transaction according OSGi enterprise specification.
	 * 
	 * @param xads XADataSource
	 * @param tm TransactionManager
	 * @param value which will be inserted into table
	 * @param toCommit Specify if the transaction will be committed or rolledback
	 * @throws SQLException
	 * @throws GenericJTAException
	 */
	private void insertIntoTransaction(XADataSource xads, TransactionManager tm, String value, boolean toCommit) throws SQLException, GenericJTAException{
		
		XAConnection xaConnection = xads.getXAConnection();
		Connection connection = xaConnection.getConnection();
		XAResource xaResource  = xaConnection.getXAResource();
		try{
			tm.begin();
			Transaction transaction = tm.getTransaction();
			transaction.enlistResource(xaResource);
	
			PreparedStatement insertStatement = connection.prepareStatement(INSERT_INTO_TABLE);
			insertStatement.setString(1, value);
			insertStatement.executeUpdate();
			if(toCommit){
				transaction.commit();
			} else {
				transaction.rollback();
			}
		}catch(RollbackException e){
			throw new GenericJTAException(e);	  
		} catch (SecurityException e) {
			throw new GenericJTAException(e);	  
		} catch (IllegalStateException e) {
			throw new GenericJTAException(e);	  
		} catch (HeuristicMixedException e) {
			throw new GenericJTAException(e);
		} catch (HeuristicRollbackException e) {
			throw new GenericJTAException(e);
		} catch (SystemException e) {
			throw new GenericJTAException(e);
		} catch (NotSupportedException e) {
			throw new GenericJTAException(e);
		}	
	}
	
	
	/**
	 * Cleans the Table
	 * 
	 * @param xaDataSource
	 * @throws SQLException
	 */
	private void cleanTable(XADataSource xaDataSource) throws SQLException {
		XAConnection xaConnection = xaDataSource.getXAConnection();
		Connection connection = xaConnection.getConnection();
		PreparedStatement statement = connection.prepareStatement(CLEAN_TABLE);
		statement.executeUpdate();
	}

}
