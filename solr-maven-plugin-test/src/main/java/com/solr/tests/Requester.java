package com.solr.tests;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;

public class Requester {

	private CloudSolrClient cloudSolrClient;

	public Requester() {
		this("default", 9983, "/");
	}
	
	public Requester(String collection, int port, String context) {
		cloudSolrClient = new CloudSolrClient("localhost:" + port + context);
		cloudSolrClient.setDefaultCollection(collection);
	}

	public int ping() throws Exception {
		SolrPingResponse pingResponse = new SolrPing().process(cloudSolrClient);
		return pingResponse.getStatus();
	}
	
	public void addDoc_withAttrName() throws Exception {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1234");
		doc.addField("attr_name", "The doc name as attr_name");
		cloudSolrClient.add(doc);
		cloudSolrClient.commit();
	}
	
	public void addDoc_WithName() throws Exception {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", "1234");
		doc.addField("name", "The doc name");
		cloudSolrClient.add(doc);
		cloudSolrClient.commit();
	}
}
