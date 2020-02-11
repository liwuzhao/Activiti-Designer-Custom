/*-******************************************************************************
 * Copyright (c) 2016 Iwao AVE!.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Iwao AVE! - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.activiti.designer.util.hyperlink;

import java.text.MessageFormat;

import org.activiti.designer.util.hyperlink.JavaMapperUtil.MethodMatcher;
import org.activiti.designer.util.hyperlink.JavaMapperUtil.SingleMethodStore;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * @author Iwao AVE!
 */
public abstract class HyperlinkDetector extends AbstractHyperlinkDetector
{
	public static IHyperlink linkToJavaMapperMethod(IJavaProject project, String mapperFqn,
		IRegion linkRegion, MethodMatcher methodMatcher)
	{
		SingleMethodStore methodStore = new SingleMethodStore();
		JavaMapperUtil.findMapperMethod(methodStore, project, mapperFqn, methodMatcher);
		if (methodStore.isEmpty())
			return null;
		return new ToJavaHyperlink(methodStore.getMethod(), linkRegion, "Open mapper method.");
	}


	protected String javaLinkLabel(String target)
	{
		return MessageFormat.format("Open {0} in Java Editor", target);
	}
}
