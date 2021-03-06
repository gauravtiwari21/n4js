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
package eu.numberfour.n4js.postprocessing;

import java.util.List;

import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.OnChangeEvictingCache;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import eu.numberfour.n4js.n4JS.ParameterizedCallExpression;
import eu.numberfour.n4js.resource.N4JSResource;
import eu.numberfour.n4js.ts.typeRefs.TypeRef;
import eu.numberfour.n4js.ts.types.TypableElement;
import eu.numberfour.n4js.typesystem.N4JSTypeSystem;
import it.xsemantics.runtime.Result;
import it.xsemantics.runtime.RuleEnvironment;

/**
 * Front-end for accessing the {@link ASTMetaInfoCache}. Should have a public getter for each public getter in
 * {@code ASTMetaInfoCache}.
 */
@Singleton
public final class ASTMetaInfoCacheHelper {

	private static final boolean DEBUG_TRACK_CACHE_CREATION_DELETION = false;

	@Inject
	private IResourceScopeCache resourceScopeCacheHelper;

	/**
	 * <b>IMPORTANT:</b> most clients should use {@link N4JSTypeSystem#type(RuleEnvironment, TypableElement)} instead!
	 * <p>
	 * Convenience method for {@link ASTMetaInfoCache#getTypeFailSafe(TypableElement)}.
	 */
	public Result<TypeRef> getTypeFailSafe(TypableElement astNode) {
		return getOrCreate((N4JSResource) astNode.eResource()).getTypeFailSafe(astNode);
	}

	/**
	 * Convenience method for {@link ASTMetaInfoCache#getInferredTypeArgs(ParameterizedCallExpression)}.
	 */
	public List<TypeRef> getInferredTypeArgs(ParameterizedCallExpression callExpr) {
		return getOrCreate((N4JSResource) callExpr.eResource()).getInferredTypeArgs(callExpr);
	}

	/**
	 * Returns the {@link ASTMetaInfoCache} of the given resource, optionally creating it if it does not exist already.
	 */
	public ASTMetaInfoCache getOrCreate(N4JSResource res) {
		return resourceScopeCacheHelper.get(ASTMetaInfoCache.class, res, () -> {
			final ASTMetaInfoCache newCache = new ASTMetaInfoCache(res);

			// DEBUG: use the following code to track cache creation/deletion
			if (DEBUG_TRACK_CACHE_CREATION_DELETION) {
				final String newCacheId = Integer.toHexString(newCache.hashCode());
				System.out.println("!! creating new cache " + newCacheId
						+ " (on resource " + Integer.toHexString(res.hashCode()) + "; URI: " + res.getURI() + ")");
				((OnChangeEvictingCache) resourceScopeCacheHelper).getOrCreate(res).addCacheListener((cacheAdapter) -> {
					if (!newCache.isEmpty()) {
						System.out.println("!!!! clearing non-empty cache " + newCacheId);
					} else {
						System.out.println("!!!! clearing empty cache " + newCacheId);
					}
					if (newCache.isProcessingInProgress()) {
						// DEBUG: good place for a break point when hunting down an accidental cache clear
						System.out.println("!!!! WARNING suspicious cache clear (cache " + newCacheId + ")");
					}
				});
			}

			return newCache;
		});
	}
}
