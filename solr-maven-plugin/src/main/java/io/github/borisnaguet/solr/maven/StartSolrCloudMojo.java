package io.github.borisnaguet.solr.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.github.borisnaguet.solr.maven.util.FileUtil;

/**
 * Starts a new SolrCloud instance
 * 
 * Can only be called once
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
	
	@Parameter(property = "solr.base.dir")
	private String baseDir;
	
	@Parameter(property = "solr.zk.port", defaultValue = "8889")
	private int zkPort;
	
	@Parameter(property = "solr.num.servers", defaultValue = "1")
	private int numServers;
	
	@Parameter(property = "solr.upload.config", defaultValue = "true")
	private boolean uploadConfig;

	@Parameter(property = "solr.config.name", defaultValue = "solrcloud-config")
	private String configName;
	
	@Parameter(property = "solr.conf.dir", defaultValue = "${project.build.directory}/solrcloud/conf")
	private String confToUploadDir;

	@Parameter(property = "solr.collections")
	private List<String> collectionsToCreate;
	
	@Parameter(property = "solr.create.collections", defaultValue = "true")
	private boolean createCols;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
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
		Path dirPath = Paths.get(isDefault ? defaultBaseDir : baseDir);
		try {
			Files.createDirectories(dirPath);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error while creating parent dirs from " + dirPath, e);
		}
		//If no baseDir is defined, we use the default, but we create a new temp dir for every start
		if(isDefault) {
			try {
				dirPath = Files.createTempDirectory(dirPath, "temp");
			}
			catch (IOException e) {
				throw new MojoExecutionException("Error while creating temp dir in " + dirPath, e);
			}
		}
		
		// 2- Init & start Solr cloud (with ZK)
		SolrCloudManager cloudManager = new SolrCloudManager(dirPath, numServers, zkPort, configName);
		cloudManager.startCluster(getLog());

		// 3- Upload some config files in ZK
		if(uploadConfig) {
			Path confDirPath = Paths.get(confToUploadDir);
			//if the dir is already there, we don't copy anything in it
			if(Files.notExists(confDirPath) || FileUtil.isEmptyDir(confDirPath)) {
				FileUtil.extractFileFromClasspath("conf/_rest_managed.json", confDirPath.resolve("_rest_managed.json"));
				FileUtil.extractFileFromClasspath("conf/currency.xml", confDirPath.resolve("currency.xml"));
				FileUtil.extractFileFromClasspath("conf/managed-schema", confDirPath.resolve("managed-schema"));
				FileUtil.extractFileFromClasspath("conf/protwords.txt", confDirPath.resolve("protwords.txt"));
				FileUtil.extractFileFromClasspath("conf/solrconfig.xml", confDirPath.resolve("solrconfig.xml"));
				FileUtil.extractFileFromClasspath("conf/stopwords.txt", confDirPath.resolve("stopwords.txt"));
				FileUtil.extractFileFromClasspath("conf/synonyms.txt", confDirPath.resolve("synonyms.txt"));
				FileUtil.extractFileFromClasspath("conf/lang/stopwords_en.txt", confDirPath.resolve("lang/stopwords_en.txt"));
			}
			
			cloudManager.uploadConfig(getLog(), confDirPath);
		}

		// 4- Create a collection 
		if(createCols) {
			for (String col : collectionsToCreate) {
				cloudManager.createCollection(getLog(), col);
			}
		}
		
		// 5- set in MavenSession, to be used later by other Mojos (like StopSolrCloud)
		session.getPluginContext(plugin, project).put(CLOUD_MANAGER_CXT, cloudManager);
	}
}
