package io.github.borisnaguet.solr.maven.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.base.Charsets;

/**
 * Utility class to manage files
 * 
 * @author BorisNaguet
 *
 */
public class FileUtil {
	private FileUtil() {
	}
	
	/**
	 * Fully read into memory a File
	 * @param file
	 * @return
	 * @throws MojoExecutionException
	 */
	public static String read(File file) throws MojoExecutionException {
		//read solr.xml content
		try {
			return com.google.common.io.Files.toString(file, Charsets.UTF_8);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Can't read " + file.getAbsolutePath(), e);
		}
	}
	
	/**
	 * Deletes a file or a directory
	 * 
	 * If the directory is not empty, it'll delete all files and sub-dirs before
	 * 
	 * @param log Maven logger to use
	 * @param path
	 * @throws IOException
	 */
	public static void delete(Log log, Path path) throws IOException {
		if(log != null && path != null) {
			try{
				if(Files.isDirectory(path)) {
					Files.walkFileTree(path, new Deleter(log));
				}
				else if (Files.isWritable(path)) {
					Files.deleteIfExists(path);
				}
			}
			catch(IOException e) {
				log.warn("ERROR: can't delete " + path + " - " + path.toAbsolutePath(), e);
			}
		}
	}
	
	private static class Deleter extends SimpleFileVisitor<Path> {
		private final Log log;
		public Deleter(Log log) {
			this.log = log;
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			log.debug("Will delete dir " + dir);
			Files.deleteIfExists(dir);
			return super.postVisitDirectory(dir, exc);
		}
		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
			log.debug("Will delete file " + path);
			Files.delete(path);
			return FileVisitResult.CONTINUE;
		}
	}
}
