package io.github.borisnaguet.solr.maven;

import static io.github.borisnaguet.solr.maven.util.FileUtil.read;
import static org.apache.solr.cloud.AbstractZkTestCase.TIMEOUT;
import static org.apache.solr.cloud.MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.cloud.ZkTestServer;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.cloud.ZkConfigManager;

import io.github.borisnaguet.solr.maven.util.FileUtil;

/**
 * Wrapper around SolrCloudManager to manage operation on the cloud, the maven way (with {@link Log}
 * , and returning {@link MojoExecutionException}...)
 * 
 * <p>
 * Most methods takes a {@link Log} as parameter, as strange as it seems, because we must get it
 * from {@link AbstractMojo#getLog()}
 * and it must not be cached on construction (this manager being set in {@link MavenSession} and
 * reused from different {@link Mojo}s).
 * </p>
 * 
 * @author BorisNaguet
 *
 */
public class SolrCloudManager {
	private final String configName;

	private final Path dataDir;
	private boolean canDeleteDataDir = false;
	
	private final Path confDir;
	private boolean canDeleteConfDir = false;
	
	/**
	 * number of Solr servers that will be started
	 */
	private final int numServers;
	/**
	 * Zookeeper port
	 */
	private final int zkPort;
	/**
	 * content of the solr.xml file that will be uploaded to Zookeeper
	 */
	private final String solrXmlContent;

	private MiniSolrCloudCluster solrCloud;

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, String configName) {
		this(dataDir, confDir, numServers, zkPort, DEFAULT_CLOUD_SOLR_XML, configName);
	}

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, File solrXmlFile, String configName)
			throws MojoExecutionException {
		this(dataDir, confDir, numServers, zkPort, read(solrXmlFile), configName);
	}

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, String solrXmlContent, String configName) {
		this.dataDir = dataDir;
		this.confDir = confDir;
		this.numServers = numServers;
		this.zkPort = zkPort;
		this.solrXmlContent = solrXmlContent;
		this.configName = configName;
	}

	public void canDeleteDataDir() {
		this.canDeleteDataDir = true;
	}

	public void canDeleteConfDir() {
		this.canDeleteConfDir = true;
	}
	
	/**
	 * Start the {@link MiniSolrCloudCluster}
	 * 
	 * @param log
	 * @throws MojoExecutionException
	 */
	public synchronized void startCluster(Log log) throws MojoExecutionException {
		if (solrCloud != null) {
			throw new MojoExecutionException("Solr already started");
		}

		log.debug("About to startCluster");

		String zkDir = dataDir.resolve("zookeeper/server1/data").toString();
		ZkTestServer zkTestServer = new ZkTestServer(zkDir, zkPort);
		try {
			log.debug("Will start ZkTestServer");
			zkTestServer.run();
			log.debug("ZkTestServer started");
		}
		catch (InterruptedException e) {
			throw new MojoExecutionException("Can't start ZooKeeper test server", e);
		}

		// Start Solr Cluster
		try {
			log.debug("Will start MiniSolrCloudCluster");
			
			JettyConfig jettyConfig = JettyConfig.builder()
					.stopAtShutdown(false)
					.build();
			
			solrCloud = new MiniSolrCloudCluster(numServers, dataDir, solrXmlContent, jettyConfig, zkTestServer);
			log.debug("MiniSolrCloudCluster started");
		}
		catch (Exception e) {
			throw new MojoExecutionException("Can't start solr", e);
		}
	}

	/**
	 * Upload config to ZK
	 * 
	 * @param log
	 * @throws MojoExecutionException
	 */
	public synchronized void uploadConfig(Log log) throws MojoExecutionException {
		try (SolrZkClient zkClient = new SolrZkClient(solrCloud.getZkServer().getZkAddress(), TIMEOUT, TIMEOUT, null)) {
			ZkConfigManager manager = new ZkConfigManager(zkClient);
			if(manager.configExists(configName)) {
				throw new MojoExecutionException("Config " + configName + " already exists on ZK");
			}

			manager.uploadConfigDir(confDir, configName);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Can't upload solr config in ZK " + configName, e);
		}
	}

	/**
	 * 
	 * 
	 * @param log
	 * @throws MojoExecutionException
	 */
	public synchronized void createCollection(Log log, String colName) throws MojoExecutionException {
		log.debug("About to create collection " + colName);

		try {
			solrCloud.createCollection(colName, 1, 1, configName, null);
		}
		catch (SolrServerException | IOException e) {
			throw new MojoExecutionException("Can't create solr collection " + colName, e);
		}
		log.debug("Collection " + colName + " created");
	}

	/**
	 * Stop the cluster and clean
	 * 
	 * @param log
	 * @throws MojoExecutionException
	 */
	public synchronized void stopCluster(Log log) throws MojoExecutionException {
		log.debug("About to stopCluster");
		try {
			if (solrCloud != null) {
				log.debug("Will shutdown");
				solrCloud.shutdown();
				log.debug("Shutdown done");
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Can't stop solr", e);
		}
	}

	public synchronized void cleanDataDir(Log log) {
		if(canDeleteDataDir) {
			clean(log, dataDir);
		}
		else {
			log.warn("Can't delete data dir: " + dataDir + " - content was already existing");
		}
	}
	
	public synchronized void cleanConfDir(Log log) {
		if(canDeleteConfDir) {
			clean(log, confDir);
		}
		else {
			log.warn("Can't delete conf dir: " + confDir + " - content was already existing");
		}
	}
	
	/**
	 * Removes the {@link #baseDir}
	 * 
	 * @param log
	 */
	protected synchronized void clean(Log log, Path dir) {
		log.debug("About to clean");
		if (dir != null && Files.exists(dir)) {
			log.debug("Will clean " + dir);
			FileUtil.delete(log, dir);
			log.debug(dir + " deleted");
		}
	}
}
