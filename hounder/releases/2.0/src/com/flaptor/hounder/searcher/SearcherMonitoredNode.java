/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/
package com.flaptor.hounder.searcher;

import java.util.ArrayList;

import com.flaptor.hounder.clusterfest.HounderMonitoreable;
import com.flaptor.hounder.searcher.group.NoGroup;
import com.flaptor.hounder.searcher.query.LazyParsedQuery;
import com.flaptor.util.Pair;
import com.flaptor.util.Statistics;

/**
 * implementation of MonitoredNode for monitoring a searcher
 * 
 * @author Martin Massera
 */
public class SearcherMonitoredNode extends HounderMonitoreable {

	CompositeSearcher searcher;

	public SearcherMonitoredNode(CompositeSearcher searcher) {
		this.searcher = searcher;
	}

	public void updateProperties() {
		super.updateProperties();
		Statistics stats = Statistics.getStatistics();
		TrafficLimitingSearcher tls = searcher.getTrafficLimitingSearcher();
		if (null != tls) {
			setProperty("maxSimultaneousQueries", String.valueOf(tls.getMaxSimultaneousQueries()));
			setProperty("simultaneousQueries", String.valueOf(tls.getSimultaneousQueries()));
		}
		try {
            setProperty("searcherException", null);
            searcher.search(new LazyParsedQuery("testing123"), 0, 1, new NoGroup(), 1, null,null);
        } catch (Throwable t) {
            setProperty("searcherException", t.getMessage());
        }
	}
}
