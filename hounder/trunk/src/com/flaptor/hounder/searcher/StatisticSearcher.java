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

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Statistics;

/**
 * A searcher for taking query times
 * 
 * @author Martin Massera
 */
public class StatisticSearcher implements ISearcher {

	private ISearcher searcher;
	
	public StatisticSearcher(ISearcher searcher) {
		this.searcher = searcher;
	}

	public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup group, int groupSize, AFilter filter, ASort sort)  throws SearcherException{
		long start = System.currentTimeMillis(); 
		boolean success = false;
		GroupedSearchResults results = null;
		try {
			results = searcher.search(query, firstResult, count, group, groupSize, filter, sort);
			success = true;
		} finally {
			if (success) { 
				long time = System.currentTimeMillis() - start;
				Statistics.getStatistics().notifyEventValue("responseTimes", time);
				results.setResponseTime(time);
			} else {
				Statistics.getStatistics().notifyEventError("responseTimes");
			}
		}
		if (null == results) throw new SearcherException("GroupedSearchResults is NULL");
		return results;		
	}

}
