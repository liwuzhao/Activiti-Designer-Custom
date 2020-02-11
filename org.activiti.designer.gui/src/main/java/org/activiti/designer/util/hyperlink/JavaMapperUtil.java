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

import java.util.ArrayList;
import java.util.List;

import org.activiti.designer.Activator;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;


/**
 * @author Iwao AVE!
 */
public class JavaMapperUtil
{
	public static String getAnnotationMemberValue(IAnnotation annotation, String memberName)
	{
		try
		{
			IMemberValuePair[] valuePairs = annotation.getMemberValuePairs();
			for (IMemberValuePair valuePair : valuePairs)
			{
				if (memberName.equals(valuePair.getMemberName()))
				{
					return (String)valuePair.getValue();
				}
			}
		}
		catch (JavaModelException e)
		{
//			Activator.log(Status.ERROR, "Failed to get member value pairs.", e);
		}
		return null;
	}

	public static IAnnotation getAnnotationAt(IAnnotatable annotatable, int offset)
		throws JavaModelException
	{
		IAnnotation[] annotations = annotatable.getAnnotations();
		for (IAnnotation annotation : annotations)
		{
			ISourceRange sourceRange = annotation.getSourceRange();
			if (isInRange(sourceRange, offset))
			{
				return annotation;
			}
		}
		return null;
	}

	private static boolean isInRange(ISourceRange sourceRange, int offset)
	{
		int start = sourceRange.getOffset();
		int end = start + sourceRange.getLength();
		return start <= offset && offset <= end;
	}

	public static void findMapperMethod(MapperMethodStore store, IJavaProject project,
		String mapperFqn, MethodMatcher annotationFilter)
	{
		try
		{
			IType mapperType = project.findType(mapperFqn.replace('$', '.'));
			if (mapperType == null)
				return;
			if (mapperType.isBinary())
			{
				findMapperMethodBinary(store, project, annotationFilter, mapperType);
			}
			else
			{
				findMapperMethodSource(store, project, mapperFqn, annotationFilter, mapperType);
			}
		}
		catch (JavaModelException e)
		{
//			Activator.log(Status.ERROR, "Failed to find type " + mapperFqn, e);
		}
	}

	private static void findMapperMethodSource(MapperMethodStore methodStore,
		IJavaProject project, String mapperFqn, MethodMatcher annotationFilter, IType mapperType)
	{
		ICompilationUnit compilationUnit = (ICompilationUnit)mapperType
			.getAncestor(IJavaElement.COMPILATION_UNIT);
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		// parser.setIgnoreMethodBodies(true);
		CompilationUnit astUnit = (CompilationUnit)parser.createAST(null);
		astUnit.accept(new JavaMapperVisitor(methodStore, project, mapperFqn, annotationFilter));
	}

	private static void findMapperMethodBinary(MapperMethodStore methodStore,
		IJavaProject project, MethodMatcher methodMatcher, IType mapperType)
		throws JavaModelException
	{
		for (IMethod method : mapperType.getMethods())
		{
			if (methodMatcher.matches(method))
			{
				methodStore.add(method);
			}
		}

		String[] superInterfaces = mapperType.getSuperInterfaceNames();
		for (String superInterface : superInterfaces)
		{
			if (!Object.class.getName().equals(superInterface))
			{
				findMapperMethod(methodStore, project, superInterface, methodMatcher);
			}
		}
	}

	public static class JavaMapperVisitor extends ASTVisitor
	{
		private MapperMethodStore methodStore;

		private IJavaProject project;

		private String mapperFqn;

		private MethodMatcher methodMatcher;

		private int nestLevel;

		@Override
		public boolean visit(TypeDeclaration node)
		{
			ITypeBinding binding = node.resolveBinding();
			if (binding == null)
				return false;

			if (mapperFqn.equals(binding.getBinaryName()))
				nestLevel = 1;
			else if (nestLevel > 0)
				nestLevel++;

			return true;
		}

		@Override
		public boolean visit(AnonymousClassDeclaration node)
		{
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node)
		{
			if (nestLevel != 1)
				return false;
			// Resolve binding first to support Lombok generated methods.
			// node.getModifiers() returns incorrect access modifiers for them.
			// https://github.com/harawata/stlipse/issues/2
			IMethodBinding method = node.resolveBinding();
			if (method == null)
				return false;

			if (methodMatcher == null)
				return false;

			try
			{
				if (methodMatcher.matches(method))
				{
					@SuppressWarnings("unchecked")
					List<SingleVariableDeclaration> params = node.parameters();
					methodStore.add(method, params);
				}
			}
			catch (JavaModelException e)
			{
//				Activator.log(Status.ERROR,
//					"Failed to visit method " + node.getName().toString() + " in " + mapperFqn, e);
			}
			return false;
		}

		public void endVisit(TypeDeclaration node)
		{
			if (nestLevel == 1 && (!methodMatcher.needExactMatch() || methodStore.isEmpty()))
			{
				@SuppressWarnings("unchecked")
				List<Type> superInterfaceTypes = node.superInterfaceTypes();
				if (superInterfaceTypes != null && !superInterfaceTypes.isEmpty())
				{
					for (Type superInterfaceType : superInterfaceTypes)
					{
						ITypeBinding binding = superInterfaceType.resolveBinding();
						if (binding != null)
						{
							String superInterfaceFqn = binding.getQualifiedName();
							if (binding.isParameterizedType())
							{
								// strip parameter part
								int paramIdx = superInterfaceFqn.indexOf('<');
								superInterfaceFqn = superInterfaceFqn.substring(0, paramIdx);
							}
							findMapperMethod(methodStore, project, superInterfaceFqn, methodMatcher);
						}
					}
				}
			}
			nestLevel--;
		}

		private JavaMapperVisitor(
			MapperMethodStore methodStore,
			IJavaProject project,
			String mapperFqn,
			MethodMatcher annotationFilter)
		{
			this.methodStore = methodStore;
			this.project = project;
			this.mapperFqn = mapperFqn;
			this.methodMatcher = annotationFilter;
		}
	}

	public static interface MapperMethodStore
	{
		/**
		 * Called when adding binary method.
		 */
		void add(IMethod method);

		/**
		 * Called when adding source method.
		 */
		void add(IMethodBinding method, List<SingleVariableDeclaration> params);

		boolean isEmpty();
	}

	public static class SingleMethodStore implements MapperMethodStore
	{
		private IMethod method;

		public IMethod getMethod()
		{
			return this.method;
		}

		@Override
		public void add(IMethod method)
		{
			this.method = method;
		}

		@Override
		public void add(IMethodBinding method, List<SingleVariableDeclaration> params)
		{
			this.method = (IMethod)method.getJavaElement();
		}

		@Override
		public boolean isEmpty()
		{
			return method == null;
		}
	}

	public static class MethodNameStore implements MapperMethodStore
	{
		private List<String> methodNames = new ArrayList<String>();

		public List<String> getMethodNames()
		{
			return methodNames;
		}

		@Override
		public void add(IMethod method)
		{
			methodNames.add(method.getElementName());
		}

		@Override
		public void add(IMethodBinding method, List<SingleVariableDeclaration> params)
		{
			methodNames.add(method.getName());
		}

		@Override
		public boolean isEmpty()
		{
			return methodNames.isEmpty();
		}
	}

	public static class MethodReturnTypeStore implements MapperMethodStore
	{
		private String returnType = null;

		public String getReturnType()
		{
			return returnType;
		}

		@Override
		public void add(IMethod method)
		{
			try
			{
				returnType = method.getReturnType();
			}
			catch (JavaModelException e)
			{
				Activator.log(Status.ERROR,
					"Failed to collect return type of method " + method.getElementName() + " in "
						+ method.getDeclaringType().getFullyQualifiedName(),
					e);
			}
		}

		@Override
		public void add(IMethodBinding method, List<SingleVariableDeclaration> params)
		{
			ITypeBinding binding = method.getReturnType();
			returnType = binding.getQualifiedName();
		}

		@Override
		public boolean isEmpty()
		{
			return returnType == null;
		}
	}


	public static abstract class MethodMatcher
	{
		abstract boolean matches(IMethod method) throws JavaModelException;

		abstract boolean matches(IMethodBinding method) throws JavaModelException;

		abstract boolean needExactMatch();

		protected boolean nameMatches(String elementId, String matchString, boolean exactMatch)
		{
			if (exactMatch)
			{
				return elementId.equals(matchString);
			}
			else
			{
				return matchString.length() == 0
					|| CharOperation.camelCaseMatch(matchString.toCharArray(), elementId.toCharArray());
			}
		}
	}

	public static class MethodNameMatcher extends MethodMatcher
	{
		private String matchString;

		private boolean exactMatch;

		public MethodNameMatcher(String matchString, boolean exactMatch)
		{
			super();
			this.matchString = matchString;
			this.exactMatch = exactMatch;
		}

		@Override
		public boolean matches(IMethod method) throws JavaModelException
		{
			return nameMatches(method.getElementName(), matchString, exactMatch);
		}

		@Override
		public boolean matches(IMethodBinding method) throws JavaModelException
		{
			return nameMatches(method.getName(), matchString, exactMatch);
		}

		@Override
		boolean needExactMatch()
		{
			return exactMatch;
		}
	}

	public static class RejectStatementAnnotation extends MethodMatcher
	{
		private String matchString;

		private boolean exactMatch;

		public RejectStatementAnnotation(String matchString, boolean exactMatch)
		{
			super();
			this.matchString = matchString;
			this.exactMatch = exactMatch;
		}

		@Override
		public boolean matches(IMethod method) throws JavaModelException
		{
			return nameMatches(method.getElementName(), matchString, exactMatch);
		}

		@Override
		public boolean matches(IMethodBinding method) throws JavaModelException
		{
			return nameMatches(method.getName(), matchString, exactMatch);
		}

		@Override
		boolean needExactMatch()
		{
			return exactMatch;
		}
	}



}
