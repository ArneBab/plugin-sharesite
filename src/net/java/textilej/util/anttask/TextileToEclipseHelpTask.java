package net.java.textilej.util.anttask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import net.java.textilej.parser.util.TextileToEclipseToc;

import org.apache.tools.ant.BuildException;

/**
 * An Ant task for converting textile to eclipse help format.
 * 
 * @author dgreen
 */
public class TextileToEclipseHelpTask extends TextileToHtmlTask {
	
	private String xmlFilenameFormat = "$1-toc.xml";
	
	
	protected String processFile(final File baseDir,final File source) throws BuildException {
		String textile = super.processFile(baseDir, source);
		
		String name = source.getName();
		if (name.lastIndexOf('.') != -1) {
			name = name.substring(0,name.lastIndexOf('.'));
		}
		
		File tocOutputFile = computeTocFile(source,name);
		if (!tocOutputFile.exists() || overwrite || tocOutputFile.lastModified() < source.lastModified()) {
			File htmlOutputFile = computeHtmlFile(source, name);
			if (textile == null) {
				textile = readFully(source);
			}
			
			Writer writer;
			try {
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(tocOutputFile)),"utf-8");
			} catch (Exception e) {
				throw new BuildException(String.format("Cannot write to file '%s': %s",tocOutputFile,e.getMessage()),e);
			}
			try {
				
				TextileToEclipseToc toEclipseToc = new TextileToEclipseToc();
				toEclipseToc.setBookTitle(title==null?name:title);
				
				String basePath = baseDir.getAbsolutePath().replace('\\','/');
				String outputFilePath = htmlOutputFile.getAbsolutePath().replace('\\','/');
				if (outputFilePath.startsWith(basePath)) {
					String filePath = outputFilePath.substring(basePath.length());
					if (filePath.startsWith("/")) {
						filePath = filePath.substring(1);
					}
					toEclipseToc.setHtmlFile(filePath);
				} else {
					toEclipseToc.setHtmlFile(htmlOutputFile.getName());
				}
				
				String tocXml = toEclipseToc.parse(textile);
				
				try {
					writer.write(tocXml);
				} catch (Exception e) {
					throw new BuildException(String.format("Cannot write to file '%s': %s",tocXml,e.getMessage()),e);
				}
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
					throw new BuildException(String.format("Cannot write to file '%s': %s",tocOutputFile,e.getMessage()),e);
				}
			}
		}
		return textile;
	}
	
	
	private File computeTocFile(File source, String name) {
		return new File(source.getParentFile(),xmlFilenameFormat.replace("$1", name));
	}


	/**
	 * @see #setXmlFilenameFormat(String)
	 */
	public String getXmlFilenameFormat() {
		return xmlFilenameFormat;
	}

	/**
	 * The format of the XML table of contents output file.  Consists of a pattern where the
	 * '$1' is replaced with the filename of the input file.  Default value is
	 * <code>$1-toc.xml</code>
	 */
	public void setXmlFilenameFormat(String xmlFilenameFormat) {
		this.xmlFilenameFormat = xmlFilenameFormat;
	}
}
