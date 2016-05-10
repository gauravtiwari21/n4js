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
package eu.numberfour.n4js.ui.workingsets;

import static com.google.common.base.Suppliers.memoize;
import static org.eclipse.core.resources.IResource.DEPTH_INFINITE;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.util.Arrays;

import com.google.common.base.Supplier;

import eu.numberfour.n4js.utils.Arrays2;

/**
 * Adapter between {@link WorkingSet N4JS} and {@link IWorkingSet Eclipse based} working sets.
 */
class WorkingSetAdapter extends ResourceMapping implements IWorkingSet, WorkingSet {

	private final WorkingSet delegate;

	private final Supplier<ImageDescriptor> imageDescriptorSupplier;

	WorkingSetAdapter(final WorkingSet delegate) {
		this.delegate = delegate;
		imageDescriptorSupplier = memoize(() -> {
			final Image image = WorkingSetAdapter.this.delegate.getWorkingSetManager().getImage().orNull();
			if (image != null) {
				return ImageDescriptor.createFromImage(image);
			}
			return null;
		});
	}

	@Override
	public String getFactoryId() {
		return null;
	}

	@Override
	public void saveState(final IMemento memento) {
		delegate.getWorkingSetManager().saveState(new NullProgressMonitor());
	}

	@Override
	public <T> T getAdapter(final Class<T> adapter) {
		return delegate.getAdapter(adapter);
	}

	@Override
	public WorkingSetManager getWorkingSetManager() {
		return (WorkingSetManager) PlatformUI.getWorkbench().getWorkingSetManager();
	}

	@Override
	public IAdaptable[] getElements() {
		return delegate.getElements();
	}

	@Override
	public String getId() {
		return delegate.getName();
	}

	@Override
	@SuppressWarnings("deprecation")
	public ImageDescriptor getImage() {
		return getImageDescriptor();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return imageDescriptorSupplier.get();
	}

	@Override
	public String getName() {
		return getId();
	}

	@Override
	public void setElements(final IAdaptable[] elements) {
		// NOOP
	}

	@Override
	public void setId(final String id) {
		// NOOP
	}

	@Override
	public void setName(final String name) {
		// NOOP
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public boolean isVisible() {
		final WorkingSet[] visibleWorkingSets = delegate.getWorkingSetManager().getWorkingSets();
		return Arrays.contains(visibleWorkingSets, delegate);
	}

	@Override
	public String getLabel() {
		return getName();
	}

	@Override
	public void setLabel(final String label) {
		// NOOP
	}

	@Override
	public boolean isSelfUpdating() {
		return false;
	}

	@Override
	public boolean isAggregateWorkingSet() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return delegate.getElements().length == 0;
	}

	@Override
	public IAdaptable[] adaptElements(final IAdaptable[] objects) {
		return null;
	}

	@Override
	public Object getModelObject() {
		return this;
	}

	@Override
	public String getModelProviderId() {
		return null;
	}

	@Override
	public IProject[] getProjects() {
		return Arrays2.filter(getElements(), IProject.class);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor)
			throws CoreException {

		return new ResourceTraversal[] { new ResourceTraversal(getProjects(), DEPTH_INFINITE, 0) };
	}

}
