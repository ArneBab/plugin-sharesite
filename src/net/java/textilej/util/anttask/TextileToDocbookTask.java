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
import net.java.textilej.parser.builder.DocBookDocumentBuilder;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.util.XmlStreamWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * An Ant task for converting textile to docbook format.
 * 
 * @author dgreen
 */
public class TextileToDocbookTask extends Task {
	private List<FileSet> filesets = new ArrayList<FileSet>();
	
	private String docbookFilenameFormat = "$1.xml";

	private String bookTitle;
	
	private boolean overwrite = true;

	protected File file;

	private String doctype;
	
	

	/**
     * Adds a set of files to process.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

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

	private void processFile(final File baseDir,final File source) throws BuildException {
		
		log(String.format("Processing file '%s'",source),Project.MSG_VERBOSE);
		
		String textile = null;
		
		String name = source.getName();
		if (name.lastIndexOf('.') != -1) {
			name = name.substring(0,name.lastIndexOf('.'));
		}
		
		File docbookOutputFile = new File(source.getParentFile(),docbookFilenameFormat.replace("$1", name));
		if (!docbookOutputFile.exists() || overwrite || docbookOutputFile.lastModified() < source.lastModified()) {

			if (textile == null) {
				textile = readFully(source);
			}
			
			Writer writer;
			try {
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(docbookOutputFile)),"utf-8");
			} catch (Exception e) {
				throw new BuildException(String.format("Cannot write to file '%s': %s",docbookOutputFile,e.getMessage()),e);
			}
			try {
				DocBookDocumentBuilder builder = new DocBookDocumentBuilder(writer) {
					@Override
					protected XmlStreamWriter createXmlStreamWriter(Writer out) {
						return super.createFormattingXmlStreamWriter(out);
					}
				};
				MarkupParser parser = new MarkupParser();
				parser.setDialect(new TextileDialect());
				parser.setBuilder(builder);
				builder.setBookTitle(bookTitle==null?name:bookTitle);
				if (doctype != null) {
					builder.setDoctype(doctype);
				}
				parser.parse(textile);
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
					throw new BuildException(String.format("Cannot write to file '%s': %s",docbookOutputFile,e.getMessage()),e);
				}
			}
		}

	}
	
	private String readFully(File inputFile) {
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
	 * @see #setDocbookFilenameFormat(String)
	 */
	public String getDocbookFilenameFormat() {
		return docbookFilenameFormat;
	}

	/**
	 * The format of the DocBook output file.  Consists of a pattern where the
	 * '$1' is replaced with the filename of the input file.  Default value is
	 * <code>$1.xml</code>
	 * 
	 * @param docbookFilenameFormat
	 */
	public void setHtmlFilenameFormat(String docbookFilenameFormat) {
		this.docbookFilenameFormat = docbookFilenameFormat;
	}

	/**
	 * Get the book title.
	 * 
	 * @return the title, or null if the textile filename is to be used as the title.
	 */
	public String getBookTitle() {
		return bookTitle;
	}

	/**
	 * 
	 * Get the book title.
	 * 
	 * @param bookTitle the title, or null if the textile filename is to be used as the title.
	 */
	public void setBookTitle(String bookTitle) {
		this.bookTitle = bookTitle;
	}

	/**
	 * Set the XML doctype of the docbook.  The doctype should look something like this:
	 * <code>&lt;!DOCTYPE book PUBLIC "-//OASIS//DTD DocBook XML V4.5//EN" "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd"&gt;</code>
	 * 
	 */
    public void setDoctype(String doctype) {
		this.doctype = doctype;
	}

	/**
	 * The XML doctype of the docbook.
	 */
	public String getDoctype() {
		return doctype;
	}
}
