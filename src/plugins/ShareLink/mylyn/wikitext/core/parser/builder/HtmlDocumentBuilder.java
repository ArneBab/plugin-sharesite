/*******************************************************************************
 * Copyright (c) 2007, 2012 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package plugins.ShareWiki.mylyn.wikitext.core.parser.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import plugins.ShareWiki.mylyn.wikitext.core.parser.Attributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.ImageAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.ImageAttributes.Align;
import plugins.ShareWiki.mylyn.wikitext.core.parser.LinkAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.ListAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.QuoteAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.TableAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.TableCellAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.TableRowAttributes;
import plugins.ShareWiki.mylyn.wikitext.core.util.DefaultXmlStreamWriter;
import plugins.ShareWiki.mylyn.wikitext.core.util.FormattingXMLStreamWriter;
import plugins.ShareWiki.mylyn.wikitext.core.util.XmlStreamWriter;

/**
 * A builder that produces XHTML output. The nature of the output is affected by various settings on the builder.
 * 
 * @author David Green
 * @author Matthias Kempka extensibility improvements, see bug 259089
 * @since 1.0
 */
public class HtmlDocumentBuilder extends AbstractXmlDocumentBuilder {
	private static final Pattern ABSOLUTE_URL_PATTERN = Pattern.compile("[a-zA-Z]{3,8}://?.*"); //$NON-NLS-1$

	private static final Map<String, String> entityReferenceToNumericEquivalent = new HashMap<String, String>();
	static {
		entityReferenceToNumericEquivalent.put("nbsp", "#160"); //$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("iexcl", "#161");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("cent", "#162");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("pound", "#163");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("curren", "#164");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("yen", "#165");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("brvbar", "#166");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("sect", "#167");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("uml", "#168");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("copy", "#169");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ordf", "#170");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("laquo", "#171");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("not", "#172");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("shy", "#173");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("reg", "#174");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("macr", "#175");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("deg", "#176");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("plusmn", "#177");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("sup2", "#178");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("sup3", "#179");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("acute", "#180");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("micro", "#181");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("para", "#182");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("middot", "#183");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("cedil", "#184");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("sup1", "#185");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ordm", "#186");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("raquo", "#187");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("frac14", "#188");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("frac12", "#189");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("frac34", "#190");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("iquest", "#191");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("times", "#215");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("divide", "#247");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Agrave", "#192");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Aacute", "#193");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Acirc", "#194");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Atilde", "#195");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Auml", "#196");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Aring", "#197");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("AElig", "#198");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ccedil", "#199");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Egrave", "#200");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Eacute", "#201");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ecirc", "#202");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Euml", "#203");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Igrave", "#204");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Iacute", "#205");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Icirc", "#206");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Iuml", "#207");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ETH", "#208");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ntilde", "#209");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ograve", "#210");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Oacute", "#211");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ocirc", "#212");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Otilde", "#213");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ouml", "#214");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Oslash", "#216");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ugrave", "#217");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Uacute", "#218");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Ucirc", "#219");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Uuml", "#220");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("Yacute", "#221");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("THORN", "#222");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("szlig", "#223");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("agrave", "#224");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("aacute", "#225");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("acirc", "#226");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("atilde", "#227");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("auml", "#228");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("aring", "#229");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("aelig", "#230");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ccedil", "#231");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("egrave", "#232");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("eacute", "#233");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ecirc", "#234");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("euml", "#235");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("igrave", "#236");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("iacute", "#237");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("icirc", "#238");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("iuml", "#239");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("eth", "#240");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ntilde", "#241");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ograve", "#242");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("oacute", "#243");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ocirc", "#244");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("otilde", "#245");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ouml", "#246");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("oslash", "#248");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ugrave", "#249");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("uacute", "#250");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("ucirc", "#251");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("uuml", "#252");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("yacute", "#253");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("thorn", "#254");//$NON-NLS-1$ //$NON-NLS-2$
		entityReferenceToNumericEquivalent.put("yuml", "#255");//$NON-NLS-1$ //$NON-NLS-2$

	}

	private static final Map<SpanType, String> spanTypeToElementName = new HashMap<SpanType, String>();
	static {
		spanTypeToElementName.put(SpanType.LINK, "a"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.BOLD, "b"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.CITATION, "cite"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.ITALIC, "i"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.EMPHASIS, "em"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.STRONG, "strong"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.DELETED, "del"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.INSERTED, "ins"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.QUOTE, "q"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.UNDERLINED, "u"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.SUPERSCRIPT, "sup"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.SUBSCRIPT, "sub"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.SPAN, "span"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.CODE, "code"); //$NON-NLS-1$
		spanTypeToElementName.put(SpanType.MONOSPACE, "tt"); //$NON-NLS-1$
	}

	private static final Map<BlockType, ElementInfo> blockTypeToElementInfo = new HashMap<BlockType, ElementInfo>();
	static {
		blockTypeToElementInfo.put(BlockType.BULLETED_LIST, new ElementInfo("ul")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.CODE, new ElementInfo("code")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.DIV, new ElementInfo("div")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.FOOTNOTE, new ElementInfo("footnote")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.LIST_ITEM, new ElementInfo("li")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.NUMERIC_LIST, new ElementInfo("ol")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.DEFINITION_LIST, new ElementInfo("dl")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.DEFINITION_TERM, new ElementInfo("dt")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.DEFINITION_ITEM, new ElementInfo("dd")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.PARAGRAPH, new ElementInfo("p")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.PREFORMATTED, new ElementInfo("pre")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.QUOTE, new ElementInfo("blockquote")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.TABLE, new ElementInfo("table")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.TABLE_CELL_HEADER, new ElementInfo("th")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.TABLE_CELL_NORMAL, new ElementInfo("td")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.TABLE_ROW, new ElementInfo("tr")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.TIP, new ElementInfo("div", "tip", //$NON-NLS-1$ //$NON-NLS-2$
				"border: 1px solid #090;background-color: #dfd;margin: 20px;padding: 0px 6px 0px 6px;")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.WARNING, new ElementInfo("div", "warning", //$NON-NLS-1$ //$NON-NLS-2$
				"border: 1px solid #c00;background-color: #fcc;margin: 20px;padding: 0px 6px 0px 6px;")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.INFORMATION, new ElementInfo("div", "info", //$NON-NLS-1$ //$NON-NLS-2$
				"border: 1px solid #3c78b5;background-color: #D8E4F1;margin: 20px;padding: 0px 6px 0px 6px;")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.NOTE, new ElementInfo("div", "note", //$NON-NLS-1$ //$NON-NLS-2$
				"border: 1px solid #F0C000;background-color: #FFFFCE;margin: 20px;padding: 0px 6px 0px 6px;")); //$NON-NLS-1$
		blockTypeToElementInfo.put(BlockType.PANEL, new ElementInfo("div", "panel", //$NON-NLS-1$ //$NON-NLS-2$
				"border: 1px solid #ccc;background-color: #FFFFCE;margin: 10px;padding: 0px 6px 0px 6px;")); //$NON-NLS-1$

	}

	private String htmlNsUri = "http://www.w3.org/1999/xhtml"; //$NON-NLS-1$

	private String htmlDtd = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"; //$NON-NLS-1$

	private boolean xhtmlStrict = false;

	private boolean emitAsDocument = true;

	private boolean emitDtd = false;

	private String encoding = "utf-8"; //$NON-NLS-1$

	private String title;

	private String defaultAbsoluteLinkTarget;

	private List<Stylesheet> stylesheets = null;

	private boolean useInlineStyles = true;

	private boolean suppressBuiltInStyles = false;

	private String linkRel;

	private String prependImagePrefix;

	private boolean filterEntityReferences = false;

	private String copyrightNotice;

	/**
	 * construct the HtmlDocumentBuilder.
	 * 
	 * @param out
	 *            the writer to which content is written
	 */
	public HtmlDocumentBuilder(Writer out) {
		this(out, false);
	}

	/**
	 * construct the HtmlDocumentBuilder.
	 * 
	 * @param out
	 *            the writer to which content is written
	 * @param formatting
	 *            indicate if the output should be formatted
	 */
	public HtmlDocumentBuilder(Writer out, boolean formatting) {
		super(formatting ? createFormattingXmlStreamWriter(out) : new DefaultXmlStreamWriter(out));
	}

	/**
	 * construct the HtmlDocumentBuilder.
	 * 
	 * @param writer
	 *            the writer to which content is written
	 */
	public HtmlDocumentBuilder(XmlStreamWriter writer) {
		super(writer);
	}

	/**
	 * Copy the configuration of this builder to the provided one. After calling this method the configuration of the
	 * other builder should be the same as this one, including stylesheets. Subclasses that have configurable settings
	 * should override this method to ensure that those settings are properly copied.
	 * 
	 * @param other
	 *            the builder to which settings are copied.
	 */
	public void copyConfiguration(HtmlDocumentBuilder other) {
		other.setBase(getBase());
		other.setBaseInHead(isBaseInHead());
		other.setDefaultAbsoluteLinkTarget(getDefaultAbsoluteLinkTarget());
		other.setEmitAsDocument(isEmitAsDocument());
		other.setEmitDtd(isEmitDtd());
		other.setHtmlDtd(getHtmlDtd());
		other.setHtmlNsUri(getHtmlNsUri());
		other.setLinkRel(getLinkRel());
		other.setTitle(getTitle());
		other.setUseInlineStyles(isUseInlineStyles());
		other.setSuppressBuiltInStyles(isSuppressBuiltInStyles());
		other.setXhtmlStrict(xhtmlStrict);
		other.setPrependImagePrefix(prependImagePrefix);
		other.setCopyrightNotice(getCopyrightNotice());
		if (stylesheets != null) {
			other.stylesheets = new ArrayList<Stylesheet>();
			other.stylesheets.addAll(stylesheets);
		}
	}

	protected static XmlStreamWriter createFormattingXmlStreamWriter(Writer out) {
		return new FormattingXMLStreamWriter(new DefaultXmlStreamWriter(out)) {
			@Override
			protected boolean preserveWhitespace(String elementName) {
				return elementName.equals("pre") || elementName.equals("code"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
	}

	/**
	 * The XML Namespace URI of the HTML elements, only used if {@link #isEmitAsDocument()}. The default value is "
	 * <code>http://www.w3.org/1999/xhtml</code>".
	 */
	public String getHtmlNsUri() {
		return htmlNsUri;
	}

	/**
	 * The XML Namespace URI of the HTML elements, only used if {@link #isEmitAsDocument()}. The default value is "
	 * <code>http://www.w3.org/1999/xhtml</code>".
	 */
	public void setHtmlNsUri(String htmlNsUri) {
		this.htmlNsUri = htmlNsUri;
	}

	/**
	 * The DTD to emit, if {@link #isEmitDtd()} and {@link #isEmitAsDocument()}. The default value is
	 * <code>&lt;!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"&gt;</code>
	 */
	public String getHtmlDtd() {
		return htmlDtd;
	}

	/**
	 * The DTD to emit, if {@link #isEmitDtd()} and {@link #isEmitAsDocument()}.
	 * 
	 * @see #getHtmlDtd()
	 */
	public void setHtmlDtd(String htmlDtd) {
		this.htmlDtd = htmlDtd;
	}

	/**
	 * Indicate if the resulting HTML should be emitted as a document. If false, the html and body tags are not included
	 * in the output. Default value is true.
	 */
	public boolean isEmitAsDocument() {
		return emitAsDocument;
	}

	/**
	 * Indicate if the resulting HTML should be emitted as a document. If false, the html and body tags are not included
	 * in the output. Default value is true.
	 */
	public void setEmitAsDocument(boolean emitAsDocument) {
		this.emitAsDocument = emitAsDocument;
	}

	/**
	 * Indicate if the resulting HTML should include a DTD. Ignored unless {@link #isEmitAsDocument()}. Default value is
	 * false.
	 */
	public boolean isEmitDtd() {
		return emitDtd;
	}

	/**
	 * Indicate if the resulting HTML should include a DTD. Ignored unless {@link #isEmitAsDocument()}. Default value is
	 * false.
	 */
	public void setEmitDtd(boolean emitDtd) {
		this.emitDtd = emitDtd;
	}

	/**
	 * Specify the character encoding for use in the HTML meta tag. For example, if the charset is specified as
	 * <code>"utf-8"</code>: <code>&lt;meta http-equiv="Content-Type" content="text/html; charset=utf-8"/&gt;</code> The
	 * default is <code>"utf-8"</code>. Ignored unless {@link #isEmitAsDocument()}
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Specify the character encoding for use in the HTML meta tag. For example, if the charset is specified as
	 * <code>"utf-8"</code>: <code>&lt;meta http-equiv="Content-Type" content="text/html; charset=utf-8"/&gt;</code> The
	 * default is <code>"utf-8"</code>.
	 * 
	 * @param encoding
	 *            the character encoding to use, or null if the HTML meta tag should not be emitted Ignored unless
	 *            {@link #isEmitAsDocument()}
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set the document title, which will be emitted into the &lt;title&gt; element. Ignored unless
	 * {@link #isEmitAsDocument()}
	 * 
	 * @return the title or null if there is none
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the document title, which will be emitted into the &lt;title&gt; element. Ignored unless
	 * {@link #isEmitAsDocument()}
	 * 
	 * @param title
	 *            the title or null if there is none
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * A default target attribute for links that have absolute (not relative) urls. By default this value is null.
	 * Setting this value will cause all HTML anchors to have their target attribute set if it's not explicitly
	 * specified in a {@link LinkAttributes}.
	 */
	public String getDefaultAbsoluteLinkTarget() {
		return defaultAbsoluteLinkTarget;
	}

	/**
	 * A default target attribute for links that have absolute (not relative) urls. By default this value is null.
	 * Setting this value will cause all HTML anchors to have their target attribute set if it's not explicitly
	 * specified in a {@link LinkAttributes}.
	 */
	public void setDefaultAbsoluteLinkTarget(String defaultAbsoluteLinkTarget) {
		this.defaultAbsoluteLinkTarget = defaultAbsoluteLinkTarget;
	}

	/**
	 * indicate if the builder should attempt to conform to strict XHTML rules. The default is false.
	 */
	public boolean isXhtmlStrict() {
		return xhtmlStrict;
	}

	/**
	 * indicate if the builder should attempt to conform to strict XHTML rules. The default is false.
	 */
	public void setXhtmlStrict(boolean xhtmlStrict) {
		this.xhtmlStrict = xhtmlStrict;
	}

	/**
	 * Add a CSS stylesheet to the output document as an URL, where the CSS stylesheet is referenced as an HTML link.
	 * Calling this method after {@link #beginDocument() starting the document} has no effect. Generates code similar to
	 * the following: <code>
	 *   &lt;link type="text/css" rel="stylesheet" href="url"/>
	 * </code>
	 * 
	 * @param url
	 *            the CSS url to use, which may be relative or absolute
	 * @return the stylesheet, whose attributes may be modified
	 * @see #addCssStylesheet(File)
	 * @deprecated use {@link #addCssStylesheet(Stylesheet)} instead
	 */
	@Deprecated
	public void addCssStylesheet(String url) {
		addCssStylesheet(new Stylesheet(url));
	}

	/**
	 * Add a CSS stylesheet to the output document, where the contents of the CSS stylesheet are embedded in the HTML.
	 * Calling this method after {@link #beginDocument() starting the document} has no effect. Generates code similar to
	 * the following:
	 * 
	 * <pre>
	 * &lt;code&gt;
	 *   &lt;style type=&quot;text/css&quot;&gt;
	 *   ... contents of the file ...
	 *   &lt;/style&gt;
	 * &lt;/code&gt;
	 * </pre>
	 * 
	 * @param file
	 *            the CSS file whose contents must be available
	 * @return the stylesheet, whose attributes may be modified
	 * @see #addCssStylesheet(String)
	 * @deprecated use {@link #addCssStylesheet(Stylesheet)} instead
	 */
	@Deprecated
	public void addCssStylesheet(File file) {
		addCssStylesheet(new Stylesheet(file));
	}

	/**
	 * Add a CSS stylesheet to the output document. Calling this method after {@link #beginDocument() starting the
	 * document} has no effect.
	 */
	public void addCssStylesheet(Stylesheet stylesheet) {
		if (stylesheet.file != null) {
			checkFileReadable(stylesheet.file);
		}

		if (stylesheets == null) {
			stylesheets = new ArrayList<Stylesheet>();
		}
		stylesheets.add(stylesheet);
	}

	protected void checkFileReadable(File file) {
		if (!file.exists()) {
			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("HtmlDocumentBuilder.3"), file)); //$NON-NLS-1$
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("HtmlDocumentBuilder.1"), file)); //$NON-NLS-1$
		}
		if (!file.canRead()) {
			throw new IllegalArgumentException(MessageFormat.format(Messages.getString("HtmlDocumentBuilder.2"), file)); //$NON-NLS-1$
		}
	}

	/**
	 * Indicate if inline styles should be used when creating output such as text boxes. When disabled inline styles are
	 * suppressed and CSS classes are used instead, with the default styles emitted as a stylesheet in the document
	 * head. If disabled and {@link #isEmitAsDocument()} is false, this option has the same effect as
	 * {@link #isSuppressBuiltInStyles()}. The default is true.
	 * 
	 * @see #isSuppressBuiltInStyles()
	 */
	public boolean isUseInlineStyles() {
		return useInlineStyles;
	}

	/**
	 * Indicate if inline styles should be used when creating output such as text boxes. When disabled inline styles are
	 * suppressed and CSS classes are used instead, with the default styles emitted as a stylesheet in the document
	 * head. If disabled and {@link #isEmitAsDocument()} is false, this option has the same effect as
	 * {@link #isSuppressBuiltInStyles()}. The default is true.
	 */
	public void setUseInlineStyles(boolean useInlineStyles) {
		this.useInlineStyles = useInlineStyles;
	}

	/**
	 * indicate if default built-in CSS styles should be suppressed. Built-in styles are styles that are emitted by this
	 * builder to create the desired visual effect when rendering certain types of elements, such as warnings or infos.
	 * the default is false.
	 * 
	 * @see #isUseInlineStyles()
	 */
	public boolean isSuppressBuiltInStyles() {
		return suppressBuiltInStyles;
	}

	/**
	 * indicate if default built-in CSS styles should be suppressed. Built-in styles are styles that are emitted by this
	 * builder to create the desired visual effect when rendering certain types of elements, such as warnings or infos.
	 * the default is false.
	 */
	public void setSuppressBuiltInStyles(boolean suppressBuiltInStyles) {
		this.suppressBuiltInStyles = suppressBuiltInStyles;
	}

	/**
	 * The 'rel' value for HTML links. If specified the value is applied to all links generated by the builder. The
	 * default value is null. Setting this value to "nofollow" is recommended for rendering HTML in areas where users
	 * may add links, for example in a blog comment. See <a
	 * href="http://en.wikipedia.org/wiki/Nofollow">http://en.wikipedia.org/wiki/Nofollow</a> for more information.
	 * 
	 * @return the rel or null if there is none.
	 * @see LinkAttributes#getRel()
	 */
	public String getLinkRel() {
		return linkRel;
	}

	/**
	 * The 'rel' value for HTML links. If specified the value is applied to all links generated by the builder. The
	 * default value is null. Setting this value to "nofollow" is recommended for rendering HTML in areas where users
	 * may add links, for example in a blog comment. See <a
	 * href="http://en.wikipedia.org/wiki/Nofollow">http://en.wikipedia.org/wiki/Nofollow</a> for more information.
	 * 
	 * @param linkRel
	 *            the rel or null if there is none.
	 * @see LinkAttributes#getRel()
	 */
	public void setLinkRel(String linkRel) {
		this.linkRel = linkRel;
	}

	@Override
	public void beginDocument() {
		writer.setDefaultNamespace(htmlNsUri);

		if (emitAsDocument) {
			if (encoding != null && encoding.length() > 0) {
				writer.writeStartDocument(encoding, "1.0"); //$NON-NLS-1$
			} else {
				writer.writeStartDocument();
			}

			if (emitDtd && htmlDtd != null) {
				writer.writeDTD(htmlDtd);
			}

			if (copyrightNotice != null) {
				writer.writeComment(copyrightNotice);
			}

			writer.writeStartElement(htmlNsUri, "html"); //$NON-NLS-1$
			writer.writeDefaultNamespace(htmlNsUri);

			emitHead();
			beginBody();
		} else {
			// sanity check
			if (stylesheets != null && !stylesheets.isEmpty()) {
				throw new IllegalStateException(Messages.getString("HtmlDocumentBuilder.0")); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Emit the HTML head, including the head tag itself.
	 * 
	 * @since 1.8
	 * @see #emitHeadContents()
	 */
	protected void emitHead() {
		writer.writeStartElement(htmlNsUri, "head"); //$NON-NLS-1$
		emitHeadContents();
		writer.writeEndElement(); // head
	}

	/**
	 * emit the contents of the HTML head, excluding the head tag itself. Subclasses may override to change the contents
	 * of the head. Subclasses should consider calling <code>super.emitHeadContents()</code> in order to preserve
	 * features such as emitting the base, title and stylesheets.
	 * 
	 * @see #emitHead()
	 */
	protected void emitHeadContents() {
		if (encoding != null && encoding.length() > 0) {
			// bug 259786: add the charset as a HTML meta http-equiv
			// see http://www.w3.org/International/tutorials/tutorial-char-enc/
			//
			// <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/> 
			writer.writeEmptyElement(htmlNsUri, "meta"); //$NON-NLS-1$
			writer.writeAttribute("http-equiv", "Content-Type"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.writeAttribute("content", String.format("text/html; charset=%s", encoding)); //$NON-NLS-1$//$NON-NLS-2$
		}
		if (copyrightNotice != null) {
			writer.writeEmptyElement(htmlNsUri, "meta"); //$NON-NLS-1$
			writer.writeAttribute("name", "copyright"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.writeAttribute("content", copyrightNotice); //$NON-NLS-1$
		}
		if (base != null && baseInHead) {
			writer.writeEmptyElement(htmlNsUri, "base"); //$NON-NLS-1$
			writer.writeAttribute("href", base.toString()); //$NON-NLS-1$
		}
		if (title != null) {
			writer.writeStartElement(htmlNsUri, "title"); //$NON-NLS-1$
			writer.writeCharacters(title);
			writer.writeEndElement(); // title
		}
		if (!useInlineStyles && !suppressBuiltInStyles) {
			writer.writeStartElement(htmlNsUri, "style"); //$NON-NLS-1$
			writer.writeAttribute("type", "text/css"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.writeCharacters("\n"); //$NON-NLS-1$
			for (Entry<BlockType, ElementInfo> ent : blockTypeToElementInfo.entrySet()) {
				ElementInfo elementInfo = ent.getValue();
				if (elementInfo.cssStyles != null && elementInfo.cssClass != null) {
					String[] classes = elementInfo.cssClass.split("\\s+"); //$NON-NLS-1$
					for (String cssClass : classes) {
						writer.writeCharacters("."); //$NON-NLS-1$
						writer.writeCharacters(cssClass);
						writer.writeCharacters(" "); //$NON-NLS-1$
					}
					writer.writeCharacters("{"); //$NON-NLS-1$
					writer.writeCharacters(elementInfo.cssStyles);
					writer.writeCharacters("}\n"); //$NON-NLS-1$
				}
			}
			writer.writeEndElement();
		}
		if (stylesheets != null) {
			for (Stylesheet stylesheet : stylesheets) {
				emitStylesheet(stylesheet);
			}
		}
	}

	private void emitStylesheet(Stylesheet stylesheet) {
		if (stylesheet.url != null) {
			// <link type="text/css" rel="stylesheet" href="url"/>
			writer.writeEmptyElement(htmlNsUri, "link"); //$NON-NLS-1$
			writer.writeAttribute("type", "text/css"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.writeAttribute("rel", "stylesheet"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.writeAttribute("href", makeUrlAbsolute(stylesheet.url)); //$NON-NLS-1$
			for (Entry<String, String> attr : stylesheet.attributes.entrySet()) {
				String attrName = attr.getKey();
				if (!"type".equals(attrName) && !"rel".equals(attrName) && !"href".equals(attrName)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					writer.writeAttribute(attrName, attr.getValue());
				}
			}
		} else {
			//	<style type="text/css">
			//	... contents of the file ...
			//	</style>
			writer.writeStartElement(htmlNsUri, "style"); //$NON-NLS-1$
			writer.writeAttribute("type", "text/css"); //$NON-NLS-1$ //$NON-NLS-2$
			for (Entry<String, String> attr : stylesheet.attributes.entrySet()) {
				String attrName = attr.getKey();
				if (!"type".equals(attrName)) { //$NON-NLS-1$ 
					writer.writeAttribute(attrName, attr.getValue());
				}
			}

			String css;
			if (stylesheet.file != null) {
				try {
					css = readFully(stylesheet.file);
				} catch (IOException e) {
					throw new IllegalStateException(MessageFormat.format(Messages.getString("HtmlDocumentBuilder.4"), //$NON-NLS-1$
							stylesheet.file), e);
				}
			} else {
				try {
					css = readFully(stylesheet.reader, 1024);
				} catch (IOException e) {
					throw new IllegalStateException(Messages.getString("HtmlDocumentBuilder.5"), e); //$NON-NLS-1$
				}
			}
			writer.writeCharacters(css);
			writer.writeEndElement();
		}
	}

	@Override
	public void endDocument() {
		if (emitAsDocument) {
			endBody();
			writer.writeEndElement(); // html
			writer.writeEndDocument();
		}

		writer.close();
	}

	/**
	 * begin the body by emitting the body element. Overriding methods should call <code>super.beginBody()</code>.
	 * 
	 * @see #endBody()
	 */
	protected void beginBody() {
		writer.writeStartElement(htmlNsUri, "body"); //$NON-NLS-1$
	}

	/**
	 * end the body by emitting the body end element tag. Overriding methods should call <code>super.endBody()</code>.
	 * 
	 * @see #beginBody()
	 */
	protected void endBody() {
		writer.writeEndElement(); // body
	}

	@Override
	public void entityReference(String entity) {
		if (filterEntityReferences) {
			String emitEntity = entity.length() > 0 && entity.charAt(0) == '#'
					? entity
					: entityReferenceToNumericEquivalent.get(entity);
			if (emitEntity == null) {
				writer.writeCharacters("&"); //$NON-NLS-1$
				writer.writeCharacters(entity);
				writer.writeCharacters(";"); //$NON-NLS-1$
			} else {
				writer.writeEntityRef(emitEntity);
			}
		} else {
			writer.writeEntityRef(entity);
		}
	}

	@Override
	public void acronym(String text, String definition) {
		writer.writeStartElement(htmlNsUri, "acronym"); //$NON-NLS-1$
		writer.writeAttribute("title", definition); //$NON-NLS-1$
		writer.writeCharacters(text);
		writer.writeEndElement();
	}

	@Override
	public void link(Attributes attributes, String hrefOrHashName, String text) {
		writer.writeStartElement(htmlNsUri, "a"); //$NON-NLS-1$
		emitAnchorHref(hrefOrHashName);
		applyLinkAttributes(attributes, hrefOrHashName);
		characters(text);
		writer.writeEndElement(); // a
	}

	@Override
	public void beginBlock(BlockType type, Attributes attributes) {
		ElementInfo elementInfo = blockTypeToElementInfo.get(type);
		if (elementInfo == null) {
			throw new IllegalStateException(type.name());
		}
		writer.writeStartElement(htmlNsUri, elementInfo.name);
		if (elementInfo.cssClass != null) {
			if (attributes.getCssClass() == null) {
				attributes.setCssClass(elementInfo.cssClass);
			} else {
				attributes.setCssClass(elementInfo.cssClass + ' ' + attributes.getCssClass());
			}
		}
		if (useInlineStyles && !suppressBuiltInStyles && elementInfo.cssStyles != null) {
			if (attributes.getCssStyle() == null) {
				attributes.setCssStyle(elementInfo.cssStyles);
			} else {
				attributes.setCssStyle(elementInfo.cssStyles + attributes.getCssStyle());
			}
		}
		if (type == BlockType.TABLE) {
			applyTableAttributes(attributes);
		} else if (type == BlockType.TABLE_ROW) {
			applyTableRowAttributes(attributes);
		} else if (type == BlockType.TABLE_CELL_HEADER || type == BlockType.TABLE_CELL_NORMAL) {
			applyCellAttributes(attributes);
		} else if (type == BlockType.BULLETED_LIST || type == BlockType.NUMERIC_LIST) {
			applyListAttributes(attributes);
		} else if (type == BlockType.QUOTE) {
			applyQuoteAttributes(attributes);
		} else {
			applyAttributes(attributes);

			// create the titled panel effect if a title is specified
			if (attributes.getTitle() != null) {
				beginBlock(BlockType.PARAGRAPH, new Attributes());
				beginSpan(SpanType.BOLD, new Attributes());
				characters(attributes.getTitle());
				endSpan();
				endBlock();
			}
		}
	}

	@Override
	public void beginHeading(int level, Attributes attributes) {
		if (level > 6) {
			level = 6;
		}
		writer.writeStartElement(htmlNsUri, "h" + level); //$NON-NLS-1$
		applyAttributes(attributes);
	}

	@Override
	public void beginSpan(SpanType type, Attributes attributes) {
		String elementName = spanTypeToElementName.get(type);
		if (elementName == null) {
			throw new IllegalStateException(type.name());
		}
		writer.writeStartElement(htmlNsUri, elementName);
		if (type == SpanType.LINK && attributes instanceof LinkAttributes) {
			String href = ((LinkAttributes) attributes).getHref();
			emitAnchorHref(href);
			applyLinkAttributes(attributes, href);
		} else {
			applyAttributes(attributes);
		}
	}

	@Override
	public void endBlock() {
		writer.writeEndElement();
	}

	@Override
	public void endHeading() {
		writer.writeEndElement();
	}

	@Override
	public void endSpan() {
		writer.writeEndElement();
	}

	@Override
	public void image(Attributes attributes, String url) {
		writer.writeEmptyElement(htmlNsUri, "img"); //$NON-NLS-1$
		applyImageAttributes(attributes);
		url = prependImageUrl(url);
		writer.writeAttribute("src", makeUrlAbsolute(url)); //$NON-NLS-1$
	}

	private void applyListAttributes(Attributes attributes) {
		applyAttributes(attributes);
		if (attributes instanceof ListAttributes) {
			ListAttributes listAttributes = (ListAttributes) attributes;
			if (listAttributes.getStart() != null) {
				writer.writeAttribute("start", listAttributes.getStart()); //$NON-NLS-1$
			}
		}
	}

	private void applyQuoteAttributes(Attributes attributes) {
		applyAttributes(attributes);
		if (attributes instanceof QuoteAttributes) {
			QuoteAttributes quoteAttributes = (QuoteAttributes) attributes;
			if (quoteAttributes.getCitation() != null) {
				writer.writeAttribute("cite", quoteAttributes.getCitation()); //$NON-NLS-1$
			}
		}
	}

	private void applyTableAttributes(Attributes attributes) {
		applyAttributes(attributes);
		if (attributes.getTitle() != null) {
			writer.writeAttribute("title", attributes.getTitle()); //$NON-NLS-1$
		}
		if (attributes instanceof TableAttributes) {
			TableAttributes tableAttributes = (TableAttributes) attributes;
			if (tableAttributes.getBgcolor() != null) {
				writer.writeAttribute("bgcolor", tableAttributes.getBgcolor()); //$NON-NLS-1$
			}
			if (tableAttributes.getBorder() != null) {
				writer.writeAttribute("border", tableAttributes.getBorder()); //$NON-NLS-1$
			}
			if (tableAttributes.getCellpadding() != null) {
				writer.writeAttribute("cellpadding", tableAttributes.getCellpadding()); //$NON-NLS-1$
			}
			if (tableAttributes.getCellspacing() != null) {
				writer.writeAttribute("cellspacing", tableAttributes.getCellspacing()); //$NON-NLS-1$
			}
			if (tableAttributes.getFrame() != null) {
				writer.writeAttribute("frame", tableAttributes.getFrame()); //$NON-NLS-1$
			}
			if (tableAttributes.getRules() != null) {
				writer.writeAttribute("rules", tableAttributes.getRules()); //$NON-NLS-1$
			}
			if (tableAttributes.getSummary() != null) {
				writer.writeAttribute("summary", tableAttributes.getSummary()); //$NON-NLS-1$
			}
			if (tableAttributes.getWidth() != null) {
				writer.writeAttribute("width", tableAttributes.getWidth()); //$NON-NLS-1$
			}
		}
	}

	private void applyTableRowAttributes(Attributes attributes) {
		applyAttributes(attributes);
		if (attributes.getTitle() != null) {
			writer.writeAttribute("title", attributes.getTitle()); //$NON-NLS-1$
		}
		if (attributes instanceof TableRowAttributes) {
			TableRowAttributes tableRowAttributes = (TableRowAttributes) attributes;
			if (tableRowAttributes.getBgcolor() != null) {
				writer.writeAttribute("bgcolor", tableRowAttributes.getBgcolor()); //$NON-NLS-1$
			}
			if (tableRowAttributes.getAlign() != null) {
				writer.writeAttribute("align", tableRowAttributes.getAlign()); //$NON-NLS-1$
			}
			if (tableRowAttributes.getValign() != null) {
				writer.writeAttribute("valign", tableRowAttributes.getValign()); //$NON-NLS-1$
			}
		}
	}

	private void applyCellAttributes(Attributes attributes) {
		applyAttributes(attributes);
		if (attributes.getTitle() != null) {
			writer.writeAttribute("title", attributes.getTitle()); //$NON-NLS-1$
		}

		if (attributes instanceof TableCellAttributes) {
			TableCellAttributes tableCellAttributes = (TableCellAttributes) attributes;
			if (tableCellAttributes.getBgcolor() != null) {
				writer.writeAttribute("bgcolor", tableCellAttributes.getBgcolor()); //$NON-NLS-1$
			}
			if (tableCellAttributes.getAlign() != null) {
				writer.writeAttribute("align", tableCellAttributes.getAlign()); //$NON-NLS-1$
			}
			if (tableCellAttributes.getValign() != null) {
				writer.writeAttribute("valign", tableCellAttributes.getValign()); //$NON-NLS-1$
			}
			if (tableCellAttributes.getRowspan() != null) {
				writer.writeAttribute("rowspan", tableCellAttributes.getRowspan()); //$NON-NLS-1$
			}
			if (tableCellAttributes.getColspan() != null) {
				writer.writeAttribute("colspan", tableCellAttributes.getColspan()); //$NON-NLS-1$
			}
		}
	}

	private void applyImageAttributes(Attributes attributes) {
		int border = 0;
		Align align = null;
		if (attributes instanceof ImageAttributes) {
			ImageAttributes imageAttributes = (ImageAttributes) attributes;
			border = imageAttributes.getBorder();
			align = imageAttributes.getAlign();
		}
		if (xhtmlStrict) {
			String borderStyle = String.format("border-width: %spx;", border); //$NON-NLS-1$
			String alignStyle = null;
			if (align != null) {
				switch (align) {
				case Center:
				case Right:
				case Left:
					alignStyle = "text-align: " + align.name().toLowerCase() + ";"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case Bottom:
				case Baseline:
				case Top:
				case Middle:
					alignStyle = "vertical-align: " + align.name().toLowerCase() + ";"; //$NON-NLS-1$ //$NON-NLS-2$
					break;
				case Texttop:
					alignStyle = "vertical-align: text-top;"; //$NON-NLS-1$
					break;
				case Absmiddle:
					alignStyle = "vertical-align: middle;"; //$NON-NLS-1$
					break;
				case Absbottom:
					alignStyle = "vertical-align: bottom;"; //$NON-NLS-1$
					break;
				}
			}
			String additionalStyles = borderStyle;
			if (alignStyle != null) {
				additionalStyles += alignStyle;
			}
			if (attributes.getCssStyle() == null || attributes.getCssStyle().length() == 0) {
				attributes.setCssStyle(additionalStyles);
			} else {
				attributes.setCssStyle(additionalStyles + attributes.getCssStyle());
			}
		}
		applyAttributes(attributes);
		boolean haveAlt = false;

		if (attributes instanceof ImageAttributes) {
			ImageAttributes imageAttributes = (ImageAttributes) attributes;
			if (imageAttributes.getHeight() != -1) {
				String val = Integer.toString(imageAttributes.getHeight());
				if (imageAttributes.isHeightPercentage()) {
					val += "%"; //$NON-NLS-1$
				}
				writer.writeAttribute("height", val); //$NON-NLS-1$
			}
			if (imageAttributes.getWidth() != -1) {
				String val = Integer.toString(imageAttributes.getWidth());
				if (imageAttributes.isWidthPercentage()) {
					val += "%"; //$NON-NLS-1$
				}
				writer.writeAttribute("width", val); //$NON-NLS-1$
			}
			if (!xhtmlStrict && align != null) {
				writer.writeAttribute("align", align.name().toLowerCase()); //$NON-NLS-1$
			}
			if (imageAttributes.getAlt() != null) {
				haveAlt = true;
				writer.writeAttribute("alt", imageAttributes.getAlt()); //$NON-NLS-1$
			}
		}
		if (attributes.getTitle() != null) {
			writer.writeAttribute("title", attributes.getTitle()); //$NON-NLS-1$
			if (!haveAlt) {
				haveAlt = true;
				writer.writeAttribute("alt", attributes.getTitle()); //$NON-NLS-1$
			}
		}
		if (xhtmlStrict) {
			if (!haveAlt) {
				// XHTML requires img/@alt
				writer.writeAttribute("alt", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			// only specify border attribute if it's not already specified in CSS
			writer.writeAttribute("border", Integer.toString(border)); //$NON-NLS-1$
		}
	}

	private void applyLinkAttributes(Attributes attributes, String href) {
		applyAttributes(attributes);
		boolean hasTarget = false;
		String rel = linkRel;
		if (attributes instanceof LinkAttributes) {
			LinkAttributes linkAttributes = (LinkAttributes) attributes;
			if (linkAttributes.getTarget() != null) {
				hasTarget = true;
				writer.writeAttribute("target", linkAttributes.getTarget()); //$NON-NLS-1$
			}
			if (linkAttributes.getRel() != null) {
				rel = rel == null ? linkAttributes.getRel() : linkAttributes.getRel() + ' ' + rel;
			}

		}
		if (attributes.getTitle() != null && attributes.getTitle().length() > 0) {
			writer.writeAttribute("title", attributes.getTitle()); //$NON-NLS-1$
		}
		if (!hasTarget && defaultAbsoluteLinkTarget != null && href != null) {
			if (isExternalLink(href)) {
				writer.writeAttribute("target", defaultAbsoluteLinkTarget); //$NON-NLS-1$
			}
		}

		if (rel != null) {
			writer.writeAttribute("rel", rel); //$NON-NLS-1$
		}
	}

	/**
	 * Note: this method does not apply the {@link Attributes#getTitle() title}.
	 */
	private void applyAttributes(Attributes attributes) {
		if (attributes.getId() != null) {
			writer.writeAttribute("id", attributes.getId()); //$NON-NLS-1$
		}
		if (attributes.getCssClass() != null) {
			writer.writeAttribute("class", attributes.getCssClass()); //$NON-NLS-1$
		}
		if (attributes.getCssStyle() != null) {
			writer.writeAttribute("style", attributes.getCssStyle()); //$NON-NLS-1$
		}
		if (attributes.getLanguage() != null) {
			writer.writeAttribute("lang", attributes.getLanguage()); //$NON-NLS-1$
		}
	}

	@Override
	public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href, String imageUrl) {
		writer.writeStartElement(htmlNsUri, "a"); //$NON-NLS-1$
		emitAnchorHref(href);
		applyLinkAttributes(linkAttributes, href);
		writer.writeEmptyElement(htmlNsUri, "img"); //$NON-NLS-1$
		applyImageAttributes(imageAttributes);
		imageUrl = prependImageUrl(imageUrl);
		writer.writeAttribute("src", makeUrlAbsolute(imageUrl)); //$NON-NLS-1$
		writer.writeEndElement(); // a
	}

	/**
	 * emit the href attribute of an anchor. Subclasses may override to alter the default href or to add other
	 * attributes such as <code>onclick</code>. Overriding classes should pass the href to
	 * {@link #makeUrlAbsolute(String)} prior to writing it to the writer.
	 * 
	 * @param href
	 *            the url for the href attribute
	 */
	protected void emitAnchorHref(String href) {
		writer.writeAttribute("href", makeUrlAbsolute(href)); //$NON-NLS-1$
	}

	private String prependImageUrl(String imageUrl) {
		if (prependImagePrefix == null || prependImagePrefix.length() == 0) {
			return imageUrl;
		}
		if (ABSOLUTE_URL_PATTERN.matcher(imageUrl).matches() || imageUrl.contains("../")) { //$NON-NLS-1$
			return imageUrl;
		}
		String url = prependImagePrefix;
		if (!prependImagePrefix.endsWith("/")) { //$NON-NLS-1$
			url += '/';
		}
		url += imageUrl;
		return url;
	}

	@Override
	public void lineBreak() {
		writer.writeEmptyElement(htmlNsUri, "br"); //$NON-NLS-1$
	}

	@Override
	public void charactersUnescaped(String literal) {
		writer.writeLiteral(literal);
	}

	private static final class ElementInfo {
		final String name;

		final String cssClass;

		final String cssStyles;

		public ElementInfo(String name, String cssClass, String cssStyles) {
			this.name = name;
			this.cssClass = cssClass;
			this.cssStyles = cssStyles != null && !cssStyles.endsWith(";") ? cssStyles + ';' : cssStyles; //$NON-NLS-1$
		}

		public ElementInfo(String name) {
			this(name, null, null);
		}
	}

	/**
	 * A CSS stylesheet definition, created via one of {@link HtmlDocumentBuilder#addCssStylesheet(File)} or
	 * {@link HtmlDocumentBuilder#addCssStylesheet(String)}.
	 */
	public static class Stylesheet {
		private final String url;

		private final File file;

		private final Reader reader;

		private final Map<String, String> attributes = new HashMap<String, String>();

		/**
		 * Create a CSS stylesheet where the contents of the CSS stylesheet are embedded in the HTML. Generates code
		 * similar to the following:
		 * 
		 * <pre>
		 * &lt;code&gt;
		 *   &lt;style type=&quot;text/css&quot;&gt;
		 *   ... contents of the file ...
		 *   &lt;/style&gt;
		 * &lt;/code&gt;
		 * </pre>
		 * 
		 * @param file
		 *            the CSS file whose contents must be available
		 */
		public Stylesheet(File file) {
			if (file == null) {
				throw new IllegalArgumentException();
			}
			this.file = file;
			url = null;
			reader = null;
		}

		/**
		 * Create a CSS stylesheet to the output document as an URL where the CSS stylesheet is referenced as an HTML
		 * link. Calling this method after {@link #beginDocument() starting the document} has no effect. Generates code
		 * similar to the following:
		 * 
		 * <pre>
		 *   &lt;link type=&quot;text/css&quot; rel=&quot;stylesheet&quot; href=&quot;url&quot;/&gt;
		 * </pre>
		 * 
		 * @param url
		 *            the CSS url to use, which may be relative or absolute
		 */
		public Stylesheet(String url) {
			if (url == null || url.length() == 0) {
				throw new IllegalArgumentException();
			}
			this.url = url;
			file = null;
			reader = null;
		}

		/**
		 * Create a CSS stylesheet where the contents of the CSS stylesheet are embedded in the HTML. Generates code
		 * similar to the following:
		 * 
		 * <pre>
		 * &lt;code&gt;
		 *   &lt;style type=&quot;text/css&quot;&gt;
		 *   ... contents of the file ...
		 *   &lt;/style&gt;
		 * &lt;/code&gt;
		 * </pre>
		 * 
		 * The caller is responsible for closing the reader.
		 * 
		 * @param reader
		 *            the reader from which content is provided.
		 */
		public Stylesheet(Reader reader) {
			if (reader == null) {
				throw new IllegalArgumentException();
			}
			this.reader = reader;
			file = null;
			url = null;
		}

		/**
		 * the attributes of the stylesheet, which may be modified prior to adding to the document. Attributes
		 * <code>href</code>, <code>type</code> and <code>rel</code> are all ignored.
		 */
		public Map<String, String> getAttributes() {
			return attributes;
		}

		/**
		 * the file of the stylesheet, or null if it's not defined
		 */
		public File getFile() {
			return file;
		}

		/**
		 * the url of the stylesheet, or null if it's not defined
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * the content reader, or null if it's not defined.
		 */
		public Reader getReader() {
			return reader;
		}
	}

	private String readFully(File inputFile) throws IOException {
		int length = (int) inputFile.length();
		if (length <= 0) {
			length = 2048;
		}
		return readFully(getReader(inputFile), length);
	}

	private String readFully(Reader input, int bufferSize) throws IOException {
		StringBuilder buf = new StringBuilder(bufferSize);
		try {
			Reader reader = new BufferedReader(input);
			int c;
			while ((c = reader.read()) != -1) {
				buf.append((char) c);
			}
		} finally {
			input.close();
		}
		return buf.toString();
	}

	protected Reader getReader(File inputFile) throws FileNotFoundException {
		return new FileReader(inputFile);
	}

	/**
	 * if specified, the prefix is prepended to relative image urls.
	 */
	public void setPrependImagePrefix(String prependImagePrefix) {
		this.prependImagePrefix = prependImagePrefix;
	}

	/**
	 * if specified, the prefix is prepended to relative image urls.
	 */
	public String getPrependImagePrefix() {
		return prependImagePrefix;
	}

	/**
	 * Indicates that {@link #entityReference(String) entity references} should be filtered. Defaults to false. When
	 * filtered, known HTML entity references are converted to their numeric counterpart, and unknown entity references
	 * are emitted as plain text.
	 * 
	 * @since 1.6
	 * @see <a href="http://www.w3schools.com/tags/ref_entities.asp">HTML Entity Reference</a>
	 */
	public boolean isFilterEntityReferences() {
		return filterEntityReferences;
	}

	/**
	 * Indicates that {@link #entityReference(String) entity references} should be filtered. Defaults to false. When
	 * filtered, known HTML entity references are converted to their numeric counterpart, and unknown entity references
	 * are emitted as plain text.
	 * 
	 * @since 1.6
	 * @see <a href="http://www.w3schools.com/tags/ref_entities.asp">HTML Entity Reference</a>
	 */
	public void setFilterEntityReferences(boolean filterEntityReferences) {
		this.filterEntityReferences = filterEntityReferences;
	}

	/**
	 * the copyright notice that should appear in the generated output
	 * 
	 * @since 1.8
	 */
	public String getCopyrightNotice() {
		return copyrightNotice;
	}

	/**
	 * the copyright notice that should appear in the generated output
	 * 
	 * @param copyrightNotice
	 *            the notice, or null if there should be none
	 * @since 1.8
	 */
	public void setCopyrightNotice(String copyrightNotice) {
		this.copyrightNotice = copyrightNotice;
	}
}
