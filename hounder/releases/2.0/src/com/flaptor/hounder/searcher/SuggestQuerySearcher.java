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

import java.util.List;

import org.apache.log4j.Logger;

import com.flaptor.hounder.searcher.filter.AFilter;
import com.flaptor.hounder.searcher.group.AGroup;
import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.hounder.searcher.query.AQuerySuggestor;
import com.flaptor.hounder.searcher.sort.ASort;
import com.flaptor.util.Statistics;

/**
 * This searcher tries a suggested query and if the suggested results
 * are significant (> original results * factor) it makes the suggestion
 * 
 * @author Martin Massera
 */
public class SuggestQuerySearcher implements ISearcher{
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    
    private ISearcher searcher;
    private AQuerySuggestor suggestor;
    private float suggestionBetterByFactor;
    private int groupsThreshold;
    private int maxSuggestionsToTry;

    /**
     * @param searcher the base searcher
     * @param suggestor suggests queries
     * @param factor if (suggested results > original results * factor) it suggests the results
     */
    public SuggestQuerySearcher(ISearcher searcher, AQuerySuggestor suggestor, int groupsThreshold, float suggestionBetterByFactor, int maxSuggestionsToTry) {
        if (null == searcher) {
            throw new IllegalArgumentException("searcher cannot be null.");
        }
        if (null == suggestor) {
            throw new IllegalArgumentException("suggestor cannot be null.");
        }
        this.searcher = searcher;
        this.suggestor = suggestor;
        this.suggestionBetterByFactor = suggestionBetterByFactor;
        this.groupsThreshold = groupsThreshold;
        this.maxSuggestionsToTry = maxSuggestionsToTry;
    }

    public GroupedSearchResults search(AQuery query, int firstResult, int count, AGroup groupBy, int groupSize, AFilter afilter, ASort asort)  throws SearcherException{        
        GroupedSearchResults res = searcher.search(query, firstResult, count, groupBy, groupSize, afilter, asort);
        if (null == res) { throw new SearcherException("GroupedSearchResults is NULL"); }

        // Check if there's the need to find a suggested query.
        if (res.totalGroupsEstimation() < groupsThreshold) {  
            if (logger.isDebugEnabled()) { logger.debug("did not get enough results for query " + query.toString() + " . trying to suggest."); }
            long start = System.currentTimeMillis();
            // suggest queries for this query
            List<AQuery> suggestions = suggestor.suggest(query);
            int bestSuggestion = -1;
            float minResultCount = res.totalResults() * suggestionBetterByFactor;
// System.out.println("Suggestion for query ["+query.toString()+"]: "+suggestions.toString());
            for (int i=0; i<suggestions.size() && i<maxSuggestionsToTry; i++) {
// System.out.println("  "+suggestions.get(i).toString());
                if (logger.isDebugEnabled()) { logger.debug("tryng suggested query " + suggestions.get(i).toString() + " for query " + query.toString()); }
                // Do the search with the suggested query, using the same filter and grouping and ignoring 
                // sorting as it doesn't affect the number of results and slows down the operation.
                GroupedSearchResults resSuggested = searcher.search(suggestions.get(i), 0, 1, groupBy, groupSize, afilter, null);
                // check that there are enough results for suggestion
// System.out.println("  ..."+resSuggested.totalGroupsEstimation()+" results");
                if (resSuggested.totalGroupsEstimation() > minResultCount) {
                    bestSuggestion = i;
                    minResultCount = resSuggested.totalGroupsEstimation();
                }
            }
// System.out.println("  best: "+bestSuggestion);
            if (bestSuggestion >= 0) {
                res.setSuggestedQuery(suggestions.get(bestSuggestion));
            }
            long end = System.currentTimeMillis();
            Statistics.getStatistics().notifyEventValue("suggestQuery", (end - start)/1000.0f);
        }
        return res;
    }

    @Override
    public void requestStop() {
        searcher.requestStop();
    }

    @Override
    public boolean isStopped() {
        return searcher.isStopped();
    }
}
