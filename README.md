# solr-maven-plugin

[![Build Status](https://travis-ci.org/BorisNaguet/solr-maven-plugin.svg?branch=master)](https://travis-ci.org/BorisNaguet/solr-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.borisnaguet/solr-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.borisnaguet/solr-maven-plugin)

A maven plugin to start/stop Apache Solr Cloud

## Install
Releases available on maven Central
```xml
<plugin>
    <groupId>io.github.borisnaguet</groupId>
    <artifactId>solr-maven-plugin</artifactId>
    <version>0.3.0</version>
</plugin>
```

Snapshots (pushed automatically from Travis, on each push) available on Sonatype repository:
```xml
<pluginRepositories>
	<pluginRepository>
		<id>ossrh</id>
		<url>https://oss.sonatype.org/content/repositories/snapshots/</url>
		<releases>
			<enabled>false</enabled>
		</releases>
		<snapshots>
			<enabled>true</enabled>
		</snapshots>
	</pluginRepository>
</pluginRepositories>
```
```xml
<plugin>
    <groupId>io.github.borisnaguet</groupId>
    <artifactId>solr-maven-plugin</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</plugin>
```

## Use
Please, see `solr-maven-plugin-test` sub-project for a full example:
* running Solr cloud with default configuration for before `tests`
* stopping after `tests`
* starting another configuration (with lots of custom setups) of Solr in `pre-integration-test` (note that **you must enable IT** in the example to see that `<skipITs>false</skipITs>`)
* stopping it in `post-integration-tests`

```xml
	<plugin>
		<groupId>io.github.borisnaguet</groupId>
		<artifactId>solr-maven-plugin</artifactId>
		<version>${project.version}</version>
		<executions>
			<!-- Unit tests: tests default configuration parameters -->
			<execution>
				<id>start-tests</id>
				<phase>process-test-resources</phase>
				<goals>
					<goal>start-solrcloud</goal>
				</goals>
				<configuration>
					<skip>${skipTests}</skip>
				</configuration>
			</execution>
			<execution>
				<id>stop-tests</id>
				<phase>prepare-package</phase>
				<goals>
					<goal>stop-solrcloud</goal>
				</goals>
				<configuration>
					<skip>${skipTests}</skip>
					<deleteConf>true</deleteConf>
					<deleteData>true</deleteData>
				</configuration>
			</execution>
			
			<!-- INTEGRATION tests: use of all config -->
			<execution>
				<id>start-IT</id>
				<!-- default phase is pre-integration-test -->
				<goals>
					<goal>start-solrcloud</goal>
				</goals>
				<configuration>
					<skip>${skipITs}</skip>
					<zkPort>9984</zkPort>
					
					<uploadConfig>false</uploadConfig>
					<confToUploadDir>solr-it-conf</confToUploadDir>
					
					<createCols>false</createCols>
					<!-- collections that were previously created in baseDir -->
					<collectionsToCreate>
						<collectionsToCreate>col1</collectionsToCreate>
						<collectionsToCreate>testCol</collectionsToCreate>
					</collectionsToCreate>
					<baseDir>solr-it-data</baseDir>
					<chroot>/solr</chroot>
					<numServers>2</numServers>
				</configuration>
			</execution>
			<execution>
				<id>stop-IT</id>
				<!-- default phase is post-integration-test -->
				<goals>
					<goal>stop-solrcloud</goal>
				</goals>
				<configuration>
					<skip>${skipITs}</skip>
				</configuration>
			</execution>
		</executions>
</plugin> 
```

All goals and parameters are available on [the maven site](https://borisnaguet.github.io/solr-maven-plugin/plugin-info.html) (with default values and likecycle phases).

### Add extra jars
In Solr, you may need to add jars to the classpath of the server, and reference some features in the `solrconfig.xml` (for example).

To do that, add that to the plugin definition (just after its groupid:artifactid:version):

```xml
<dependencies>
	<dependency>
		<groupId>org.apache.solr</groupId>
		<artifactId>solr-analysis-extras</artifactId>
		<version>5.5.1</version>
	</dependency>
	<dependency>
		<groupId>org.apache.solr</groupId>
		<artifactId>solr-dataimporthandler</artifactId>
		<version>5.5.1</version>
	</dependency>
</dependencies>
```

Now, you can reference it in solrconfig.xml:
```xml
<!-- lib import needed for standard deployment (not by plugin) -->
<lib dir="${solr.install.dir:..}/dist/" regex="solr-dataimporthandler-\d.*\.jar" />

<requestHandler name="/dataimport" class="org.apache.solr.handler.dataimport.DataImportHandler">
    <lst name="defaults">
      <str name="config">data-config.xml</str>
    </lst>
</requestHandler>
```

### Standalone start
We've seen how to start & stop Solr with your maven build (using default phase or not).
But what if you want to start solr using the same version & config and run tests from Eclipse/IntelliJ and/or execute queries manually against it after/before tests.
Since version 0.3.0 you can start it with a command line:

```
mvn -Dsolr.keep.running=true io.github.borisnaguet:solr-maven-plugin:start-solrcloud
```

If you feel it's a bit long, you can use [this trick](https://maven.apache.org/settings.html#Plugin_Groups) (but maybe I should rename things to be even shorter).

The previous command will only use the configuration at the plugin level - not inside executions. If you configured the plugin inside an **execution**, you can specify its **id** with maven:

```
mvn -Dsolr.keep.running=true io.github.borisnaguet:solr-maven-plugin:start-solrcloud@start-IT
```

After a while you'll see that:
```
[INFO] ------------------------------------------------------------------
[INFO] Hit ENTER on the console to stop Cassandra and continue the build.
```
So it's better to clean stop with **Enter** instead of kill.

## To improve
This plugin is already used in a large professional project, but it could still be improved.
The main missing feature, is the ability to pick a Solr version: for the moment it's hard-coded in plugin.

## Build (for maven plugin developpers)
If you want to fork this project and make changes to the plugin, you'll only need to use `maven install`

## Contribute
Yes!

Please fill an issue, or a PR.
https://github.com/BorisNaguet/solr-maven-plugin/issues
