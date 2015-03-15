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
package plugins.ShareWiki.mylyn.internal.wikitext.textile.core.token;

import plugins.ShareWiki.mylyn.internal.wikitext.textile.core.TextileContentState;
import plugins.ShareWiki.mylyn.wikitext.core.parser.Attributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.DocumentBuilder.SpanType;
import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.PatternBasedElement;
import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.PatternBasedElementProcessor;
import plugins.ShareWiki.mylyn.wikitext.textile.core.TextileLanguage;

/**
 * @author David Green
 */
public class FootnoteReferenceReplacementToken extends PatternBasedElement {

	@Override
	protected String getPattern(int groupOffset) {
		return "(?:\\[(\\d+)\\])"; //$NON-NLS-1$
	}

	@Override
	protected int getPatternGroupCount() {
		return 1;
	}

	@Override
	protected PatternBasedElementProcessor newProcessor() {
		return new FootnoteReferenceReplacementTokenProcessor();
	}

	private static class FootnoteReferenceReplacementTokenProcessor extends PatternBasedElementProcessor {
		@Override
		public void emit() {
			String footnote = group(1);

			if (((TextileLanguage) getMarkupLanguage()).isPreprocessFootnotes()
					&& !((TextileContentState) getState()).hasFootnoteNumber(footnote)) {
				builder.characters(group(0));
			} else {
				String htmlId = state.getFootnoteId(footnote);

				int originalSegmentEndOffset = state.getLineSegmentEndOffset();
				state.setLineCharacterOffset(start(1) - 1);
				state.setLineSegmentEndOffset(end(1) + 1);

				builder.beginSpan(SpanType.SUPERSCRIPT, new Attributes(null, "footnote", null, null)); //$NON-NLS-1$
				builder.link("#" + htmlId, footnote); //$NON-NLS-1$
				builder.endSpan();

				state.setLineCharacterOffset(originalSegmentEndOffset);
				state.setLineSegmentEndOffset(originalSegmentEndOffset);
			}
		}
	}

}
