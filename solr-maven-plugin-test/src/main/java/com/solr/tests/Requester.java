package com.solr.tests;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;

public class Requester {

	private CloudSolrClient cloudSolrClient;

	public Requester() {
		this("default");
	}
	
	public Requester(String collection) {
		cloudSolrClient = new CloudSolrClient("localhost:8889/solr");
		cloudSolrClient.setDefaultCollection(collection);
	}

	public int ping() throws Exception {
		SolrPingResponse pingResponse = new SolrPing().process(cloudSolrClient);
		return pingResponse.getStatus();
	}
	
	public void addDoc() throws Exception {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1234");
		doc.addField("attr_name", "A lovely summer holiday");
		cloudSolrClient.add(doc);
		cloudSolrClient.commit();
		
	}
	
}
