package org.activiti.designer.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.packageview.PackageFragmentRootContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

public class GetProjectUtil {
	
	  public static IJavaProject getJavaProject() throws PartInitException{  
		  IProject project = null;  
		  IJavaProject result = null;

	      //1.根据当前编辑器获取工程  
	      IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

	      if(part != null){  
	          Object object = part.getEditorInput().getAdapter(IFile.class);  
	          if(object != null){  
	              project = ((IFile)object).getProject();  
	          }  
	      }  
	        
	      if(project == null){  
	          ISelectionService selectionService =     
	                  Workbench.getInstance().getActiveWorkbenchWindow().getSelectionService();    
	          ISelection selection = selectionService.getSelection();    
	          if(selection instanceof IStructuredSelection) {    
	              Object element = ((IStructuredSelection)selection).getFirstElement();    
	      
	              if (element instanceof IResource) {    
	                  project= ((IResource)element).getProject();    
	              } else if (element instanceof PackageFragmentRootContainer) {    
	                  IJavaProject jProject =     
	                      ((PackageFragmentRootContainer)element).getJavaProject();    
	                  project = jProject.getProject();    
	              } else if (element instanceof IJavaElement) {    
	                  IJavaProject jProject= ((IJavaElement)element).getJavaProject();    
	                  project = jProject.getProject();    
	              } else if(element instanceof EditPart){  
	                  IFile file = (IFile) ((DefaultEditDomain)((EditPart)element).getViewer().getEditDomain()).getEditorPart().getEditorInput().getAdapter(IFile.class);  
	                  project = file.getProject();  
	              }   
	          }     
	      }  
		  result = JavaCore.create(project);

	      return result;  
	  }  
}
