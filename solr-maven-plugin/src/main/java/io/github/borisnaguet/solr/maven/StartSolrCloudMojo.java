package io.github.borisnaguet.solr.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PRE_INTEGRATION_TEST;

import java.nio.file.Paths;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
	
	@Parameter(property = "solr.base.dir", required = true, defaultValue = "${project.build.directory}/solrcloud")
	private String baseDir;
	
	@Parameter(property = "solr.zk.port", required = true, defaultValue = "8889")
	protected int zkPort;
	
	@Parameter(property = "solr.num.servers", required = true, defaultValue = "1")
	private int numServers;
	
	@Parameter(property = "solr.config.name", required = true, defaultValue = "solrcloud-config")
	private String configName;
	
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(skip) {
			getLog().info("solr.skip=true - not starting Solr");
			return;
		}
		
//		try {
//			baseDir = Files.createTempDirectory(Paths.get(".",  "target"), "solrcloud");
//		}
//		catch (IOException e) {
//			throw new MojoExecutionException("Error while creating temp dir in 'target'", e);
//		}
		
		SolrCloudManager cloudManager = new SolrCloudManager(Paths.get(baseDir), numServers, zkPort);
		
		cloudManager.startCluster(getLog());
		
		session.getPluginContext(plugin, project).put(CLOUD_MANAGER_CXT, cloudManager);
		
	}
}
