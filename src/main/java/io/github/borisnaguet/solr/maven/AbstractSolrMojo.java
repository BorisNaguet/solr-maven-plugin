package io.github.borisnaguet.solr.maven;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

public abstract class AbstractSolrMojo extends AbstractMojo {
	
	protected static final String CLOUD_MANAGER_CXT = "cloudManager";

	@Parameter(property = "solr.skip", required = false)
	protected boolean skip;
	
	@Parameter(defaultValue = "${session}", readonly = true)
	protected MavenSession session;
	
	@Parameter( defaultValue = "${project}", readonly = true )
	protected MavenProject project;

    @Parameter( defaultValue = "${mojoExecution}", readonly = true )
    protected MojoExecution mojo;

    @Parameter( defaultValue = "${plugin}", readonly = true )
    protected PluginDescriptor plugin;

    @Parameter( defaultValue = "${settings}", readonly = true )
    protected Settings settings;

    @Parameter( defaultValue = "${project.basedir}", readonly = true )
    protected File basedir;

    @Parameter( defaultValue = "${project.build.directory}", readonly = true )
    protected File target;

}
