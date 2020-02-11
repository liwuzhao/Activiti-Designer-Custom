/*-****************************************************************************** 
 * Copyright (c) 2014 Iwao AVE!.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Iwao AVE! - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.activiti.designer.util.hyperlink;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.activiti.designer.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 * @author Iwao AVE!
 */
@SuppressWarnings("restriction")
public class MybatipseXmlUtil
{
	private static final List<String> defaultTypeAliases = Arrays.asList("string", "map",
		"hashmap", "list", "arraylist", "collection", "iterator", "resultset",

		"_byte", "_long", "_short", "_int", "_integer", "_double", "_float", "_boolean",

		"_byte[]", "_long[]", "_short[]", "_int[]", "_integer[]", "_double[]", "_float[]",
		"_boolean[]",

		"byte", "long", "short", "int", "integer", "double", "float", "boolean",

		"byte[]", "long[]", "short[]", "int[]", "integer[]", "double[]", "float[]", "boolean[]",

		"date", "decimal", "bigdecimal", "biginteger", "object",

		"date[]", "decimal[]", "bigdecimal[]", "biginteger[]", "object[]",

		"jdbc", "managed",

		"jndi", "pooled", "unpooled",

		"perpetual", "fifo", "lru", "soft", "weak",

		"db_vendor",

		"xml", "raw",

		"slf4j", "commons_logging", "log4j", "log4j2", "jdk_logging", "stdout_logging",
		"no_logging",

		"cglib", "javassist");

	public static boolean isDefaultTypeAlias(String type)
	{
		return type != null && defaultTypeAliases.contains(type.toLowerCase(Locale.ENGLISH));
	}


	public static String getNamespaceFromActiveEditor(IJavaProject project)
	{
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		IEditorPart editor = page.getActiveEditor();
		IEditorInput input = editor.getEditorInput();
		IPath path = ((FileEditorInput)input).getFile().getFullPath();

		try
		{
			for (IClasspathEntry entry : project.getRawClasspath())
			{
				if (entry.getPath().isPrefixOf(path))
				{
					IPath relativePath = path.makeRelativeTo(entry.getPath());
					return relativePath.removeFileExtension().toString().replace('/', '.');
				}
			}
		}
		catch (JavaModelException e)
		{
			Activator.log(Status.ERROR,
				"Failed to get raw classpath for project: " + project.getElementName(), e);
		}
		return null;
	}


	public static Node findEnclosingStatementNode(Node parentNode)
	{
		try
		{
			return XpathUtil.xpathNode(parentNode, "ancestor-or-self::select|ancestor-or-self::update"
				+ "|ancestor-or-self::insert|ancestor-or-self::delete");
		}
		catch (XPathExpressionException e)
		{
			Activator.log(Status.ERROR, e.getMessage(), e);
		}
		return null;
	}

	public static String findEnclosingType(Node node)
	{
		String type = null;
		Node parentNode = node.getParentNode();
		while (parentNode != null && type == null)
		{
			String nodeName = parentNode.getNodeName();
			if ("resultMap".equals(nodeName))
				type = getAttribute(parentNode, "type");
			else if ("collection".equals(nodeName))
				type = getAttribute(parentNode, "ofType");
			else if ("association".equals(nodeName))
				type = getAttribute(parentNode, "javaType");
			else if ("case".equals(nodeName))
				type = getAttribute(parentNode, "resultType");
			parentNode = parentNode.getParentNode();
		}
		return normalizeTypeName(type);
	}

	public static String normalizeTypeName(String src)
	{
		return src == null ? null : src.replace('$', '.');
	}

	private static String getAttribute(Node node, String attributeName)
	{
		NamedNodeMap attributes = node.getAttributes();
		if (attributes != null)
		{
			Node typeAttrNode = attributes.getNamedItem(attributeName);
			if (typeAttrNode != null)
			{
				return typeAttrNode.getNodeValue();
			}
		}
		return null;
	}

	private MybatipseXmlUtil()
	{
	}

	public static String getProjectPath()
	{
		String path = null;
		path = Platform.getLocation().toString();
		return path;
	}

	public static IProject getCurrentProject()
	{
		ISelectionService selectionService = Workbench.getInstance()
			.getActiveWorkbenchWindow()
			.getSelectionService();

		ISelection selection = selectionService.getSelection();

		IProject project = null;
		if (selection instanceof IStructuredSelection)
		{
			Object element = ((IStructuredSelection)selection).getFirstElement();

			if (element instanceof IResource)
			{
				project = ((IResource)element).getProject();
			}
			else if (element instanceof PackageFragmentRootContainer)
			{
				IJavaProject jProject = ((PackageFragmentRootContainer)element).getJavaProject();
				project = jProject.getProject();
			}
			else if (element instanceof IJavaElement)
			{
				IJavaProject jProject = ((IJavaElement)element).getJavaProject();
				project = jProject.getProject();
			}

		}
		return project;
	}

}
