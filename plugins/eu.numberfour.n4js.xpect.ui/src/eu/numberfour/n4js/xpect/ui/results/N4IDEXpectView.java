/**
 * Copyright (c) 2016 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   NumberFour AG - Initial API and implementation
 */
package eu.numberfour.n4js.xpect.ui.results;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.ViewPart;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import eu.numberfour.n4js.xpect.ui.runner.N4IDEXpectFileNameUtil;

/**
 * Test results viewer. For failed tests can display diff view of expected/actual in
 * org.eclipse.compare.internal.CompareEditor. Additionally generates bug report in special
 * org.eclipse.ui.console.MessageConsole instance.
 *
 * Used to display Xpect tests results, but could/should be adjusted to implement general test view (JUnit like);
 */
public class N4IDEXpectView extends ViewPart {
	Composite container;
	TreeViewer testTreeViewer;
	TraceConsole stacktraceConsole;
	TextConsoleViewer stacktraceConsoleViewer;
	Label stacktraceLabel;

	/** root of the {@link Description} tree, used to display test tree */
	Description dataRoot;
	/** tracks execution status of elements in the test tree */
	ExecutionResults testsExecutionStatus = new ExecutionResults();

	/** This view id, can be used to ask platform UI for this view instances */
	public static final String ID = "eu.numberfour.n4js.xpect.ui.runner.N4IDEXpectView";
	private static final String VIEW_CONTEXT_ID = "eu.numberfour.n4js.xpect.ui.runner.N4IDEXpectView.context";

	@Override
	public void createPartControl(Composite parent) {

		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginHeight = 5;
		fillLayout.marginWidth = 5;
		parent.setLayout(fillLayout);

		// main container
		container = new Composite(parent, SWT.BORDER);
		container.setLayout(new FillLayout());

		// create container for stack trace data
		Composite stacktraceDataContainer = new Composite(parent, SWT.BORDER);

		FormLayout formLayout = new FormLayout();
		formLayout.marginHeight = 5;
		formLayout.marginWidth = 5;
		formLayout.spacing = 5;
		stacktraceDataContainer.setLayout(formLayout);

		Composite stackLabelContainer = new Composite(stacktraceDataContainer, SWT.NO_SCROLL | SWT.SHADOW_NONE);
		stackLabelContainer.setLayout(new GridLayout());

		FormData stackLabelFormData = new FormData();
		stackLabelFormData.top = new FormAttachment(0);
		stackLabelFormData.left = new FormAttachment(0);
		stackLabelFormData.right = new FormAttachment(100);
		stackLabelFormData.bottom = new FormAttachment(10);
		stackLabelContainer.setLayoutData(stackLabelFormData);

		Composite stackTraceContainer = new Composite(stacktraceDataContainer, SWT.NO_SCROLL | SWT.SHADOW_NONE);
		stackTraceContainer.setLayout(new FillLayout());

		stackLabelFormData = new FormData();
		stackLabelFormData.top = new FormAttachment(stackLabelContainer);
		stackLabelFormData.left = new FormAttachment(0);
		stackLabelFormData.right = new FormAttachment(100);
		stackLabelFormData.bottom = new FormAttachment(100);
		stackTraceContainer.setLayoutData(stackLabelFormData);

		// Create viewer for test tree in main container
		testTreeViewer = new TreeViewer(container);
		testTreeViewer.setContentProvider(new XpectContentProvider());
		testTreeViewer.setLabelProvider(new XpectLabelProvider(this.testsExecutionStatus));
		testTreeViewer.setInput(null);

		// create stack trace label
		stacktraceLabel = new Label(stackLabelContainer, SWT.SHADOW_OUT);
		stacktraceLabel.setText("no trace");

		// create stack trace console
		MessageConsole messageConsole = new MessageConsole("trace", null);
		stacktraceConsole = new TraceConsole(messageConsole);
		stacktraceConsoleViewer = new TextConsoleViewer(stackTraceContainer, messageConsole);

		// context menu
		getSite().setSelectionProvider(testTreeViewer);
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		getSite().registerContextMenu(contextMenu, testTreeViewer);
		Control control = testTreeViewer.getControl();
		Menu menu = contextMenu.createContextMenu(control);
		control.setMenu(menu);
		activateContext();

		createSelectionActions();

	}

	/**
	 * Activate a context that this view uses. It will be tied to this view activation events and will be removed when
	 * the view is disposed.
	 *
	 */
	private void activateContext() {
		IContextService contextService = getSite().getService(IContextService.class);
		// this will get cleaned up automatically when the site
		// is disposed
		contextService.activateContext(VIEW_CONTEXT_ID);
	}

	/**
	 * Adds selection listener and handler to the test tree view. On selection of broken test will display failure stack
	 * trace in stack trace part of the view.
	 */
	private void createSelectionActions() {
		testTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {

				if (event.getSelection().isEmpty()) {
					stacktraceConsole.clear();
					stacktraceLabel.setText("no trace");
					stacktraceLabel.getParent().layout(true);
					return;
				}

				stacktraceConsole.clear();
				stacktraceLabel.setText("no trace");
				stacktraceLabel.getParent().layout(true);

				if (event.getSelection() instanceof IStructuredSelection) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();

					Description desc = (Description) selection.getFirstElement();
					// handle failed test
					if (desc.isTest() && testsExecutionStatus.hasFailed(desc)) {
						Throwable failureException = testsExecutionStatus.getFailure(desc).getException();

						// print exception to trace window
						StringWriter stackTraceWriter = new StringWriter();
						failureException.printStackTrace(new PrintWriter(stackTraceWriter));
						String testName = N4IDEXpectFileNameUtil.getTestName(desc);

						// handle xpect run error
						if (testName.startsWith(N4IDEXpectFileNameUtil.TEST_FILE_INIT_ERROR_MSG)) {
							stacktraceLabel.setText("test initializaiton stack trace error");
							stacktraceLabel.getParent().layout(true);
							stacktraceConsole.write(stackTraceWriter.toString());
							return;
						}

						stacktraceLabel.setText("stacktrace of exception for "
								+ testName);
						stacktraceLabel.getParent().layout(true);
						stacktraceConsole.write(stackTraceWriter.toString());
					}
				}
			}
		});
	}

	@Override
	public void setFocus() {
		testTreeViewer.getControl().setFocus();

	}

	/** clears cached data, resets view, resets input data */
	public void notifySessionStarted(Description description) {
		this.dataRoot = description;
		testsExecutionStatus.initResults(description);
		stacktraceLabel.setText("no trace");
		stacktraceLabel.getParent().layout(true);
		stacktraceConsole.clear();
		testTreeViewer.setInput(this.dataRoot);
	}

	/** refreshes view */
	public void notifySessionFinished() {
		testTreeViewer.setInput(this.dataRoot);
		testTreeViewer.setSelection(testTreeViewer.getSelection());
	}

	/** sets given description status to failed, refreshes view */
	public void notifyFailedExecutionOf(Failure failure) {
		testsExecutionStatus.executionFailed(failure);
		testTreeViewer.setInput(this.dataRoot);
	}

	/** sets given description status to started, refreshes view */
	public void notifyStartedExecutionOf(Description description) {
		testsExecutionStatus.executionStarted(description);
		testTreeViewer.setInput(this.dataRoot);
	}

	/** sets given description status to ignored, refreshes view */
	public void notifyIgnoredExecutionOf(Description description) {
		testsExecutionStatus.executionIgnored(description);
		testTreeViewer.setInput(this.dataRoot);
	}

	/** sets given description status to finished, refreshes view */
	public void notifyFinishedExecutionOf(Description description) {
		if (testsExecutionStatus.getStatus(description) == ExecutionStatus.STARTED) {
			testsExecutionStatus.testPassed(description);
		}
		testTreeViewer.setInput(this.dataRoot);
	}
}
