package org.eclipse.ant.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;
import java.io.File;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;

/**
 * Ant task which replaces the standard Ant Javac task.  This version of the task
 * uses a special compiler adapter factory which in turn uses the Ant plug-in's
 * object class registry to find the class to use for compiler adapters.  This is required
 * because of the platform's classloading strategy.
 * <p>
 * This task can be used as a direct replacement for the original Javac task
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 */

public class EclipseJavac extends Javac {
	private static final String FAIL_MSG = "Compile failed, messages should have been provided.";
/**
 * Executes the task.
 */
public void execute() throws BuildException {
	// first off, make sure that we've got a srcdir

	Path src = getSrcdir();
	if (src == null) {
		throw new BuildException("srcdir attribute must be set!", location);
	}
	String[] list = src.list();
	if (list.length == 0) {
		throw new BuildException("srcdir attribute must be set!", location);
	}

	File destDir = getDestdir();
	if (destDir != null && !destDir.isDirectory()) {
		throw new BuildException("destination directory \"" + destDir + "\" does not exist or is not a directory", location);
	}

	// scan source directories and dest directory to build up 
	// compile lists
	resetFileLists();
	for (int i = 0; i < list.length; i++) {
		File srcDir = (File) project.resolveFile(list[i]);
		if (!srcDir.exists()) {
			throw new BuildException("srcdir \"" + srcDir.getPath() + "\" does not exist!", location);
		}

		DirectoryScanner ds = this.getDirectoryScanner(srcDir);

		String[] files = ds.getIncludedFiles();

		scanDir(srcDir, destDir != null ? destDir : srcDir, files);
	}

	// compile the source files

	String compiler = project.getProperty("build.compiler");
	if (compiler == null) {
		if (Project.getJavaVersion().startsWith("1.3")) {
			compiler = "modern";
		} else {
			compiler = "classic";
		}
	}

	if (compileList.length > 0) {

		CompilerAdapter adapter = EclipseCompilerAdapterFactory.getCompiler(compiler, this);
		log("Compiling " + compileList.length + " source file" + (compileList.length == 1 ? "" : "s") + (destDir != null ? " to " + destDir : ""));

		// now we need to populate the compiler adapter
		adapter.setJavac(this);

		// finally, lets execute the compiler!!
		if (!adapter.execute()) {
			if (failOnError) {
				throw new BuildException(FAIL_MSG, location);
			} else {
				log(FAIL_MSG, Project.MSG_ERR);
			}
		}
	}
}
}
