package io.github.borisnaguet.solr.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * 
 * @author BorisNaguet
 *
 */
@Mojo(name = "stop-solrcloud", defaultPhase = POST_INTEGRATION_TEST)
public class StopSolrCloudMojo extends AbstractSolrMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if(skip) {
			getLog().info("solr.skip=true - not stopping Solr");
			return;
		}
		
		SolrCloudManager solrCloudManager = (SolrCloudManager) session.getPluginContext(plugin, project).get(CLOUD_MANAGER_CXT);
		
		solrCloudManager.stopCluster(getLog());
	}

}
