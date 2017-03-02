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
import org.apache.solr.cloud.ZkTestServer.LimitViolationAction;
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

	private ZkTestServer zkTestServer;

	private String chroot;

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, String configName, String chroot) {
		this(dataDir, confDir, numServers, zkPort, DEFAULT_CLOUD_SOLR_XML, configName, chroot);
	}

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, File solrXmlFile, String configName, String chroot)
			throws MojoExecutionException {
		this(dataDir, confDir, numServers, zkPort, read(solrXmlFile), configName, chroot);
	}

	public SolrCloudManager(Path dataDir, Path confDir, int numServers, int zkPort, String solrXmlContent, String configName, String chroot) {
		this.dataDir = dataDir;
		this.confDir = confDir;
		this.numServers = numServers;
		this.zkPort = zkPort;
		this.solrXmlContent = solrXmlContent;
		this.configName = configName;
		this.chroot = chroot;
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
	 * @param log maven log
	 * @throws MojoExecutionException exception
	 */
	public synchronized void startCluster(Log log) throws MojoExecutionException {
		if (solrCloud != null) {
			throw new MojoExecutionException("Solr already started");
		}

		log.debug("About to startCluster");

		String zkDir = dataDir.resolve("zookeeper/server1/data").toString();
		zkTestServer = new ZkTestServer(zkDir, zkPort);
		//TODO: look more into why we need that
		zkTestServer.setViolationReportAction(LimitViolationAction.IGNORE);
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
			
			solrCloud = new MiniSolrCloudCluster(numServers, dataDir, solrXmlContent, jettyConfig, zkTestServer, chroot);
			log.debug("MiniSolrCloudCluster started");
		}
		catch (Exception e) {
			throw new MojoExecutionException("Can't start solr", e);
		}
	}

	/**
	 * Upload config to ZK
	 * 
	 * @param log maven log
	 * @throws MojoExecutionException exception
	 */
	public synchronized void uploadConfig(Log log) throws MojoExecutionException {
		try (SolrZkClient zkClient = new SolrZkClient(solrCloud.getZkServer().getZkAddress(chroot), TIMEOUT, TIMEOUT, null)) {
			ZkConfigManager manager = new ZkConfigManager(zkClient);
			if(manager.configExists(configName)) {
				throw new MojoExecutionException("Config " + configName + " already exists on ZK");
			}
			
			log.debug("about to upload config from " + confDir + " to " + configName);
			manager.uploadConfigDir(confDir, configName);
			log.debug("Config uploaded");
		}
		catch (IOException e) {
			throw new MojoExecutionException("Can't upload solr config in ZK " + configName, e);
		}
	}

	/**
	 * Creates a collection
	 * 
	 * @param log maven log
	 * @param colName collection name to create
	 * @throws MojoExecutionException Exception
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
	 * @param log maven log
	 * @throws MojoExecutionException exception
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
			log.error("Can't stop Solr, will try to stop zk before launching Exception");
			throw new MojoExecutionException("Can't stop solr", e);
		}
		finally {
			try {
				zkTestServer.shutdown();
			}
			catch (IOException | InterruptedException e) {
				throw new MojoExecutionException("Can't stop zookeeper", e);
			}
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
	 * Removes the dir
	 * 
	 * @param log maven log
	 * @param dir dir to clean
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
