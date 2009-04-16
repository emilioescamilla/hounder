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
package com.flaptor.hounder.searcher.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import com.flaptor.hounder.util.TokenUtil;

/**
 * Suggests queries based on a WordSuggestor
 * @author Flaptor Development Team
 */
public class AQuerySuggestor {
    
    private static final Logger logger = Logger.getLogger(com.flaptor.util.Execute.whoAmI());
    

    // The word suggestor to ask for suggestions about tokens.
    private WordSuggestor suggestor;

    /** Creates an AQuerySuggestor with the given suggestor */
    protected AQuerySuggestor(WordSuggestor suggestor) {
        this.suggestor = suggestor;
    }
    

    /**
     * Suggest a List of AQuery for the given query.
     * Those queries are just similar, using the suggestor this
     * AQuerySuggestor uses, but may not have results associated
     * with it (a search of those queries may give empty results).
     *
     * @param query
     *          The query that needs suggestions
     * @return 
     *          A List<AQuery> of suggested queries.
     */
    public List<AQuery> suggest(AQuery query) {
        query = findLazyParsedQuery(query);
        List<AQuery> suggested = suggestLinear(query);
        return suggested;
    }
   
    private AQuery findLazyParsedQuery(AQuery query){
        if (query instanceof LazyParsedQuery) return query;
        if (query instanceof ABinaryOperator) {
            ABinaryOperator binary = (ABinaryOperator)query;
            AQuery res = findLazyParsedQuery(binary.getLeftTerm());
            if (res != null ) return res;

            res = findLazyParsedQuery(binary.getRightTerm());
            if (res != null ) return res;
        }

        // else
        return null;
    }

    private List<AQuery> suggestLinear(AQuery query) {

        List<AQuery> queries = new ArrayList<AQuery>();

        if (query instanceof LazyParsedQuery) {
            String originalString = ((LazyParsedQuery)query).getQueryString();
            StandardTokenizer tokenizer = new StandardTokenizer(new StringReader(originalString));
            List<String> tokens = new ArrayList<String>();
            try {
                while(true) {
                    Token token = tokenizer.next();
                    if (null == token) break;
                    tokens.add(TokenUtil.termText(token));
                }

                // for every word, suggest something
                for (int i =0; i< tokens.size(); i++) {
                    StringBuffer sb = new StringBuffer();
                    for (int j = 0; j < i; j++) {
                        
                            sb.append(tokens.get(j));
                            sb.append(" ");

                    }
                    String[] suggestions = suggestor.suggestWords(tokens.get(i));
                    for (String suggestion: suggestions) {
                        // generate final sb
                        StringBuffer sbf = new StringBuffer(sb);
                        sbf.append(suggestion);
                        sbf.append(" ");
                        for (int k = i+1; k < tokens.size(); k++) {
                            sbf.append(tokens.get(k));
                            sbf.append(" ");
                        }
                        queries.add(new LazyParsedQuery(sbf.toString()));
                    }
                }

            } catch (IOException e) {
                logger.error("Error while suggesting query", e);
                return new ArrayList<AQuery>(); 
            }
        } else {
            // TODO FIXME
            logger.debug("can not make suggestions for queries of type " + query.getClass());
        }

        return queries;
    }
}
