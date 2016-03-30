package io.github.borisnaguet.solr.maven;

import static org.apache.solr.cloud.MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.cloud.ZkTestServer;

import com.google.common.base.Charsets;

import io.github.borisnaguet.solr.maven.util.FileUtil;

public class SolrCloudManager {
	private final Path baseDir;
	private final int numServers;
	private final int zkPort;
	private final String solrXmlContent;
	
	private MiniSolrCloudCluster solrCloud;
	
	public SolrCloudManager(Path baseDir, int numServers, int zkPort) {
		this(baseDir, numServers, zkPort, DEFAULT_CLOUD_SOLR_XML);
	}

	public SolrCloudManager(Path baseDir, int numServers, int zkPort, File solrXmlFile) throws MojoExecutionException {
		this(baseDir, numServers, zkPort, read(solrXmlFile));
	}
	
	public SolrCloudManager(Path baseDir, int numServers, int zkPort, String solrXmlContent) {
		this.baseDir = baseDir;
		this.numServers = numServers;
		this.zkPort = zkPort;
		//TODO: don't keep it in memory?
		this.solrXmlContent = solrXmlContent;
	}
	
	private static String read(File solrXmlFile) throws MojoExecutionException {
		//read solr.xml content
		try {
			return com.google.common.io.Files.toString(solrXmlFile, Charsets.UTF_8);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Can't read " + solrXmlFile.getAbsolutePath(), e);
		}
	}
	
	public synchronized void startCluster(Log log) throws MojoExecutionException {
		if(solrCloud != null) {
			throw new MojoExecutionException("Solr already started");
		}
		
		log.debug("About to startCluster");
		
		//just in case it hasn't been cleaned previously
		clean(log);
		
		String zkDir = baseDir.resolve("zookeeper/server1/data").toString();
		ZkTestServer zkTestServer = new ZkTestServer(zkDir, zkPort);
		try {
			log.debug("Will start ZkTestServer");
			zkTestServer.run();
			log.debug("ZkTestServer started");
		}
		catch (InterruptedException e) {
			clean(log);
			throw new MojoExecutionException("Can't start ZooKeeper test server", e);
		}
		
		//Start Solr Cluster
		try {
			log.debug("Will start MiniSolrCloudCluster");
			solrCloud = new MiniSolrCloudCluster(numServers, baseDir, solrXmlContent, JettyConfig.builder().build(), zkTestServer);
			log.debug("MiniSolrCloudCluster started");
		}
		catch (Exception e) {
			clean(log);
			throw new MojoExecutionException("Can't start solr", e);
		}
		
		
		//Upload some config in ZK to be used on collection creation
//		solrCloud.uploadConfigDir(new File("solrcloud/conf"), configName);
	}
	
	public synchronized void createCollection(Log log) {
		log.debug("About to createCollection");
		
//		String colName = "col_mavenp_plugin" ;
//		solrCloud.createCollection(colName, 1, 1, configName, null);
	}
	
	public synchronized void stopCluster(Log log) throws MojoExecutionException {
		log.debug("About to stopCluster");
		try {
			if(solrCloud != null) {
				log.debug("Will shutdown");
				solrCloud.shutdown();
				log.debug("Shutdown done");
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Can't stop solr", e);
		}
		finally {
			clean(log);
		}
	}
	
	public synchronized void clean(Log log) {
		log.debug("About to clean");
		if(baseDir != null && Files.exists(baseDir)) {
			try {
				log.debug("Will clean " + baseDir);
				FileUtil.delete(log, baseDir);
				log.debug(baseDir + " deleted");
			}
			catch (IOException e) {
				log.warn("Error while cleaning", e);
			}
		}
	}
}
