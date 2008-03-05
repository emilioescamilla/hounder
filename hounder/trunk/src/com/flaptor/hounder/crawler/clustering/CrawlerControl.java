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
package com.flaptor.hounder.crawler.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.flaptor.clustering.AbstractModule;
import com.flaptor.clustering.ModuleNodeDescriptor;
import com.flaptor.clustering.NodeDescriptor;
import com.flaptor.clustering.NodeUnreachableException;
import com.flaptor.clustering.WebModule;
import com.flaptor.util.remote.WebServer;
import com.flaptor.util.remote.XmlrpcClient;

/**
 * Clusterfest Module for controlling crawlers
 * 
 * @author Martin Massera
 */
public class CrawlerControl extends AbstractModule<CrawlerControlNode> implements WebModule {

	@Override
	protected CrawlerControlNode createModuleNode(NodeDescriptor node) {
		return new CrawlerControlNode(node);
	}

	@Override
	public boolean shouldRegister(NodeDescriptor node) throws NodeUnreachableException {
		try {
			boolean ret = getCrawlerControllableProxy(node.getXmlrpcClient()).ping();
			return ret;
		} catch (NoSuchMethodException e) {
			return false;
		} catch (Exception e) {
			throw new NodeUnreachableException(e);
		}
	}
    @Override
    protected void notifyModuleNode(CrawlerControlNode node) {
        //do nothing
    }
	
	public String getBoostConfig(CrawlerControlNode node) {
		try {
			return node.getBoostConfig();
		} catch (NodeUnreachableException e) {
			return null;
		}
	}
	
	/**
	 * @param xmlrpcClient
	 * @return a proxy of crawlerControllable that uses that xmlrpcClient
	 */
	public static CrawlerControllable getCrawlerControllableProxy(XmlrpcClient xmlrpcClient) {
		return (CrawlerControllable)XmlrpcClient.proxy("crawlerControl", CrawlerControllable.class, xmlrpcClient);	
	}
	public String action(String action, HttpServletRequest request) {
		return null;
	}
	public List<String> getActions() {
		return new ArrayList<String>();
	}
	public String getModuleHTML() {
		return null;
	}
	public String getNodeHTML(NodeDescriptor node, int nodeNum) {
		if (isRegistered(node)) return "<a href=\"crawlerControl.jsp?node="+nodeNum+"\">crawler</a>";
		else return null;
	}
	public void setup(WebServer server) {
	}
}
