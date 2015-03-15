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

import plugins.ShareWiki.mylyn.wikitext.core.parser.outline.OutlineItem;
import plugins.ShareWiki.mylyn.wikitext.core.parser.util.MarkupToEclipseToc;

/**
 * @author David Green
 */
public class SplittingMarkupToEclipseToc extends MarkupToEclipseToc {
	@Override
	protected String computeFile(OutlineItem item) {
		if (item instanceof SplitOutlineItem) {
			String target = ((SplitOutlineItem) item).getSplitTarget();
			if (target != null) {
				return target;
			}
		}
		return super.computeFile(item);
	}
}
