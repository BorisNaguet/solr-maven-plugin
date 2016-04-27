package io.github.borisnaguet.solr.maven.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
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
	 * 
	 * @param file
	 * @return
	 * @throws MojoExecutionException
	 */
	public static String read(File file) throws MojoExecutionException {
		// read solr.xml content
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
	 */
	public static void delete(Log log, Path path) {
		if (log != null && path != null) {
			try {
				if (Files.isDirectory(path)) {
					Files.walkFileTree(path, new Deleter(log));
				}
				else if (Files.isWritable(path)) {
					Files.deleteIfExists(path);
				}
			}
			catch (IOException e) {
				log.warn("Can't delete " + path.toAbsolutePath() + " - "+ e.getMessage() + " (will try on exit)");
				log.debug(e);
				path.toFile().deleteOnExit();
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
			try {
				Files.deleteIfExists(dir);
			}
			catch(IOException e) {
				dir.toFile().deleteOnExit();
				throw e;
			}
			return super.postVisitDirectory(dir, exc);
		}

		@Override
		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
			log.debug("Will delete file " + path);
			try {
				Files.delete(path);
			}
			catch(IOException e) {
				path.toFile().deleteOnExit();
				throw e;
			}
			return FileVisitResult.CONTINUE;
		}
	}

	public static void extractFileFromClasspath(String sourceFile, Path destFile) throws MojoExecutionException {
		extractFileFromClasspath(sourceFile, destFile, false);
	}
	
	public static void extractFileFromClasspath(String sourceFile, Path destFile, boolean deleteOnExit) throws MojoExecutionException {
		if(Files.exists(destFile)) {
			throw new MojoExecutionException("Won't overwrite file " + destFile);
		}
		
		try {
			Path parentDir = destFile.getParent();
			if(Files.notExists(parentDir)) {
				Files.createDirectories(parentDir);
				if(deleteOnExit) {
					parentDir.toFile().deleteOnExit();
				}
			}
			
			InputStream in = FileUtil.class.getClassLoader().getResourceAsStream(sourceFile);
			Files.copy(in, destFile);
			if(deleteOnExit) {
				destFile.toFile().deleteOnExit();
			}
		}
		catch (IOException e) {
			throw new MojoExecutionException("Can't extract " + sourceFile + " to " + destFile, e);
		}
	}
	
	public static boolean isEmptyDir(Path dir) {
		if (dir != null && Files.isDirectory(dir) && Files.isReadable(dir)) {
			 try(DirectoryStream<Path> ds = Files.newDirectoryStream(dir);) {
			     return ! ds.iterator().hasNext();
			 }
			catch (IOException e) {
				//for what we do here, that's fine
				return false;
			}
		}
		return false;
	}
}
