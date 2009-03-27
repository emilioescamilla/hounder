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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import org.apache.lucene.document.Field;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.flaptor.hounder.searcher.query.AQuery;
import com.flaptor.util.Config;
import com.flaptor.util.DomUtil;
import com.flaptor.util.StringUtil;
import java.util.List;

/**
 * @author Flaptor Development Team
 */
public class XmlResults {

    /**
     * Private empty default constructor to prevent inheritance and instantiation.
     */
    private XmlResults() {}

    /**
     * Creates a XML search results document.
     * The generated dom contains only valid xml characters (infringing chars are removed).
     * @param queryString the query string, as entered by the user
     * @param start the offset of the first result
     * @param count the number of results requested (the actual number of results found may be smaller)
     * @param orderBy the field by which the results are sorted
     * @param sr the GroupedSearchResults structure containing the result of performing the query
     * @param status the code returned by the searcher
     * @param statusDesc the status description
     * @return a DOM document
     * <br>An empty sr argument means that no results were found.
     */
    public static final Document buildXml(String queryString, int start, int count, String orderBy, GroupedSearchResults sr, int status, String statusMessage, String xsltUri) {

        Document dom = DocumentHelper.createDocument();
        if (null != xsltUri) {
            Map<String,String> map = new HashMap<String,String>();
            map.put("type", "text/xsl");
            map.put("href", xsltUri);
            dom.addProcessingInstruction("xml-stylesheet", map);
        }
        Element root;
        Element group;
        root = dom.addElement("SearchResults");

        root.addElement("totalResults").addText(Integer.toString(sr.totalGroupsEstimation()));
        root.addElement("startIndex").addText(Integer.toString(start));
        root.addElement("itemsPerPage").addText(Integer.toString(count));
        root.addElement("orderBy").addText(DomUtil.filterXml(orderBy));
        root.addElement("query").addText(DomUtil.filterXml(queryString));
        AQuery suggestedQuery = sr.getSuggestedQuery();
        if (null != suggestedQuery) {
            root.addElement("suggestedQuery").addText(DomUtil.filterXml(suggestedQuery.toString()));
        }
        root.addElement("status").addText(Integer.toString(status));
        root.addElement("statusDesc").addText(statusMessage);

        for (int i=0; i<sr.groups(); i++) {
            String name = sr.getGroup(i).first();
            group = root.addElement("group").addAttribute("name", name);
            Vector<org.apache.lucene.document.Document> docs = sr.getGroup(i).last();
            for (int j = 0; j < docs.size(); j++) {
                org.apache.lucene.document.Document doc = sr.getGroup(i).last().get(j);                
                createAndAddElement(doc, group);
            }
        }
        return dom;
    }

    private static Element createAndAddElement(org.apache.lucene.document.Document doc, Element parent) {
        Element item = parent.addElement("result");            
        for (Iterator iter = doc.getFields().iterator(); iter.hasNext(); ) {
            Field f = (Field) iter.next();
            item.addElement(f.name()).addText(DomUtil.filterXml(f.stringValue()));
        }
        return item;
    }
}

