/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package plugins.ShareWiki.mylyn.internal.wikitext.core.parser.builder;

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import plugins.ShareWiki.mylyn.wikitext.core.parser.Attributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.ImageAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.LinkAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.builder.AbstractXmlDocumentBuilder;
import plugins.ShareWiki.mylyn.wikitext.core.parser.builder.DitaBookMapDocumentBuilder;
import plugins.ShareWiki.mylyn.wikitext.core.parser.builder.DocBookDocumentBuilder;
import plugins.ShareWiki.mylyn.wikitext.core.parser.outline.OutlineItem;
import plugins.ShareWiki.mylyn.wikitext.core.util.FormattingXMLStreamWriter;
import plugins.ShareWiki.mylyn.wikitext.core.util.XmlStreamWriter;

/**
 * A document builder that creates an OASIS DITA topic
 * 
 * @author David Green
 * @see DitaBookMapDocumentBuilder
 */
public class DitaTopicDocumentBuilder extends AbstractXmlDocumentBuilder {

	private static final String __TOPIC = "__topic"; //$NON-NLS-1$

	private static Set<Integer> entityReferenceToUnicode = new HashSet<Integer>();
	static {
		entityReferenceToUnicode.add(215);
		entityReferenceToUnicode.add(8211);
		entityReferenceToUnicode.add(8212);
		entityReferenceToUnicode.add(8220);
		entityReferenceToUnicode.add(8221);
		entityReferenceToUnicode.add(8216);
		entityReferenceToUnicode.add(8217);
	}

	private final Stack<BlockDescription> blockDescriptions = new Stack<BlockDescription>();

	private String doctype = "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA 1.1 Topic//EN\" \"http://docs.oasis-open.org/dita/v1.1/OS/dtd/topic.dtd\">"; //$NON-NLS-1$

	private static class TopicInfo {
		int headingLevel;

		int openElements;
	}

	private final Stack<TopicInfo> topicInfos = new Stack<TopicInfo>();

	private OutlineItem outline;

	private String filename;

	private int topicBreakLevel = Integer.MAX_VALUE;

	private String rootTitle;

	/**
	 * Create a DitaTopicDocumentBuilder that writes formatted output to the given writer. Output without formatting can
	 * be created using {@link #DitaTopicDocumentBuilder(XmlStreamWriter, boolean)}.
	 * 
	 * @param out
	 *            the writer to which formatted XML content output
	 */
	public DitaTopicDocumentBuilder(Writer out) {
		super(out);
	}

	/**
	 * Equivalent to <code>new DitaTopicDocumentBuilder(writer,true)</code>
	 * 
	 * @see #DitaTopicDocumentBuilder(XmlStreamWriter, boolean)
	 */
	public DitaTopicDocumentBuilder(XmlStreamWriter writer) {
		this(writer, true);
	}

	/**
	 * @param writer
	 *            the writer to which output is written
	 * @param formatting
	 *            indicate if the writer should format output
	 */
	public DitaTopicDocumentBuilder(XmlStreamWriter writer, boolean formatting) {
		super(formatting ? wrapStreamWriter(writer) : writer);
	}

	@Override
	protected XmlStreamWriter createXmlStreamWriter(Writer out) {
		XmlStreamWriter writer = super.createXmlStreamWriter(out);
		return wrapStreamWriter(writer);
	}

	/**
	 * wrap the stream writer in order to produce formatted output
	 */
	private static FormattingXMLStreamWriter wrapStreamWriter(XmlStreamWriter writer) {
		return new FormattingXMLStreamWriter(writer) {
			@Override
			protected boolean preserveWhitespace(String elementName) {
				return elementName.equals("codeblock") || elementName.startsWith("pre"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
	}

	public void setDoctype(String doctype) {
		this.doctype = doctype;
	}

	public String getDoctype() {
		return doctype;
	}

	@Override
	public void acronym(String text, String definition) {
		ensureOpenTopic();
		// TODO: definition? according to DITA 1.1 'term' is the right thing to use here, however DITA 1.1 has no provision for a glossary.
		//       we may want to look at reference/refbody/simpletable to generate a glossary
		writer.writeStartElement("term"); //$NON-NLS-1$
		characters(text);
		writer.writeEndElement();
	}

	private BlockDescription findBlockDescription(BlockType type) {
		for (int x = blockDescriptions.size() - 1; x >= 0; --x) {
			BlockDescription blockDescription = blockDescriptions.get(x);
			if (blockDescription.type == type) {
				return blockDescription;
			}
		}
		return null;
	}

	private static class BlockDescription {
		BlockType type;

		int size;

		int entrySize; // the size of an entry, if it is open, otherwise 0

		@SuppressWarnings("unused")
		final String[] nestedElementNames;

		final boolean closeElementsOnBlockStart;

		public BlockDescription(BlockType type, int size, String[] nestedElementNames, boolean closeElementsOnBlockStart) {
			this.size = size;
			this.entrySize = nestedElementNames == null ? 0 : nestedElementNames.length;
			this.type = type;
			this.nestedElementNames = nestedElementNames;
			this.closeElementsOnBlockStart = closeElementsOnBlockStart;
		}
	}

	@Override
	public void beginBlock(BlockType type, Attributes attributes) {
		ensureOpenTopic();

		String elementName;
		String[] elementNames = null;
		boolean allowTitle = false;
		boolean closeElementsOnBlockStart = false;
		BlockDescription previousBlock = null;
		if (!blockDescriptions.isEmpty()) {
			previousBlock = blockDescriptions.peek();
		}
		boolean phraseTitle = false;

		switch (type) {
		case BULLETED_LIST:
			elementName = "ul"; //$NON-NLS-1$
			break;
		case NUMERIC_LIST:
			elementName = "ol"; //$NON-NLS-1$
			break;
		case DEFINITION_LIST:
			elementName = "dl"; //$NON-NLS-1$
			break;
		case DEFINITION_TERM:

			BlockDescription blockDescription = findBlockDescription(BlockType.DEFINITION_LIST);
			if (blockDescription.entrySize > 0) {
				endBlockEntry(blockDescription);
			}
			openBlockEntry(blockDescription, new String[] { "dlentry" }); //$NON-NLS-1$

			elementName = "dt"; //$NON-NLS-1$
			break;
		case DEFINITION_ITEM:
			elementName = "dd"; //$NON-NLS-1$
			elementNames = new String[] { "p" }; //$NON-NLS-1$
			closeElementsOnBlockStart = true;
			break;
		case FOOTNOTE:
		case PARAGRAPH:
			elementName = "p"; //$NON-NLS-1$
			break;
		case CODE:
			elementName = "codeph"; //$NON-NLS-1$
			break;
		case PREFORMATTED:
			elementName = "pre"; //$NON-NLS-1$
			break;
		case QUOTE:
			elementName = "lq"; //$NON-NLS-1$
			break;
		case LIST_ITEM:
			elementName = "li"; //$NON-NLS-1$
			elementNames = new String[] { "p" }; //$NON-NLS-1$
			closeElementsOnBlockStart = true;
			break;
		case TABLE:
			elementName = "simpletable"; //$NON-NLS-1$
			break;
		case TABLE_CELL_HEADER:
			// TODO: no such thing as header cells in DITA, only header rows
			//       need a way to detect beforehand if we're about to emit a header row
			elementName = "stentry"; //$NON-NLS-1$
			break;
		case TABLE_CELL_NORMAL:
			elementName = "stentry"; //$NON-NLS-1$
			break;
		case TABLE_ROW:
			elementName = "strow"; //$NON-NLS-1$
			break;
		case INFORMATION:
		case NOTE:
		case WARNING:
		case TIP:
		case PANEL:
			elementName = "note"; //$NON-NLS-1$
			allowTitle = true;
			phraseTitle = true;
			break;
		case DIV:
			elementName = null;
			break;
		default:
			throw new IllegalStateException(type.name());
		}

		int blockSize;
		if (elementName != null) {
			blockSize = 1;

			if (previousBlock != null && previousBlock.closeElementsOnBlockStart) {
				endBlockEntry(previousBlock);
			}
			writer.writeStartElement(elementName);
			switch (type) {
			case INFORMATION:
				writer.writeAttribute("type", "important"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case NOTE:
				writer.writeAttribute("type", "note"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case WARNING:
				writer.writeAttribute("type", "caution"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case TIP:
				writer.writeAttribute("type", "tip"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case PANEL:
				writer.writeAttribute("type", "other"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}
			applyAttributes(attributes);

			if (elementNames != null) {
				for (String name : elementNames) {
					writer.writeStartElement(name);
				}
			}

			if (allowTitle && attributes.getTitle() != null) {
				if (phraseTitle) {
					writer.writeStartElement("ph"); //$NON-NLS-1$
					writer.writeAttribute("outputclass", "title"); //$NON-NLS-1$ //$NON-NLS-2$
					writer.writeCharacters(attributes.getTitle());
					writer.writeEndElement();
				} else {
					writer.writeStartElement("title"); //$NON-NLS-1$
					writer.writeCharacters(attributes.getTitle());
					writer.writeEndElement();
				}
			}

		} else {
			blockSize = 0;
		}
		blockDescriptions.push(new BlockDescription(type, blockSize, elementNames, closeElementsOnBlockStart));
	}

	@Override
	public void endBlock() {
		final BlockDescription blockDescription = blockDescriptions.pop();
		int size = blockDescription.size + blockDescription.entrySize;
		for (int x = 0; x < size; ++x) {
			writer.writeEndElement();
		}
	}

	private void endBlockEntry(BlockDescription blockDescription) {
		for (int x = 0; x < blockDescription.entrySize; ++x) {
			writer.writeEndElement();
		}
		blockDescription.entrySize = 0;
	}

	private void openBlockEntry(BlockDescription blockDescription, String[] entry) {
		for (String ent : entry) {
			writer.writeStartElement(ent);
		}
		blockDescription.entrySize += entry.length;
	}

	@Override
	public void beginDocument() {
		writer.writeStartDocument();
		writer.writeDTD(doctype);
		if (rootTitle != null) {
			writer.writeStartElement("topic"); //$NON-NLS-1$
			writer.writeStartElement("title"); //$NON-NLS-1$
			writer.writeCharacters(rootTitle);
			writer.writeEndElement();
		}
	}

	@Override
	public void beginHeading(int level, Attributes attributes) {
		closeTopics(Math.max(level - 1, 0));

		if (topicInfos.isEmpty() || topicInfos.peek().headingLevel < level) {
			TopicInfo topicInfo = new TopicInfo();
			topicInfo.headingLevel = level;
			topicInfo.openElements = 2;

			topicInfos.push(topicInfo);

			writer.writeStartElement("topic"); //$NON-NLS-1$

			if (attributes != null) {
				applyAttributes(attributes);
				attributes = null;
			}
			writer.writeStartElement("title"); //$NON-NLS-1$
		}
	}

	private void applyAttributes(Attributes attributes) {
		if (attributes.getId() != null) {
			writer.writeAttribute("id", attributes.getId()); //$NON-NLS-1$
		}
		if (attributes.getCssClass() != null) {
			writer.writeAttribute("outputclass", attributes.getCssClass()); //$NON-NLS-1$
		}
	}

	@Override
	public void beginSpan(SpanType type, Attributes attributes) {
		ensureOpenTopic();
		switch (type) {
		case BOLD:
		case STRONG:
			writer.writeStartElement("b"); //$NON-NLS-1$
			break;
		case CITATION:
			writer.writeStartElement("cite"); //$NON-NLS-1$
			break;
		case CODE:
			writer.writeStartElement("codeph"); //$NON-NLS-1$
			break;
		case DELETED:
			// no equivalent?
			writer.writeStartElement("ph"); //$NON-NLS-1$
			attributes.setCssClass(attributes.getCssClass() == null ? "deleted" : attributes.getCssClass() + " deleted"); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case EMPHASIS:
			writer.writeStartElement("i"); //$NON-NLS-1$
			break;
		case INSERTED:
			// no equivalent?
			writer.writeStartElement("ph"); //$NON-NLS-1$
			attributes.setCssClass(attributes.getCssClass() == null ? "inserted" : attributes.getCssClass() //$NON-NLS-1$
					+ " inserted"); //$NON-NLS-1$
			break;
		case UNDERLINED:
			writer.writeStartElement("u"); //$NON-NLS-1$
			break;
		case ITALIC:
			writer.writeStartElement("i"); //$NON-NLS-1$
			break;
		case SPAN:
			writer.writeStartElement("ph"); //$NON-NLS-1$
			break;
		case SUBSCRIPT:
			writer.writeStartElement("sub"); //$NON-NLS-1$
			break;
		case SUPERSCRIPT:
			writer.writeStartElement("sup"); //$NON-NLS-1$
			break;
		case MONOSPACE:
			writer.writeStartElement("tt"); //$NON-NLS-1$
			break;
		case QUOTE:
			writer.writeStartElement("q"); //$NON-NLS-1$
			break;
		case LINK: {
			LinkAttributes linkAttributes = (LinkAttributes) attributes;
			writer.writeStartElement("xref"); //$NON-NLS-1$
			writer.writeAttribute("href", computeDitaXref(linkAttributes.getHref())); //$NON-NLS-1$
		}
			break;
		default:
			Logger.getLogger(DocBookDocumentBuilder.class.getName()).warning("No DITA topic mapping for " + type); //$NON-NLS-1$
			writer.writeStartElement("ph"); //$NON-NLS-1$
			break;
		}
		applyAttributes(attributes);
	}

	@Override
	public void endSpan() {
		writer.writeEndElement();
	}

	@Override
	public void charactersUnescaped(String literal) {
		ensureOpenTopic();
		// note: this *may* have HTML tags in it
		writer.writeLiteral(literal);
	}

	private void ensureOpenTopic() {
		if (topicInfos.isEmpty()) {
			beginHeading(1, new Attributes());
			endHeading();
		}
	}

	private void closeTopics(int toLevel) {
		if (toLevel < 0) {
			toLevel = 0;
		}
		while (!topicInfos.isEmpty() && topicInfos.peek().headingLevel > toLevel) {
			TopicInfo topicInfo = topicInfos.pop();
			for (int x = 0; x < topicInfo.openElements; ++x) {
				writer.writeEndElement();
			}
		}
		if (!topicInfos.isEmpty()) {
			TopicInfo topicInfo = topicInfos.peek();
			while (topicInfo.openElements > 1) {
				--topicInfo.openElements;
				writer.writeEndElement();
			}
		}
	}

	@Override
	public void endDocument() {
		closeTopics(0);
		if (rootTitle != null) {
			writer.writeEndElement();
		}
		writer.writeEndDocument();
	}

	@Override
	public void endHeading() {
		writer.writeEndElement(); // title
		writer.writeStartElement("body"); //$NON-NLS-1$
	}

	@Override
	public void image(Attributes attributes, String url) {
		ensureOpenTopic();

		boolean emitAsFigure = attributes.getTitle() != null;
		if (emitAsFigure) {
			writer.writeStartElement("fig"); //$NON-NLS-1$
			writer.writeStartElement("title");//$NON-NLS-1$
			writer.writeCharacters(attributes.getTitle());
			writer.writeEndElement();
		}

		writer.writeEmptyElement("image"); //$NON-NLS-1$
		writer.writeAttribute("href", url); //$NON-NLS-1$
		applyImageAttributes(attributes);

		if (emitAsFigure) {
			writer.writeEndElement();
		}
	}

	@Override
	public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href, String imageUrl) {
		ensureOpenTopic();
		writer.writeStartElement("xref"); //$NON-NLS-1$
		writer.writeAttribute("href", computeDitaXref(href)); //$NON-NLS-1$
		writer.writeAttribute("format", "html"); //$NON-NLS-1$ //$NON-NLS-2$
		image(imageAttributes, imageUrl);
		writer.writeEndElement();
	}

	private void applyImageAttributes(Attributes imageAttributes) {
		applyAttributes(imageAttributes);
		if (imageAttributes instanceof ImageAttributes) {
			ImageAttributes attributes = (ImageAttributes) imageAttributes;
			if (attributes.getAlt() != null) {
				writer.writeAttribute("alt", attributes.getAlt()); //$NON-NLS-1$
			}
			if (attributes.getHeight() > 0) {
				writer.writeAttribute("height", Integer.toString(attributes.getHeight())); //$NON-NLS-1$
			}
			if (attributes.getWidth() > 0) {
				writer.writeAttribute("width", Integer.toString(attributes.getWidth())); //$NON-NLS-1$
			}
			if (attributes.getAlign() != null) {
				switch (attributes.getAlign()) {
				case Left:
					writer.writeAttribute("align", "center"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case Right:
					writer.writeAttribute("align", "right"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case Center:
					writer.writeAttribute("align", "center"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
				writer.writeAttribute("placement", "break"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	@Override
	public void lineBreak() {
		// no equivalent in DITA?
	}

	@Override
	public void link(Attributes attributes, String hrefOrHashName, String text) {
		ensureOpenTopic();
		writer.writeStartElement("xref"); //$NON-NLS-1$
		writer.writeAttribute("href", computeDitaXref(hrefOrHashName)); //$NON-NLS-1$
		if (text != null) {
			characters(text);
		}
		writer.writeEndElement();
	}

	@Override
	public void characters(String text) {
		ensureOpenTopic();
		super.characters(text);
	}

	@Override
	public void entityReference(String entity) {
		ensureOpenTopic();
		if (entity.startsWith("#")) { //$NON-NLS-1$
			String numeric = entity.substring(1);
			int base = 10;
			if (numeric.startsWith("x")) { //$NON-NLS-1$
				numeric = entity.substring(1);
				base = 16;
			}
			int unicodeValue = Integer.parseInt(numeric, base);
			if (entityReferenceToUnicode.contains(unicodeValue)) {
				writer.writeCharacters("" + ((char) unicodeValue)); //$NON-NLS-1$
				return;
			}
		}
		writer.writeEntityRef(entity);
	}

	/**
	 * the outline if available, otherwise null {@link #setOutline(OutlineItem)}
	 */
	public OutlineItem getOutline() {
		return outline;
	}

	/**
	 * Set the outline of the document being parsed if xref URLs are to be correctly computed. OASIS DITA has its own
	 * URL syntax for DITA-specific links, which need some translation at the time that we build the document.
	 */
	public void setOutline(OutlineItem outline) {
		this.outline = outline;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * According to the DITA documentation, DITA content URLs use special syntax. this method translates internal URLs
	 * correctly according to the DITA rules.
	 * 
	 * @return the href adjusted, or the original href if the given URL appears to be to non-document content
	 */
	private String computeDitaXref(String href) {
		if (href.startsWith("#") && topicBreakLevel < Integer.MAX_VALUE) { //$NON-NLS-1$
			if (outline != null) {
				OutlineItem item = outline.findItemById(href.substring(1));
				if (item != null) {
					OutlineItem topicItem = computeTopicFileItem(item);
					String targetFilename = computeTargetFilename(topicItem);
					String ref;
					if (targetFilename.equals(filename)) {
						ref = href;
					} else {
						ref = targetFilename + href;
					}
					return ref;
				}
			}
		}
		return href;
	}

	public static String computeName(String headingId, String topicFilenameSuffix) {
		String name = headingId == null ? __TOPIC : headingId.replaceAll("[^a-zA-Z0-9_.-]", "-"); //$NON-NLS-1$ //$NON-NLS-2$
		name = name + topicFilenameSuffix;
		return name;
	}

	private String computeTargetFilename(OutlineItem item) {
		String filenameSuffix = filename.substring(filename.lastIndexOf('.'));
		return computeName(item.getLevel() == topicBreakLevel ? item.getId() : null, filenameSuffix);
	}

	private OutlineItem computeTopicFileItem(OutlineItem item) {
		while (item.getLevel() > topicBreakLevel && item.getParent() != null
				&& item.getParent().getLevel() > (topicBreakLevel - 1)) {
			item = item.getParent();
		}
		return item;
	}

	/**
	 * the heading level at which topics are determined
	 */
	public int getTopicBreakLevel() {
		return topicBreakLevel;
	}

	/**
	 * the heading level at which topics are determined
	 */
	public void setTopicBreakLevel(int topicBreakLevel) {
		this.topicBreakLevel = topicBreakLevel;
	}

	/**
	 * The title of the root topic if there should be one. If specified, the topic file is created with a 'wrapper' root
	 * topic with the given title.
	 */
	public void setRootTopicTitle(String rootTitle) {
		this.rootTitle = rootTitle;
	}

	/**
	 * The title of the root topic if there should be one. If specified, the topic file is created with a 'wrapper' root
	 * topic with the given title.
	 */
	public String getRootTopicTitle() {
		return rootTitle;
	}
}
