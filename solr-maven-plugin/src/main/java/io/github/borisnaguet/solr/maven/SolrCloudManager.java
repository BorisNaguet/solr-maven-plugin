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

	private final Path baseDir;
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

	public SolrCloudManager(Path baseDir, int numServers, int zkPort, String configName) {
		this(baseDir, numServers, zkPort, DEFAULT_CLOUD_SOLR_XML, configName);
	}

	public SolrCloudManager(Path baseDir, int numServers, int zkPort, File solrXmlFile, String configName)
			throws MojoExecutionException {
		this(baseDir, numServers, zkPort, read(solrXmlFile), configName);
	}

	public SolrCloudManager(Path baseDir, int numServers, int zkPort, String solrXmlContent, String configName) {
		this.baseDir = baseDir;
		this.numServers = numServers;
		this.zkPort = zkPort;
		// TODO: don't keep it in memory?
		this.solrXmlContent = solrXmlContent;
		this.configName = configName;
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

		// just in case it hasn't been cleaned previously
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

		// Start Solr Cluster
		try {
			log.debug("Will start MiniSolrCloudCluster");
			solrCloud = new MiniSolrCloudCluster(numServers, baseDir, solrXmlContent, JettyConfig.builder().build(), zkTestServer);
			log.debug("MiniSolrCloudCluster started");
		}
		catch (Exception e) {
			clean(log);
			throw new MojoExecutionException("Can't start solr", e);
		}
	}

	/**
	 * Upload config to ZK
	 * 
	 * @param log
	 * @throws MojoExecutionException
	 */
	public synchronized void uploadConfig(Log log, Path configDir) throws MojoExecutionException {
		try (SolrZkClient zkClient = new SolrZkClient(solrCloud.getZkServer().getZkAddress(), TIMEOUT, TIMEOUT, null)) {
			ZkConfigManager manager = new ZkConfigManager(zkClient);
			if(manager.configExists(configName)) {
				//TODO: optional (keep the existing one)?
				throw new MojoExecutionException("Config " + configName + " already exists on ZK");
			}

			manager.uploadConfigDir(configDir, configName);
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
		finally {
			try {
				Thread.sleep(2_000);
			}
			catch (InterruptedException e) {
			}
			clean(log);
		}
	}

	/**
	 * Removes the {@link #baseDir}
	 * 
	 * @param log
	 */
	protected synchronized void clean(Log log) {
		log.debug("About to clean");
		if (baseDir != null && Files.exists(baseDir)) {
			log.debug("Will clean " + baseDir);
			FileUtil.delete(log, baseDir);
			log.debug(baseDir + " deleted");
		}
	}
}
