package io.github.borisnaguet.solr.maven;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Starts Solrcloud and blocks. This is to be used for command-line only
 * 
 * This is equivalent to "start-solrcloud -Dsolr.keep.running"
 * 
 * @author BorisNaguet
 *
 */
@Mojo(name = "run")
public class RunSolrCloudMojo extends StartSolrCloudMojo {
	@Override
	protected boolean isKeepRunning() {
		return true;
	}
}
