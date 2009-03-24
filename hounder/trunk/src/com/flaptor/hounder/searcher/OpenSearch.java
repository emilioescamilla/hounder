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

/**
 * @author Flaptor Development Team
 */
public class OpenSearch {

    private static final String XMLNS_A9_OPENSEARCH_1_0 = "http://a9.com/-/spec/opensearchrss/1.0/";
    private static final String XMLNS_HOUNDER_OPENSEARCH_1_0 = "http://www.flaptor.com/opensearchrss/1.0/";

    private static final Set<String> fieldsToShow = new HashSet<String>();

    /**
     * OpenSearch standard, forces to have 3 fields: title, link, description
     * The name in the index of those fields might be different so we have to
     * map between the index-name and the opensearch name.
     * This field map the 'description' field
     */
    private static final String descField;
    private static final String descPrefix;
    private static final String linkField;
    private static final String linkPrefix;
    private static final String titleField;
    private static final String titlePrefix;
    private static final String xsltPath;

    static {
        Config config = Config.getConfig("opensearch.properties");
        String[] fieldList = config.getStringArray("opensearch.show.hounder.fields");
        titlePrefix = config.getString("opensearch.title.prefix");
        titleField = config.getString("opensearch.title.from.index.field");
        linkPrefix = config.getString("opensearch.link.prefix");
        linkField = config.getString("opensearch.link.from.index.field");
        descField = config.getString("opensearch.description.from.index.field");    	
        descPrefix = config.getString("opensearch.description.prefix");    	
        fieldsToShow.addAll(Arrays.asList(fieldList));
        xsltPath = config.getString("opensearch.xsltPath");
    }

    /**
     * Private empty default constructor to prevent inheritance and instantiation.
     */
    private OpenSearch() {}

    /**
     * Creates a OpenSearch's compatible DOM document.
     * The generated dom contains only valid xml characters (infringing chars are removed).
     * Compliant with OpenSearch 1.0 with most of the Nutch 0.8.1 extensions.
     * @param baseUrl the url of the webapp
     * @param htmlSearcher the name of the component (servlet/jsp) that returns the search results in an HTML page
     * @param opensearchSearcher the name of the component (servlet/jsp) that returns the search results in an OpenSearch RSS page
     * @param extraParams the parameters present in the request, not passed explicitly (such as sort, reverse, etc.)
     * @param queryString the query string, as entered by the user
     * @param start the offset of the first result
     * @param count the number of results requested (the actual number of results found may be smaller)
     * @param sr the SearchResults structure containing the result of performing the query
     * @return a DOM document
     * <br>An empty sr argument means that no results were found.
     */
    public static final Document buildDom_1_0(String baseUrl, String htmlSearcher, String opensearchSearcher, String extraParams, String queryString, int start, int count, GroupedSearchResults sr, int status, String statusMessage, boolean useXslt) {

        String encodedQuery = null;
        try {
            encodedQuery = URLEncoder.encode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen!
            encodedQuery = "";
        }
        Document dom = DocumentHelper.createDocument();
        if (useXslt) {
            Map<String,String> map = new HashMap<String,String>();
            map.put("type", "text/xsl");
            map.put("href", xsltPath);
            dom.addProcessingInstruction("xml-stylesheet", map);
        }

        Namespace opensearchNs = DocumentHelper.createNamespace("opensearch", XMLNS_A9_OPENSEARCH_1_0);
        Namespace hounderNs = DocumentHelper.createNamespace("hounder", XMLNS_HOUNDER_OPENSEARCH_1_0);
        Element root;
        Element channel;
        if (!useXslt) {
            root = dom.addElement("rss").
            addAttribute("version", "2.0");
            channel = root.addElement("channel");
        } else {
            channel = dom.addElement("searchResults");
            root = channel;
        } 
        root.add(opensearchNs);
        root.add(hounderNs);

        channel.addElement("title").addText(titlePrefix+" "+DomUtil.filterXml(queryString));
        channel.addElement("link").addText(baseUrl + "/" + htmlSearcher +
                "?query=" +encodedQuery + "&start=" + start + extraParams);
        channel.addElement("description").addText(descPrefix+" "+DomUtil.filterXml(queryString));
        channel.addElement(QName.get("totalResults", opensearchNs)).addText(Integer.toString(sr.totalGroupsEstimation()));
        channel.addElement(QName.get("startIndex", opensearchNs)).addText(Integer.toString(start));
        channel.addElement(QName.get("itemsPerPage", opensearchNs)).addText(Integer.toString(count));
        channel.addElement(QName.get("query",hounderNs)).addText(DomUtil.filterXml(queryString));
        AQuery suggestedQuery = sr.getSuggestedQuery();
        if (null != suggestedQuery) {
            channel.addElement(QName.get("suggestedQuery",hounderNs)).addText(DomUtil.filterXml(suggestedQuery.toString()));
        }
        channel.addElement(QName.get("status",hounderNs)).addText(Integer.toString(status));
        channel.addElement(QName.get("statusDesc",hounderNs)).addText(statusMessage);
        if (sr.lastDocumentOffset() > 0) {
            channel.addElement(QName.get("nextPage",hounderNs)).addText(baseUrl + "/" + opensearchSearcher + 
                    "?query=" + encodedQuery + "&start=" + (sr.lastDocumentOffset()) + extraParams);
        }

        for (int i=0; i< sr.groups(); i++) {
            Vector<org.apache.lucene.document.Document> docs= sr.getGroup(i).last();
            Element parent= null;
            for (int j = 0; j < docs.size(); j++) {
                org.apache.lucene.document.Document doc = sr.getGroup(i).last().get(j);                
                if (0 == j) {// j=0 is head of group. j>0 is tail
                    parent= createAndAddElement(doc, channel, hounderNs);
                } else {
                    createAndAddElement(doc, parent, hounderNs);
                }

            }
        }
        return dom;
    }

    private static Element createAndAddElement( org.apache.lucene.document.Document doc, 
            Element parent, Namespace hounderNs){
        String link= StringUtil.nullToEmpty(doc.get(linkField)).trim();
        String description= StringUtil.nullToEmpty(doc.get(descField)).trim();
        String title= StringUtil.nullToEmpty(doc.get(titleField)).trim();           
        if ("".equals(title)) {
            title=link;
        }            

        Element item = parent.addElement("item");            
        item.addElement("title").addText(DomUtil.filterXml(title));
        item.addElement("link").addText(linkPrefix + DomUtil.filterXml(link));
        String desc = DomUtil.filterXml(description);
        System.out.println("===================================================================================");
        System.out.println("description: " + description);
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println("desc: " + desc);
        System.out.println("===================================================================================");
        item.addElement("description").addText(desc);

        for (Iterator iter = doc.getFields().iterator(); iter.hasNext(); ) {
            Field f = (Field) iter.next();
            if (fieldsToShow.contains(f.name())) {
                item.addElement(QName.get(f.name(),hounderNs)).addText(DomUtil.filterXml(f.stringValue()));
            }
        }
        return item;
    }
}

