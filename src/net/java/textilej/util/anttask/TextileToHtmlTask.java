package net.java.textilej.util.anttask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.util.XmlStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for converting textile to HTML format.
 * 
 * @author dgreen
 */
public class TextileToHtmlTask extends Task {
	private List<FileSet> filesets = new ArrayList<FileSet>();
	
	protected String htmlFilenameFormat = "$1.html";
	
	protected boolean overwrite = true;

	private List<Stylesheet> stylesheets = new ArrayList<Stylesheet>();
	
	protected File file;
	
	protected String title;
	
	private boolean useInlineCssStyles = true;
	private boolean suppressBuiltInCssStyles = false;
	
	@Override
	public void execute() throws BuildException {
		if (file == null && filesets.isEmpty()) {
			throw new BuildException("Please add one or more source filesets or specify @file");
		}
		if (file != null && !filesets.isEmpty()) {
			throw new BuildException("@file may not be specified if filesets are also specified");
		}
		if (file != null) {
			if (!file.exists()) {
				throw new BuildException(String.format("File cannot be found: %s",file));
			} else if (!file.isFile()) {
				throw new BuildException(String.format("Not a file: %s",file));
			} else if (!file.canRead()) {
				throw new BuildException(String.format("Cannot read file: %s",file));
			}
		}
		
		for (Stylesheet stylesheet: stylesheets) {
			if (stylesheet.url == null && stylesheet.file == null) {
				throw new BuildException("Must specify one of @file or @url on <stylesheet>");
			}
			if (stylesheet.url != null && stylesheet.file != null) {
				throw new BuildException("May only specify one of @file or @url on <stylesheet>");
			}
			if (stylesheet.file != null) {
				if (!stylesheet.file.exists()) {
					throw new BuildException("Stylesheet file does not exist: "+stylesheet.file);	
				}
				if (!stylesheet.file.isFile()) {
					throw new BuildException("Referenced stylesheet is not a file: "+stylesheet.file);	
				}
				if (!stylesheet.file.canRead()) {
					throw new BuildException("Cannot read stylesheet: "+stylesheet.file);	
				}
			}
		}
		
		for (FileSet fileset: filesets) {
			
			File filesetBaseDir = fileset.getDir(getProject());
            DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
            
            String[] files = ds.getIncludedFiles();
            if (files != null) {
	            File baseDir = ds.getBasedir();
	            for (String file: files) {
	            	File inputFile = new File(baseDir,file);
	            	try {
	            		processFile(filesetBaseDir,inputFile);
	            	} catch (BuildException e) {
	            		throw e;
	            	} catch (Exception e) {
	            		throw new BuildException(String.format("Cannot process file '%s': %s",inputFile,e.getMessage()),e);
	            	}
	            }
            }
		}
		if (file != null) {
			try {
        		processFile(file.getParentFile(),file);
        	} catch (BuildException e) {
        		throw e;
        	} catch (Exception e) {
        		throw new BuildException(String.format("Cannot process file '%s': %s",file,e.getMessage()),e);
        	}
		}
	}

	/**
	 * process the file
	 * 
	 * @param baseDir
	 * @param source
	 * 
	 * @return the textile markup, or null if the file was not written
	 * 
	 * @throws BuildException
	 */
	protected String processFile(final File baseDir,final File source) throws BuildException {
		
		log(String.format("Processing file '%s'",source),Project.MSG_VERBOSE);
		
		String textile = null;
		
		String name = source.getName();
		if (name.lastIndexOf('.') != -1) {
			name = name.substring(0,name.lastIndexOf('.'));
		}
		
		File htmlOutputFile = computeHtmlFile(source, name);
		if (!htmlOutputFile.exists() || overwrite || htmlOutputFile.lastModified() < source.lastModified()) {

			if (textile == null) {
				textile = readFully(source);
			}
			
			Writer writer;
			try {
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(htmlOutputFile)),"utf-8");
			} catch (Exception e) {
				throw new BuildException(String.format("Cannot write to file '%s': %s",htmlOutputFile,e.getMessage()),e);
			}
			try {
				HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer) {
					@Override
					protected XmlStreamWriter createXmlStreamWriter(Writer out) {
						return super.createFormattingXmlStreamWriter(out);
					}
				};
				builder.setUseInlineStyles(useInlineCssStyles);
				builder.setSuppressBuiltInStyles(suppressBuiltInCssStyles);
				for (Stylesheet stylesheet: stylesheets) {
					if (stylesheet.url != null) {
						builder.addCssStylesheet(stylesheet.url);
					} else {
						builder.addCssStylesheet(stylesheet.file);
					}
				}
				
				builder.setTitle(title==null?name:title);
				MarkupParser parser = new MarkupParser();
				parser.setDialect(new TextileDialect());
				parser.setBuilder(builder);
				builder.setEmitDtd(true);
				
				parser.parse(textile);
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
					throw new BuildException(String.format("Cannot write to file '%s': %s",htmlOutputFile,e.getMessage()),e);
				}
			}
		}
		return textile;
	}

	protected File computeHtmlFile(final File source, String name) {
		return new File(source.getParentFile(),htmlFilenameFormat.replace("$1", name));
	}
	
	protected String readFully(File inputFile) {
		StringWriter w = new StringWriter();
		try {
			Reader r = new InputStreamReader(new BufferedInputStream(new FileInputStream(inputFile)));
			try {
				int i;
				while ((i = r.read()) != -1) {
					w.write((char)i);
				}
			} finally {
				r.close();
			}
		} catch (IOException e) {
			throw new BuildException(String.format("Cannot read file '%s': %s",inputFile,e.getMessage()),e);
		}
		return w.toString();
	}
	
	/**
	 * @see #setHtmlFilenameFormat(String)
	 */
	public String getHtmlFilenameFormat() {
		return htmlFilenameFormat;
	}

	/**
	 * The format of the HTML output file.  Consists of a pattern where the
	 * '$1' is replaced with the filename of the input file.  Default value is
	 * <code>$1.html</code>
	 * 
	 * @param htmlFilenameFormat
	 */
	public void setHtmlFilenameFormat(String htmlFilenameFormat) {
		this.htmlFilenameFormat = htmlFilenameFormat;
	}

	/**
	 * The document title, as it appears in the head
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * The document title, as it appears in the head
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * the file to process
	 */
    public File getFile() {
		return file;
	}

    /**
     * the file to process
     */
	public void setFile(File file) {
		this.file = file;
	}

	/**
     * Adds a set of files to process.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }
    
	public void addStylesheet(Stylesheet stylesheet) {
		if (stylesheet == null) {
			throw new IllegalArgumentException();
		}
		stylesheets.add(stylesheet);
	}
	
	
	/**
	 * @see HtmlDocumentBuilder#isUseInlineStyles()
	 */
	public boolean isUseInlineCssStyles() {
		return useInlineCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isUseInlineStyles()
	 */
	public void setUseInlineCssStyles(boolean useInlineCssStyles) {
		this.useInlineCssStyles = useInlineCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isSuppressBuiltInStyles()
	 */
	public boolean isSuppressBuiltInCssStyles() {
		return suppressBuiltInCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isSuppressBuiltInStyles()
	 */
	public void setSuppressBuiltInCssStyles(boolean suppressBuiltInCssStyles) {
		this.suppressBuiltInCssStyles = suppressBuiltInCssStyles;
	}



	public static class Stylesheet {
		private File file;
		private String url;
		
		public File getFile() {
			return file;
		}
		public void setFile(File file) {
			this.file = file;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
	}
}
