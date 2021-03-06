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
package eu.numberfour.n4js.projectModel;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;

import com.google.common.base.Optional;

import eu.numberfour.n4js.n4mf.ModuleFilter;
import eu.numberfour.n4js.ts.types.TModule;

/**
 * The runtime facade for the n4js model containing the core (UI-free) support for n4js projects.
 * <p>
 * The single instance of this interface can be accessed via dependency injection.
 * </p>
 */
public interface IN4JSCore {

	/**
	 * Returns the N4JS project at the given location.
	 * <p>
	 * Note that no check is done at this time on the existence or structure of the location.
	 * </p>
	 *
	 * @param location
	 *            the project location
	 * @return the n4js project corresponding to the given project, null if the given location is null
	 */
	IN4JSProject create(URI location);

	/**
	 * Returns the N4JS project that contains the element at the given location.
	 *
	 * @param nestedLocation
	 *            the project location
	 * @return the n4js project corresponding to the given project.
	 */
	Optional<? extends IN4JSProject> findProject(URI nestedLocation);

	/**
	 * Returns list of the N4JS projects that are in current working scope (IWorkspace or registered projects).
	 *
	 * @return List containing n4js projects in scope
	 */
	Iterable<IN4JSProject> findAllProjects();

	/**
	 * returns the source container that covers the given location.
	 */
	Optional<? extends IN4JSSourceContainer> findN4JSSourceContainer(URI nestedLocation);

	/**
	 * returns true if for the given URI validation is disabled
	 */
	boolean isNoValidate(URI nestedLocation);

	/**
	 * returns true if for the given URI module wrapping is disabled
	 */
	boolean isNoModuleWrapping(URI nestedLocation);

	/**
	 * returns the project relative path to the folder where the generated files should be placed
	 */
	String getOutputPath(URI nestedLocation);

	/**
	 * returns for the given URI the no-validate module filter
	 */
	ModuleFilter getModuleValidationFilter(URI uri);

	/**
	 * returns for the given URI the no-module-wrapping module filter
	 */
	ModuleFilter getNoModuleWrappingFilter(URI nestedLocation);

	/**
	 * Creates and returns a new resource set that is properly set up for loading resources in the default workspace
	 * represented by the receiving IN4JSCore, i.e. for loading resources contained in the projects returned by
	 * {@link #findAllProjects()}.
	 * <p>
	 * IMPORTANT:
	 * <ul>
	 * <li>This method should <b>NEVER</b> be used in a context where a resource set is already in place, e.g. during
	 * validations use the editor's resource set, within the incremental builder always use the builder's resource set.
	 * <li>This method should <b>ONLY</b> be used to access the
	 * {@link ResourceDescriptionsProvider#PERSISTED_DESCRIPTIONS persisted state} and <b>NEVER</b> in cases where the
	 * dirty state of the editor(s) or the live scope is to be taken into account (in such cases it is very unlikely
	 * that you have to create a new resource set from scratch, so probably you are in the above case and should try to
	 * obtain an existing resource set).
	 * </ul>
	 *
	 * @param contextProject
	 *            provide an N4JS project that defines the context, for example the containing project of the resource
	 *            you want to load. If this is not available, it is ok to pass in Optional.absent() for now (see
	 *            implementation in {@code N4JSEclipseCore#createResourceSet(Optional)} for details).<br>
	 *            Will be ignored in the headless case.
	 */
	ResourceSet createResourceSet(Optional<IN4JSProject> contextProject);

	/**
	 * Returns the Xtext index for the the given resource set. This resource set should be properly set up for Xtext and
	 * N4JS; usually client code should always prefer obtaining an existing resource set from Xtext and only if this is
	 * not available pass in a resource set returned by method {@link #createResourceSet(Optional)}.
	 * <p>
	 * Use with care. If this method is used with a resource set newly created via method
	 * {@link #createResourceSet(Optional)}, then the returned index represents the persisted information, only. All
	 * dirty editors or the currently running build with incrementally compiled resources are ignored.
	 *
	 * @param resourceSet
	 *            properly configured resource set (load options!)
	 */
	IResourceDescriptions getXtextIndex(ResourceSet resourceSet);

	/**
	 * Deserialize the TModule stored in the user data of the Xtext index.
	 */
	TModule loadModuleFromIndex(ResourceSet resourceSet, IResourceDescription resourceDescription,
			boolean allowFullLoad);
}
