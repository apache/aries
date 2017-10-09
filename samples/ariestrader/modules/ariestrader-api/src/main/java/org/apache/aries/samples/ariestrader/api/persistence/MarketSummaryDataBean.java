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
package org.apache.aries.samples.ariestrader.api.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.aries.samples.ariestrader.util.FinancialUtils;
import org.apache.aries.samples.ariestrader.util.Log;
import org.apache.aries.samples.ariestrader.util.TradeConfig;

public class MarketSummaryDataBean implements Serializable
{

	private BigDecimal 	TSIA;			/* Trade Stock Index Average */
	private BigDecimal 	openTSIA;		/* Trade Stock Index Average at the open */	
	private double  	volume; 		/* volume of shares traded */
	private Collection 	topGainers;		/* Collection of top gaining stocks */
	private Collection 	topLosers;		/* Collection of top losing stocks */	
	//FUTURE private Collection 	topVolume;		/* Collection of top stocks by volume */		
	private Date			summaryDate;   /* Date this summary was taken */
	
	//cache the gainPercent once computed for this bean
	private BigDecimal  gainPercent=null;

	public MarketSummaryDataBean(){ }
	public MarketSummaryDataBean(BigDecimal TSIA,
							BigDecimal  openTSIA,
							double		volume,
							Collection 	topGainers,
							Collection 	topLosers//, Collection topVolume
							)
	{
		setTSIA(TSIA);
		setOpenTSIA(openTSIA);
		setVolume(volume);
		setTopGainers(topGainers);
		setTopLosers(topLosers);
		setSummaryDate(new java.sql.Date(System.currentTimeMillis()));
		gainPercent = FinancialUtils.computeGainPercent(getTSIA(), getOpenTSIA());
		
	}
	

	public String toString()
	{
		StringBuilder ret = new StringBuilder();
		ret.append("\n\tMarket Summary at: ").append(getSummaryDate())
		   .append("\n\t\t        TSIA:").append(getTSIA())
		   .append("\n\t\t    openTSIA:").append(getOpenTSIA())
		   .append("\n\t\t        gain:").append(getGainPercent())
		   .append("\n\t\t      volume:").append(getVolume());

		if ( (getTopGainers()==null) || (getTopLosers()==null) )
			return ret.toString();
		ret.append("\n\t\t   Current Top Gainers:");
		Iterator it = getTopGainers().iterator();
		while ( it.hasNext() ) 
		{
			QuoteDataBean quoteData = (QuoteDataBean) it.next();
			ret.append("\n\t\t\t").append(quoteData.toString());
		}
		ret.append("\n\t\t   Current Top Losers:");
		it = getTopLosers().iterator();
		while ( it.hasNext() ) 
		{
			QuoteDataBean quoteData = (QuoteDataBean) it.next();
			ret.append("\n\t\t\t").append(quoteData.toString());
		}
		return ret.toString();		
	}
	public String toHTML()
	{
		String ret = "<BR>Market Summary at: " + getSummaryDate()
			+ "<LI>        TSIA:" + getTSIA() + "</LI>"
			+ "<LI>    openTSIA:" + getOpenTSIA() + "</LI>"
			+ "<LI>      volume:" + getVolume() + "</LI>"
			;
		if ( (getTopGainers()==null) || (getTopLosers()==null) )
			return ret;
		ret += "<BR> Current Top Gainers:";
		Iterator it = getTopGainers().iterator();
		while ( it.hasNext() ) 
		{
			QuoteDataBean quoteData = (QuoteDataBean) it.next();
			ret += ( "<LI>"  + quoteData.toString()  + "</LI>" );
		}
		ret += "<BR>   Current Top Losers:";
		it = getTopLosers().iterator();
		while ( it.hasNext() ) 
		{
			QuoteDataBean quoteData = (QuoteDataBean) it.next();
			ret += ( "<LI>"  + quoteData.toString()  + "</LI>" );
		}
		return ret;
	}
	public void print()
	{
		Log.log( this.toString() );
	}	
	
	public BigDecimal getGainPercent()
	{
		if ( gainPercent == null )
			gainPercent = FinancialUtils.computeGainPercent(getTSIA(), getOpenTSIA());
		return gainPercent;
	}


	/**
	 * Gets the tSIA
	 * @return Returns a BigDecimal
	 */
	public BigDecimal getTSIA() {
		return TSIA;
	}
	/**
	 * Sets the tSIA
	 * @param tSIA The tSIA to set
	 */
	public void setTSIA(BigDecimal tSIA) {
		TSIA = tSIA;
	}

	/**
	 * Gets the openTSIA
	 * @return Returns a BigDecimal
	 */
	public BigDecimal getOpenTSIA() {
		return openTSIA;
	}
	/**
	 * Sets the openTSIA
	 * @param openTSIA The openTSIA to set
	 */
	public void setOpenTSIA(BigDecimal openTSIA) {
		this.openTSIA = openTSIA;
	}

	/**
	 * Gets the volume
	 * @return Returns a BigDecimal
	 */
	public double getVolume() {
		return volume;
	}
	/**
	 * Sets the volume
	 * @param volume The volume to set
	 */
	public void setVolume(double volume) {
		this.volume = volume;
	}

	/**
	 * Gets the topGainers
	 * @return Returns a Collection
	 */
	public Collection getTopGainers() {
		return topGainers;
	}
	/**
	 * Sets the topGainers
	 * @param topGainers The topGainers to set
	 */
	public void setTopGainers(Collection topGainers) {
		this.topGainers = topGainers;
	}

	/**
	 * Gets the topLosers
	 * @return Returns a Collection
	 */
	public Collection getTopLosers() {
		return topLosers;
	}
	/**
	 * Sets the topLosers
	 * @param topLosers The topLosers to set
	 */
	public void setTopLosers(Collection topLosers) {
		this.topLosers = topLosers;
	}

	/**
	 * Gets the summaryDate
	 * @return Returns a Date
	 */
	public Date getSummaryDate() {
		return summaryDate;
	}
	/**
	 * Sets the summaryDate
	 * @param summaryDate The summaryDate to set
	 */
	public void setSummaryDate(Date summaryDate) {
		this.summaryDate = summaryDate;
	}

}
