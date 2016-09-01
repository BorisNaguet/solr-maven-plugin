# solr-maven-plugin

[![Build Status](https://travis-ci.org/BorisNaguet/solr-maven-plugin.svg?branch=master)](https://travis-ci.org/BorisNaguet/solr-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.borisnaguet/solr-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.borisnaguet/solr-maven-plugin)

A maven plugin to start/stop Apache Solr Cloud 5

## Install
Releases available on maven Central
```xml
<plugin>
    <groupId>io.github.borisnaguet</groupId>
    <artifactId>solr-maven-plugin</artifactId>
    <version>0.2.0</version>
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
Please, see test project for the moment.

## Build
Well, maven install ?

## Contribute
Yes!

Please fill an issue, or a PR.
https://github.com/BorisNaguet/solr-maven-plugin/issues
