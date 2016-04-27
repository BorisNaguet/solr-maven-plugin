package io.github.borisnaguet.solr.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 
 * @author BorisNaguet
 *
 */
@Mojo(name = "stop-solrcloud", defaultPhase = POST_INTEGRATION_TEST)
public class StopSolrCloudMojo extends AbstractSolrMojo {

	@Parameter(property = "solr.delete.conf", defaultValue = "false")
	private boolean deleteConf;
	
	@Parameter(property = "solr.delete.data", defaultValue = "false")
	private boolean deleteData;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(skip) {
			getLog().info("solr.skip=true - not stopping Solr");
			return;
		}
		
		SolrCloudManager solrCloudManager = (SolrCloudManager) session.getPluginContext(plugin, project).get(CLOUD_MANAGER_CXT);
		
		solrCloudManager.stopCluster(getLog());
		
		if(deleteConf) {
			solrCloudManager.cleanConfDir(getLog());
		}
		if(deleteData) {
			solrCloudManager.cleanDataDir(getLog());
		}
	}

}
