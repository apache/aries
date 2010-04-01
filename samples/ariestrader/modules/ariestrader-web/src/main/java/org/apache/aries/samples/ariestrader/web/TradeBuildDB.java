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
package org.apache.aries.samples.ariestrader.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.aries.samples.ariestrader.api.TradeDBManager;
import org.apache.aries.samples.ariestrader.api.TradeServicesManager;
import org.apache.aries.samples.ariestrader.api.TradeServiceUtilities;
import org.apache.aries.samples.ariestrader.api.TradeServices;
import org.apache.aries.samples.ariestrader.api.persistence.*;
import org.apache.aries.samples.ariestrader.util.*;

/**
 * TradeBuildDB uses operations provided by the TradeApplication to 
 *   (a) create the Database tables 
 *   (b) populate a AriesTrader database without creating the tables. 
 * Specifically, a new AriesTrader User population is created using
 * UserIDs of the form "uid:xxx" where xxx is a sequential number 
 * (e.g. uid:0, uid:1, etc.). New stocks are also created of the form "s:xxx",
 * again where xxx represents sequential numbers (e.g. s:1, s:2, etc.)
 */
public class TradeBuildDB {

    private static TradeServicesManager tradeServicesManager = null;
    private static TradeDBManager tradeDBManager = null;

    /**
     * Populate a Trade DB using standard out as a log
     */
    public TradeBuildDB() throws Exception {
        this(new java.io.PrintWriter(System.out), null);
    }

    /**
     * Re-create the AriesTrader db tables and populate them OR just populate a 
     * AriesTrader DB, logging to the provided output stream
     */
    public TradeBuildDB(java.io.PrintWriter out, String warPath)
        throws Exception {
        String symbol, companyName;
        int errorCount = 0; // Give up gracefully after 10 errors
        if (tradeServicesManager == null) {
            tradeServicesManager = TradeServiceUtilities.getTradeServicesManager();
        }
        TradeServices tradeServices = tradeServicesManager.getTradeServices();

        if (tradeDBManager == null) {
            tradeDBManager = TradeServiceUtilities.getTradeDBManager();
        }

        // TradeStatistics.statisticsEnabled=false; // disable statistics
        out.println("<HEAD><BR><EM> TradeBuildDB: Building AriesTrader Database...</EM><BR>"
            + "This operation will take several minutes. Please wait...</HEAD>");
        out.println("<BODY>");

        if (warPath != null) {
            boolean success = false;
            String dbProductName = null;
            File ddlFile = null;
            Object[] sqlBuffer = null;

            // Find out the Database being used
            try {
                dbProductName = tradeDBManager.checkDBProductName();
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to check DB Product name");
            }
            if (dbProductName == null) {
                out.println("<BR>TradeBuildDB: **** Unable to check DB Product name,"
                    + "please check Database/AppServer configuration and retry ****</BR></BODY>");
                return;
            }

            // Locate DDL file for the specified database
            try {
                out.println("<BR>TradeBuildDB: **** Database Product detected: "
                    + dbProductName + " ****</BR>");
                if (dbProductName.startsWith("DB2/")) { // if db is DB2
                    ddlFile = new File(warPath + File.separatorChar + "dbscripts"
                        + File.separatorChar + "db2" + File.separatorChar + "Table.ddl");
                } else if (dbProductName.startsWith("Apache Derby")) { // if db is Derby
                    ddlFile = new File(warPath + File.separatorChar + "dbscripts"
                        + File.separatorChar + "derby" + File.separatorChar + "Table.ddl");
                } else if (dbProductName.startsWith("Oracle")) { // if the Db is Oracle
                    ddlFile = new File(warPath + File.separatorChar + "dbscripts"
                        + File.separatorChar + "oracle" + File.separatorChar + "Table.ddl");
                } else { // Unsupported "Other" Database
                    ddlFile = new File(warPath + File.separatorChar + "dbscripts"
                        + File.separatorChar + "other" + File.separatorChar + "Table.ddl");
                    out.println("<BR>TradeBuildDB: **** This Database is "
                        + "unsupported/untested use at your own risk ****</BR>");
                }

                if (!ddlFile.exists()) {
                    Log.error("TradeBuildDB: DDL file doesnt exist at path "
                        + ddlFile.getCanonicalPath()
                        + " , please provide the file and retry");
                    out.println("<BR>TradeBuildDB: DDL file doesnt exist at path <I>"
                        + ddlFile.getCanonicalPath() +
                        "</I> , please provide the file and retry ****</BR></BODY>");
                    return;
                }
                out.println("<BR>TradeBuildDB: **** The DDL file at path <I>"
                    + ddlFile.getCanonicalPath()
                    + "</I> will be used ****</BR>");
                out.flush();
            } catch (Exception e) {
                Log.error(e,
                    "TradeBuildDB: Unable to locate DDL file for the specified database");
                out.println("<BR>TradeBuildDB: **** Unable to locate DDL file for "
                    + "the specified database ****</BR></BODY>");
                return;
            }

            // parse the DDL file and fill the SQL commands into a buffer
            try {
                sqlBuffer = parseDDLToBuffer(ddlFile);
            } catch (Exception e) {
                Log.error(e, "TradeBuildDB: Unable to parse DDL file");
                out.println("<BR>TradeBuildDB: **** Unable to parse DDL file for the specified "+
                    "database ****</BR></BODY>");
                return;
            }
            if ((sqlBuffer == null) || (sqlBuffer.length == 0)) {
                out.println("<BR>TradeBuildDB: **** Parsing DDL file returned empty buffer, please check "+
                    "that a valid DB specific DDL file is available and retry ****</BR></BODY>");
                return;
            }

            // send the sql commands buffer to drop and recreate the AriesTrader tables
            out.println("<BR>TradeBuildDB: **** Dropping and Recreating the AriesTrader tables... ****</BR>");
            try {
                success = tradeDBManager.recreateDBTables(sqlBuffer, out);
            } catch (Exception e) {
                Log.error(e,
                    "TradeBuildDB: Unable to drop and recreate AriesTrader Db Tables, "+
                    "please check for database consistency before continuing");
            }
            if (!success) {
                out.println("<BR>TradeBuildDB: **** Unable to drop and recreate AriesTrader Db Tables, "+
                    "please check for database consistency before continuing ****</BR></BODY>");
                return;
            }
            out.println("<BR>TradeBuildDB: **** AriesTrader tables successfully created! ****</BR><BR><b> "+
                "Please Stop and Re-start your AriesTrader application (or your application server) and then use "+
                "the \"Repopulate AriesTrader Database\" link to populate your database.</b></BR><BR><BR></BODY>");
            return;
        } // end of createDBTables

        out.println("<BR>TradeBuildDB: **** Creating "
            + TradeConfig.getMAX_QUOTES() + " Quotes ****</BR>");
        // Attempt to delete all of the Trade users and Trade Quotes first
        try {
            tradeDBManager.resetTrade(true);
        } catch (Exception e) {
            Log.error(e, "TradeBuildDB: Unable to delete Trade users "+
                "(uid:0, uid:1, ...) and Trade Quotes (s:0, s:1, ...)");
        }
        for (int i = 0; i < TradeConfig.getMAX_QUOTES(); i++) {
            symbol = "s:" + i;
            companyName = "S" + i + " Incorporated";
            try {
                tradeServices.createQuote(symbol, companyName,
				    new java.math.BigDecimal(TradeConfig.rndPrice()));
                if (i % 10 == 0) {
                    out.print("....." + symbol);
                    if (i % 100 == 0) {
                        out.println(" -<BR>");
                        out.flush();
                    }
                }
            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    String error = "Populate Trade DB aborting after 10 create quote errors. Check "+
                        "the EJB datasource configuration. Check the log for details <BR><BR> Exception is: <BR> "
                        + e.toString();
                    Log.error(e, error);
                    throw e;
                }
            }
        }
        out.println("<BR>");
        out.println("<BR>**** Registering " + TradeConfig.getMAX_USERS()
            + " Users **** ");
        errorCount = 0; // reset for user registrations

        // Registration is a formal operation in Trade 2.
        for (int i = 0; i < TradeConfig.getMAX_USERS(); i++) {
            String userID = "uid:" + i;
            String fullname = TradeConfig.rndFullName();
            String email = TradeConfig.rndEmail(userID);
            String address = TradeConfig.rndAddress();
            String creditcard = TradeConfig.rndCreditCard();
            double initialBalance =
                (double) (TradeConfig.rndInt(100000)) + 200000;
            if (i == 0) {
                initialBalance = 1000000; // uid:0 starts with a cool million.
            }
            try {
                AccountDataBean accountData =
                    tradeServices.register(userID, "xxx", fullname, address,
                        email, creditcard, new BigDecimal(initialBalance));
                if (accountData != null) {
                    if (i % 50 == 0) {
                        out.print("<BR>Account# " + accountData.getAccountID()
                            + " userID=" + userID);
                    }

                    // 0-MAX_HOLDING (inclusive), avg holdings per user = (MAX-0)/2
                    int holdings = TradeConfig.rndInt(TradeConfig.getMAX_HOLDINGS() + 1); 
                    double quantity = 0;
                    for (int j = 0; j < holdings; j++) {
                        symbol = TradeConfig.rndSymbol();
                        quantity = TradeConfig.rndQuantity();
                        tradeServices.buy(userID, symbol, quantity,
						    TradeConfig.orderProcessingMode);
                    }
                    if (i % 50 == 0) {
                        out.println(" has " + holdings + " holdings.");
                        out.flush();
                    }
                } else {
                    out.println("<BR>UID " + userID
                        + " already registered.</BR>");
                    out.flush();
                }

            } catch (Exception e) {
                if (errorCount++ >= 10) {
                    String error = "Populate Trade DB aborting after 10 user registration errors. "+
                        "Check the log for details. <BR><BR> Exception is: <BR>" + e.toString();
                    Log.error(e, error);
                    throw e;
                }
            }
        } // end-for
        out.println("</BODY>");
    }

    public Object[] parseDDLToBuffer(File ddlFile) throws Exception {
        BufferedReader br = null;
        ArrayList sqlBuffer = new ArrayList(30); // initial capacity 30 assuming we have 30 ddl-sql statements to read

        try {
            if (Log.doTrace())
                Log.traceEnter("TradeBuildDB:parseDDLToBuffer - " + ddlFile);

            br = new BufferedReader(new FileReader(ddlFile));
            String s;
            String sql = new String();
            while ((s = br.readLine()) != null) {
                s = s.trim();
                if ((s.length() != 0) && (s.charAt(0) != '#')) // Empty lines or lines starting with "#" are ignored
                {
                    sql = sql + " " + s;
                    if (s.endsWith(";")) // reached end of sql statement
                    {
                        sql = sql.replace(';', ' '); // remove the semicolon
                        sqlBuffer.add(sql);
                        sql = "";
                    }
                }
            }
        } catch (IOException ex) {
            Log.error("TradeBuildDB:parseDDLToBuffer Exeception during open/read of File: "
                + ddlFile, ex);
            throw ex;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Log.error("TradeBuildDB:parseDDLToBuffer Failed to close BufferedReader",
                        ex);
                }
            }
        }
        return sqlBuffer.toArray();
    }

    public static void main(String args[]) throws Exception {
        new TradeBuildDB();

    }
}
