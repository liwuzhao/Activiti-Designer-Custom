/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.designer.property;

import org.activiti.bpmn.model.UserTask;
import org.activiti.designer.util.GetProjectUtil;
import org.activiti.designer.util.hyperlink.HyperlinkDetector;
import org.activiti.designer.util.hyperlink.JavaMapperUtil.RejectStatementAnnotation;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;


public class PropertyUserTaskSection extends ActivitiPropertySection implements ITabbedPropertyConstants {
	  
  protected TableViewer tableViewer;
  protected Text assigneeText;
  protected Text candidateUsersText;
  protected Text candidateGroupsText;
  protected Text formKeyText;
  protected Text dueDateText;
  protected Text priorityText;
  protected Text categoryText;
  protected Text skipExpressionText;
  protected Button openButton;
  
  protected Combo methodCombo;
  // TODO: 从某个文件中取值
  private String[] methodValues = new String[] {"com.Test.testMe", "com.Test.testA"};

  @Override
  public void createFormControls(TabbedPropertySheetPage aTabbedPropertySheetPage) {
	// addTable();
	addLabels();
	addButtons();
    addActions();

  }	

  protected void addLabels() {
	    assigneeText = createTextControl(false);
	    createLabel("Assignee", assigneeText);
	    candidateUsersText = createTextControl(false);
	    createLabel("Candidate users (comma separated)", candidateUsersText);
	    candidateGroupsText = createTextControl(false);
	    createLabel("Candidate groups (comma separated)", candidateGroupsText);
	    formKeyText = createTextControl(false);
	    createLabel("Form key", formKeyText);
	    dueDateText = createTextControl(false);
	    createLabel("Due date (variable)", dueDateText);
	    priorityText = createTextControl(false);
	    createLabel("Priority", priorityText);
	    categoryText = createTextControl(false);
	    createLabel("Category", categoryText);
	    skipExpressionText = createTextControl(false);
	    createLabel("Skip expression", skipExpressionText);
	    methodCombo = createCombobox(methodValues, 1);
		createLabel("Method", methodCombo);
}
  protected void addButtons() {
	    Composite buttonComposite = new Composite(formComposite, SWT.NONE);
	    GridLayout layout = new GridLayout(1, true);
	    buttonComposite.setBackground(formComposite.getBackground());
	    buttonComposite.setLayout(layout);
	    FormData data = new FormData();
	    data.top = createTopFormAttachment();
//	    data.left = new FormAttachment(tableViewer.getControl());
	    data.right = new FormAttachment(100, 0);
	    buttonComposite.setLayoutData(data);
	    
	    openButton = new Button(buttonComposite, SWT.PUSH);
	    openButton.setText("open");
	    openButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
  }
  
  protected void addActions() {

	  openButton.addSelectionListener(new SelectionAdapter() {
	      @Override
	      public void widgetSelected(SelectionEvent e) {
	    	  try {
	    		// 获取当前 project
	    		String methodFullPath = methodValues[methodCombo.getSelectionIndex()];
	    		String splits[] = methodFullPath.split("\\.");
	    	    String methodName = splits[splits.length - 1];
	    	    
	    		String packageName = methodFullPath.replace("." + methodName, "");

	    		
 				IJavaProject curProject = GetProjectUtil.getJavaProject();
				// 创建 hyperlink 对象
				IHyperlink hyperlink = HyperlinkDetector.linkToJavaMapperMethod(curProject,
						packageName,
						new Region(0, 0),
						new RejectStatementAnnotation(methodName, true));
				hyperlink.open();
			} catch (PartInitException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	      }
	  });

  }
  
  @Override
  protected Object getModelValueForControl(Control control, Object businessObject) {
    UserTask task = (UserTask) businessObject;
    if (control == assigneeText) {
      return task.getAssignee();
    } else if (control == candidateUsersText) {
      return getCommaSeperatedString(task.getCandidateUsers());
    } else if (control == candidateGroupsText) {
      return getCommaSeperatedString(task.getCandidateGroups());
    } else if (control == formKeyText) {
      return task.getFormKey();
    } else if (control == dueDateText) {
      return task.getDueDate();
    } else if (control == priorityText) {
      return task.getPriority();
    } else if (control == categoryText) {
      return task.getCategory();
    } else if (control == skipExpressionText) {
      return task.getSkipExpression();
    }
    return null;
  }

  @Override
  protected void storeValueInModel(Control control, Object businessObject) {
    UserTask task = (UserTask) businessObject;
    if (control == assigneeText) {
      task.setAssignee(assigneeText.getText());
    } else if (control == candidateUsersText) {
      updateCandidates(task, candidateUsersText);
    } else if (control == candidateGroupsText) {
      updateCandidates(task, candidateGroupsText);
    } else if (control == formKeyText) {
      task.setFormKey(formKeyText.getText());
    } else if (control == dueDateText) {
      task.setDueDate(dueDateText.getText());
    } else if (control == priorityText) {
      task.setPriority(priorityText.getText());
    } else if (control == categoryText) {
      task.setCategory(categoryText.getText());
    } else if (control == skipExpressionText) {
      task.setSkipExpression(skipExpressionText.getText());
    }
  }
  
  protected void updateCandidates(UserTask userTask, Object source) {
    String candidates = ((Text) source).getText();
    
    if (source == candidateUsersText) {
      userTask.getCandidateUsers().clear();
    } else {
      userTask.getCandidateGroups().clear();
    }
    
    if (StringUtils.isNotEmpty(candidates)) {
      String[] expressionList = null;
      if (candidates.contains(",")) {
        expressionList = candidates.split(",");
      } else {
        expressionList = new String[] { candidates };
      }
      
      for (String user : expressionList) {
        if (source == candidateUsersText) {
          userTask.getCandidateUsers().add(user.trim());
        } else {
          userTask.getCandidateGroups().add(user.trim());
        }
      }
    }
  }
}
