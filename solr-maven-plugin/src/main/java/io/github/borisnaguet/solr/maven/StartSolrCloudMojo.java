package io.github.borisnaguet.solr.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;
import static org.apache.solr.cloud.MiniSolrCloudCluster.DEFAULT_CLOUD_SOLR_XML;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.cassandra.ConsoleScanner;

import io.github.borisnaguet.solr.maven.util.FileUtil;

/**
 * Starts a new SolrCloud instance
 * 
 * @author BorisNaguet
 *
 */
@Mojo(name = "start-solrcloud", defaultPhase = PRE_INTEGRATION_TEST)
public class StartSolrCloudMojo extends AbstractSolrMojo {
	
//	@Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
//	protected List<Artifact> pluginDependencies;
	
	@Parameter(defaultValue = "${project.build.directory}/solrcloud", readonly = true)
	private String defaultBaseDir;
	
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;
	
	@Parameter(property = "solr.base.dir")
	private String baseDir;
	
	@Parameter(property = "solr.zk.port", defaultValue = "9983")
	private int zkPort;
	
	@Parameter(property = "solr.num.servers", defaultValue = "1")
	private int numServers;
	
	@Parameter(property = "solr.upload.config", defaultValue = "true")
	private boolean uploadConfig;

	@Parameter(property = "solr.config.name", defaultValue = "solrcloud-config")
	private String configName;

	@Parameter(property = "solr.zk.chroot", defaultValue = "")
	private String chroot;

	@Parameter(property = "solr.conf.dir", defaultValue = "${project.build.directory}/solrcloud/conf")
	private String confToUploadDir;

	@Parameter(property = "solr.collections")
	private List<String> collectionsToCreate;
	
	@Parameter(property = "solr.create.collections", defaultValue = "true")
	private boolean createCols;
	
	@Parameter(property = "solr.num.shards", defaultValue = "1")
	private int numShards;
	
	@Parameter(property = "solr.repli.factor", defaultValue = "1")
	private int replicationFactor;
	
	@Parameter(property = "solr.keep.running", defaultValue = "false")
	private boolean keepRunning;
	
	protected boolean isKeepRunning() {
		return keepRunning;
	}
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if(skip) {
				getLog().info("solr.skip=true - not starting Solr");
				return;
			}
			
			//no defaultValue possible in Maven for Lists
			if(collectionsToCreate.isEmpty()) {
				collectionsToCreate.add("default");
			}
			
			// 1- Create the temp dir (for data)
			boolean isDefault = baseDir == null;
			Path dataDir = isDefault ? Paths.get(defaultBaseDir) : Paths.get(project.getBasedir().getAbsolutePath(), baseDir);
			try {
				Files.createDirectories(dataDir);
			}
			catch (IOException e) {
				throw new MojoExecutionException("Error while creating parent dirs from " + dataDir, e);
			}
			//If no baseDir is defined, we use the default, but we create a new temp dir for every start
			if(isDefault) {
				try {
					dataDir = Files.createTempDirectory(dataDir, "data-");
				}
				catch (IOException e) {
					throw new MojoExecutionException("Error while creating temp dir in " + dataDir, e);
				}
			}
			
			// 2- Init & start Solr cloud (with ZK)
			Path confDir = Paths.get(confToUploadDir);
			if(! confDir.isAbsolute()) {
				confDir = Paths.get(project.getBasedir().getAbsolutePath(), confToUploadDir);
			}
			//TODO: get solrXml from config (+ option to upload it?)
			String solrXml = uploadConfig ? DEFAULT_CLOUD_SOLR_XML : null;
			SolrCloudManager cloudManager = new SolrCloudManager(dataDir, confDir, numServers, zkPort, solrXml, configName, chroot);
			
			if(Files.notExists(dataDir) || FileUtil.isEmptyDir(dataDir)) {
				cloudManager.canDeleteDataDir();
			}
	
			cloudManager.startCluster(getLog());
			
			// 3- Upload some config files in ZK
			if(uploadConfig) {
				//if the dir is already there, we don't copy anything in it
				if(Files.notExists(confDir) || FileUtil.isEmptyDir(confDir)) {
					cloudManager.canDeleteConfDir();
					
					getLog().info("Will extract default conf to " + confDir);
					
					FileUtil.extractFileFromClasspath("conf/_rest_managed.json", confDir.resolve("_rest_managed.json"));
					FileUtil.extractFileFromClasspath("conf/currency.xml", confDir.resolve("currency.xml"));
					FileUtil.extractFileFromClasspath("conf/managed-schema", confDir.resolve("managed-schema"));
					FileUtil.extractFileFromClasspath("conf/protwords.txt", confDir.resolve("protwords.txt"));
					FileUtil.extractFileFromClasspath("conf/solrconfig.xml", confDir.resolve("solrconfig.xml"));
					FileUtil.extractFileFromClasspath("conf/stopwords.txt", confDir.resolve("stopwords.txt"));
					FileUtil.extractFileFromClasspath("conf/synonyms.txt", confDir.resolve("synonyms.txt"));
					FileUtil.extractFileFromClasspath("conf/lang/stopwords_en.txt", confDir.resolve("lang/stopwords_en.txt"));
				}
				
				getLog().info("Will upload conf from " + confDir + "to ZK config: " + configName );
				cloudManager.uploadConfig(getLog());
			}
	
			// 4- Create a collection 
			if(createCols) {
				for (String col : collectionsToCreate) {
					cloudManager.createCollection(getLog(), col, numShards, replicationFactor);
				}
			}
			
			// 5- set in MavenSession, to be used later by other Mojos (like StopSolrCloud)
			session.getPluginContext(plugin, project).put(CLOUD_MANAGER_CXT, cloudManager);
			
			if(isKeepRunning() && settings.getInteractiveMode()) {
				ConsoleScanner consoleScanner = new ConsoleScanner();
				consoleScanner.start();
				try {
					Thread.sleep(200);
					getLog().info("------------------------------------------------------------------");
					getLog().info("Hit ENTER on the console to stop Solr and continue the build.\n");
					consoleScanner.waitForFinished();
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		catch (Exception e) {
			getLog().error(e);
			throw e;
		}
	}
}
