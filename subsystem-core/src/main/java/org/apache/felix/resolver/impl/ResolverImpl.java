/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;

import org.apache.aries.subsystem.core.Environment;
import org.apache.aries.subsystem.core.ResolutionException;
import org.apache.felix.resolver.FelixCapability;
import org.apache.felix.resolver.FelixEnvironment;
import org.apache.felix.resolver.FelixResolver;
import org.apache.felix.resolver.Logger;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.AbstractNamespace;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wire;
import org.osgi.framework.resource.Wiring;

public class ResolverImpl implements FelixResolver
{
    private final Logger m_logger;

    // Holds candidate permutations based on permutating "uses" chains.
    // These permutations are given higher priority.
    private final List<Candidates> m_usesPermutations = new ArrayList<Candidates>();
    // Holds candidate permutations based on permutating requirement candidates.
    // These permutations represent backtracking on previous decisions.
    private final List<Candidates> m_importPermutations = new ArrayList<Candidates>();

    public ResolverImpl(Logger logger)
    {
        m_logger = (logger != null) ? logger : new Logger() {

            public void log(int level, String msg)
            {
                // Just eat the log message.
            }

            public void log(int level, String msg, Throwable throwable)
            {
                // Just eat the log message.
            }
        };
    }

    public Map<Resource, List<Wire>> resolve(
        Environment env,
        Collection<? extends Resource> mandatoryRevisions,
        Collection<? extends Resource> optionalRevisions)
    {
        return resolve(env instanceof FelixEnvironment ? (FelixEnvironment)env : new EnvironmentAdaptor(env), mandatoryRevisions, optionalRevisions, Collections.EMPTY_SET);
    }

    public Map<Resource, List<Wire>> resolve(
        FelixEnvironment env,
        Collection<? extends Resource> mandatoryRevisions,
        Collection<? extends Resource> optionalRevisions,
        Collection<? extends Resource> ondemandFragments)
    {
        Map<Resource, List<Wire>> wireMap = new HashMap<Resource, List<Wire>>();
        Map<Resource, Packages> revisionPkgMap = new HashMap<Resource, Packages>();

        boolean retry;
        do
        {
            retry = false;

            try
            {
                // Create object to hold all candidates.
                Candidates allCandidates = new Candidates();

                // Populate mandatory revisions; since these are mandatory
                // revisions, failure throws a resolve exception.
                for (Iterator<? extends Resource> it = mandatoryRevisions.iterator();
                    it.hasNext(); )
                {
                    Resource br = it.next();
                    if (Util.isFragment(br) || !env.getWirings().containsKey(br))
                    {
                        allCandidates.populate(env, br, Candidates.MANDATORY);
                    }
                    else
                    {
                        it.remove();
                    }
                }

                // Populate optional revisions; since these are optional
                // revisions, failure does not throw a resolve exception.
                for (Resource br : optionalRevisions)
                {
                    boolean isFragment = Util.isFragment(br);
                    if (isFragment || !env.getWirings().containsKey(br))
                    {
                        allCandidates.populate(env, br, Candidates.OPTIONAL);
                    }
                }

                // Populate ondemand fragments; since these are optional
                // revisions, failure does not throw a resolve exception.
                for (Resource br : ondemandFragments)
                {
                    boolean isFragment = Util.isFragment(br);
                    if (isFragment)
                    {
                        allCandidates.populate(env, br, Candidates.ON_DEMAND);
                    }
                }

                // Merge any fragments into hosts.
                allCandidates.prepare();

                // Create a combined list of populated revisions; for
                // optional revisions. We do not need to consider ondemand
                // fragments, since they will only be pulled in if their
                // host is already present.
                Set<Resource> allRevisions =
                    new HashSet<Resource>(mandatoryRevisions);
                for (Resource br : optionalRevisions)
                {
                    if (allCandidates.isPopulated(br))
                    {
                        allRevisions.add(br);
                    }
                }

                // Record the initial candidate permutation.
                m_usesPermutations.add(allCandidates);

                ResolutionException rethrow = null;

                // If a populated revision is a fragment, then its host
                // must ultimately be verified, so store its host requirement
                // to use for package space calculation.
                Map<Resource, List<Requirement>> hostReqs =
                    new HashMap<Resource, List<Requirement>>();
                for (Resource br : allRevisions)
                {
                    if (Util.isFragment(br))
                    {
                        hostReqs.put(
                            br,
                            br.getRequirements(HostNamespace.HOST_NAMESPACE));
                    }
                }

                do
                {
                    rethrow = null;

                    revisionPkgMap.clear();
                    m_packageSourcesCache.clear();

                    allCandidates = (m_usesPermutations.size() > 0)
                        ? m_usesPermutations.remove(0)
                        : m_importPermutations.remove(0);
//allCandidates.dump();

                    for (Resource br : allRevisions)
                    {
                        Resource target = br;

                        // If we are resolving a fragment, then get its
                        // host candidate and verify it instead.
                        List<Requirement> hostReq = hostReqs.get(br);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getResource();
                        }

                        calculatePackageSpaces(
                            env, allCandidates.getWrappedHost(target), allCandidates,
                            revisionPkgMap, new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpRevisionPkgMap(revisionPkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                env, false, allCandidates.getWrappedHost(target),
                                allCandidates, revisionPkgMap, new HashMap());
                        }
                        catch (ResolutionException ex)
                        {
                            rethrow = ex;
                        }
                    }
                }
                while ((rethrow != null)
                    && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                // If there is a resolve exception, then determine if an
                // optionally resolved revision is to blame (typically a fragment).
                // If so, then remove the optionally resolved resolved and try
                // again; otherwise, rethrow the resolve exception.
                if (rethrow != null)
                {
                    Collection<Requirement> exReqs = rethrow.getUnresolvedRequirements();
                    Requirement faultyReq = ((exReqs == null) || exReqs.isEmpty())
                        ? null : exReqs.iterator().next();
                    Resource faultyRevision = (faultyReq == null)
                        ? null : getActualResource(faultyReq.getResource());
                    if (faultyReq instanceof HostedRequirement)
                    {
                        faultyRevision =
                            ((HostedRequirement) faultyReq)
                                .getOriginalRequirement().getResource();
                    }
                    if (optionalRevisions.remove(faultyRevision))
                    {
                        retry = true;
                    }
                    else if (ondemandFragments.remove(faultyRevision))
                    {
                        retry = true;
                    }
                    else
                    {
                        throw rethrow;
                    }
                }
                // If there is no exception to rethrow, then this was a clean
                // resolve, so populate the wire map.
                else
                {
                    for (Resource br : allRevisions)
                    {
                        Resource target = br;

                        // If we are resolving a fragment, then we
                        // actually want to populate its host's wires.
                        List<Requirement> hostReq = hostReqs.get(br);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getResource();
                        }

                        if (allCandidates.isPopulated(target))
                        {
                            wireMap =
                                populateWireMap(
                                    env, allCandidates.getWrappedHost(target),
                                    revisionPkgMap, wireMap, allCandidates);
                        }
                    }
                }
            }
            finally
            {
                // Always clear the state.
                m_usesPermutations.clear();
                m_importPermutations.clear();
            }
        }
        while (retry);

        return wireMap;
    }

// TODO: RFC-112 - This method should replace the following method as the proper
//       way to do dynamic imports. This leaves the verification and requirement
//       synthesization to the environment. The resolver just has to verify that
//       the candidate doesn't result in a conflict with an existing package.
    public Map<Resource, List<Wire>> resolve(
        FelixEnvironment env, Resource resource, Requirement req, SortedSet<Capability> candidates,
        Collection<? extends Resource> ondemandFragments)
    {
        if (env.getWirings().containsKey(resource) && !candidates.isEmpty())
        {
            Candidates allCandidates = new Candidates();
            allCandidates.populateDynamic(env, resource, req, candidates);

            Map<Resource, List<Wire>> wireMap = new HashMap<Resource, List<Wire>>();
            Map<Resource, Packages> resourcePkgMap = new HashMap<Resource, Packages>();

            boolean retry;
            do
            {
                retry = false;

                try
                {
                    // Try to populate optional fragments.
                    for (Resource br : ondemandFragments)
                    {
                        if (Util.isFragment(br))
                        {
                            allCandidates.populate(env, br, Candidates.ON_DEMAND);
                        }
                    }

                    // Merge any fragments into hosts.
                    allCandidates.prepare();

                    // Record the initial candidate permutation.
                    m_usesPermutations.add(allCandidates);

                    ResolutionException rethrow = null;

                    do
                    {
                        rethrow = null;

                        resourcePkgMap.clear();
                        m_packageSourcesCache.clear();

                        allCandidates = (m_usesPermutations.size() > 0)
                            ? m_usesPermutations.remove(0)
                            : m_importPermutations.remove(0);
//allCandidates.dump();

                        // For a dynamic import, the instigating revision
                        // will never be a fragment since fragments never
                        // execute code, so we don't need to check for
                        // this case like we do for a normal resolve.

                        calculatePackageSpaces(
                            env, resource, allCandidates,
                            resourcePkgMap, new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpRevisionPkgMap(revisionPkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                env, false, resource,
                                allCandidates, resourcePkgMap, new HashMap());
                        }
                        catch (ResolutionException ex)
                        {
                            rethrow = ex;
                        }
                    }
                    while ((rethrow != null)
                        && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                    // If there is a resolve exception, then determine if an
                    // optionally resolved revision is to blame (typically a fragment).
                    // If so, then remove the optionally resolved revision and try
                    // again; otherwise, rethrow the resolve exception.
                    if (rethrow != null)
                    {
                        Collection<Requirement> exReqs = rethrow.getUnresolvedRequirements();
                        Requirement faultyReq = ((exReqs == null) || exReqs.isEmpty())
                            ? null : exReqs.iterator().next();
                        Resource faultyResource = null;
                        if (faultyReq instanceof HostedRequirement)
                        {
                            faultyResource =
                                ((HostedRequirement) faultyReq)
                                    .getOriginalRequirement().getResource();
                        }
                        if (ondemandFragments.remove(faultyResource))
                        {
                            retry = true;
                        }
                        else
                        {
                            throw rethrow;
                        }
                    }
                    // If there is no exception to rethrow, then this was a clean
                    // resolve, so populate the wire map.
                    else
                    {
                        wireMap = populateDynamicWireMap(
                            env, resource, req, resourcePkgMap, wireMap, allCandidates);
                        return wireMap;
                    }
                }
                finally
                {
                    // Always clear the state.
                    m_usesPermutations.clear();
                    m_importPermutations.clear();
                }
            }
            while (retry);
        }

        return null;
    }

    private void calculatePackageSpaces(
        Environment env,
        Resource resource,
        Candidates allCandidates,
        Map<Resource, Packages> revisionPkgMap,
        Map<Capability, List<Resource>> usesCycleMap,
        Set<Resource> cycle)
    {
        if (cycle.contains(resource))
        {
            return;
        }
        cycle.add(resource);

        // Create parallel arrays for requirement and proposed candidate
        // capability or actual capability if revision is resolved or not.
        List<Requirement> reqs = new ArrayList();
        List<Capability> caps = new ArrayList();
        boolean isDynamicImporting = false;
        Wiring wiring = env.getWirings().get(resource);
        if (wiring != null)
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (Wire wire : wiring.getRequiredResourceWires(null))
            {
                // Wrap the requirement as a hosted requirement if it comes
                // from a fragment, since we will need to know the host. We
                // also need to wrap if the requirement is a dynamic import,
                // since that requirement will be shared with any other
                // matching dynamic imports.
                Requirement r = wire.getRequirement();
                if (!r.getResource().equals(wire.getRequirer())
                    || ((r.getDirectives()
                            .get(AbstractNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE) != null)
// TODO: RFC-112 - Need dynamic constant.
                        && r.getDirectives()
                            .get(AbstractNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE).equals("dynamic")))
                {
                    r = new HostedRequirement(wire.getRequirer(), r);
                }
                // Wrap the capability as a hosted capability if it comes
                // from a fragment, since we will need to know the host.
                Capability c = wire.getCapability();
                if (!c.getResource().equals(wire.getProvider()))
                {
                    c = new HostedCapability(wire.getProvider(), c);
                }
                reqs.add(r);
                caps.add(c);
            }

            // Since the revision is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            for (Requirement req
                : Util.getDynamicRequirements(wiring.getResourceRequirements(null)))
            {
                // Get the candidates for the current requirement.
                SortedSet<Capability> candCaps = allCandidates.getCandidates(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                Capability cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
                isDynamicImporting = true;
                // Can only dynamically import one at a time, so break
                // out of the loop after the first.
                break;
            }
        }
        else
        {
            for (Requirement req : resource.getRequirements(null))
            {
                String resolution = req.getDirectives()
                    .get(AbstractNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
// TODO: RFC-112 - Need dynamic constant.
                if ((resolution == null) || !resolution.equals("dynamic"))
                {
                    // Get the candidates for the current requirement.
                    SortedSet<Capability> candCaps = allCandidates.getCandidates(req);
                    // Optional requirements may not have any candidates.
                    if (candCaps == null)
                    {
                        continue;
                    }

                    Capability cap = candCaps.iterator().next();
                    reqs.add(req);
                    caps.add(cap);
                }
            }
        }

        // First, add all exported packages to the target revision's package space.
        calculateExportedPackages(env, resource, allCandidates, revisionPkgMap);
        Packages revisionPkgs = revisionPkgMap.get(resource);

        // Second, add all imported packages to the target revision's package space.
        for (int i = 0; i < reqs.size(); i++)
        {
            Requirement req = reqs.get(i);
            Capability cap = caps.get(i);
            calculateExportedPackages(env, cap.getResource(), allCandidates, revisionPkgMap);
            mergeCandidatePackages(
                env, resource, req, cap, revisionPkgMap, allCandidates,
                new HashMap<Resource, List<Capability>>());
        }

        // Third, have all candidates to calculate their package spaces.
        for (int i = 0; i < caps.size(); i++)
        {
            calculatePackageSpaces(
                env, caps.get(i).getResource(), allCandidates, revisionPkgMap,
                usesCycleMap, cycle);
        }

        // Fourth, if the target revision is unresolved or is dynamically importing,
        // then add all the uses constraints implied by its imported and required
        // packages to its package space.
        // NOTE: We do not need to do this for resolved revisions because their
        // package space is consistent by definition and these uses constraints
        // are only needed to verify the consistency of a resolving revision. The
        // only exception is if a resolved revision is dynamically importing, then
        // we need to calculate its uses constraints again to make sure the new
        // import is consistent with the existing package space.
        if ((wiring == null) || isDynamicImporting)
        {
            // Merge uses constraints from required capabilities.
            for (int i = 0; i < reqs.size(); i++)
            {
                Requirement req = reqs.get(i);
                Capability cap = caps.get(i);
                // Ignore bundle/package requirements, since they are
                // considered below.
                if (!req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)
                    && !req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    List<Requirement> blameReqs = new ArrayList();
                    blameReqs.add(req);

                    mergeUses(
                        env,
                        resource,
                        revisionPkgs,
                        cap,
                        blameReqs,
                        revisionPkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
            // Merge uses constraints from imported packages.
            for (Entry<String, List<Blame>> entry : revisionPkgs.m_importedPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    // Ignore revisions that import from themselves.
                    if (!blame.m_cap.getResource().equals(resource))
                    {
                        List<Requirement> blameReqs = new ArrayList();
                        blameReqs.add(blame.m_reqs.get(0));

                        mergeUses(
                            env,
                            resource,
                            revisionPkgs,
                            blame.m_cap,
                            blameReqs,
                            revisionPkgMap,
                            allCandidates,
                            usesCycleMap);
                    }
                }
            }
            // Merge uses constraints from required bundles.
            for (Entry<String, List<Blame>> entry : revisionPkgs.m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    List<Requirement> blameReqs = new ArrayList();
                    blameReqs.add(blame.m_reqs.get(0));

                    mergeUses(
                        env,
                        resource,
                        revisionPkgs,
                        blame.m_cap,
                        blameReqs,
                        revisionPkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
        }
    }

    private void mergeCandidatePackages(
        Environment env, Resource current, Requirement currentReq,
        Capability candCap, Map<Resource, Packages> revisionPkgMap,
        Candidates allCandidates, Map<Resource, List<Capability>> cycles)
    {
        List<Capability> cycleCaps = cycles.get(current);
        if (cycleCaps == null)
        {
            cycleCaps = new ArrayList<Capability>();
            cycles.put(current, cycleCaps);
        }
        if (cycleCaps.contains(candCap))
        {
            return;
        }
        cycleCaps.add(candCap);

        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, currentReq, candCap, revisionPkgMap);
        }
        else if (candCap.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
        {
// TODO: FELIX3 - THIS NEXT LINE IS A HACK. IMPROVE HOW/WHEN WE CALCULATE EXPORTS.
            calculateExportedPackages(
                env, candCap.getResource(), allCandidates, revisionPkgMap);

            // Get the candidate's package space to determine which packages
            // will be visible to the current revision.
            Packages candPkgs = revisionPkgMap.get(candCap.getResource());

            // We have to merge all exported packages from the candidate,
            // since the current revision requires it.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    currentReq,
                    entry.getValue().m_cap,
                    revisionPkgMap);
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            Wiring candWiring = env.getWirings().get(candCap.getResource());
            if (candWiring != null)
            {
                for (Wire bw : candWiring.getRequiredResourceWires(null))
                {
                    if (bw.getRequirement().getNamespace()
                        .equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        String value = bw.getRequirement()
                            .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(Constants.VISIBILITY_REEXPORT))
                        {
                            mergeCandidatePackages(
                                env,
                                current,
                                currentReq,
                                bw.getCapability(),
                                revisionPkgMap,
                                allCandidates,
                                cycles);
                        }
                    }
                }
            }
            else
            {
                for (Requirement req
                    : candCap.getResource().getRequirements(null))
                {
                    if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        String value =
                            req.getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(Constants.VISIBILITY_REEXPORT)
                            && (allCandidates.getCandidates(req) != null))
                        {
                            mergeCandidatePackages(
                                env,
                                current,
                                currentReq,
                                allCandidates.getCandidates(req).iterator().next(),
                                revisionPkgMap,
                                allCandidates,
                                cycles);
                        }
                    }
                }
            }
        }

        cycles.remove(current);
    }

    private void mergeCandidatePackage(
        Resource current, boolean requires,
        Requirement currentReq, Capability candCap,
        Map<Resource, Packages> revisionPkgMap)
    {
        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            // Merge the candidate capability into the revision's package space
            // for imported or required packages, appropriately.

            String pkgName = (String)
                candCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);

            List blameReqs = new ArrayList();
            blameReqs.add(currentReq);

            Packages currentPkgs = revisionPkgMap.get(current);

            Map<String, List<Blame>> packages = (requires)
                ? currentPkgs.m_requiredPkgs
                : currentPkgs.m_importedPkgs;
            List<Blame> blames = packages.get(pkgName);
            if (blames == null)
            {
                blames = new ArrayList<Blame>();
                packages.put(pkgName, blames);
            }
            blames.add(new Blame(candCap, blameReqs));

//dumpRevisionPkgs(current, currentPkgs);
        }
    }

    private void mergeUses(
        Environment env, Resource current, Packages currentPkgs,
        Capability mergeCap, List<Requirement> blameReqs,
        Map<Resource, Packages> revisionPkgMap,
        Candidates allCandidates,
        Map<Capability, List<Resource>> cycleMap)
    {
        // If there are no uses, then just return.
        // If the candidate revision is the same as the current revision,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        if (current.equals(mergeCap.getResource()))
        {
            return;
        }

        // Check for cycles.
        List<Resource> list = cycleMap.get(mergeCap);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<Resource>() : list;
        list.add(current);
        cycleMap.put(mergeCap, list);

        for (Capability candSourceCap : getPackageSources(env, mergeCap, revisionPkgMap))
        {
            List<String> uses;
            if (candSourceCap instanceof FelixCapability)
            {
                uses = ((FelixCapability) candSourceCap).getUses();
            }
            else
            {
                uses = Collections.EMPTY_LIST;
                String s = candSourceCap.getDirectives()
                    .get(AbstractNamespace.CAPABILITY_USES_DIRECTIVE);
                if (s != null)
                {
                    // Parse these uses directive.
                    StringTokenizer tok = new StringTokenizer(s, ",");
                    uses = new ArrayList(tok.countTokens());
                    while (tok.hasMoreTokens())
                    {
                        uses.add(tok.nextToken().trim());
                    }
                }
            }
            for (String usedPkgName : uses)
            {
                Packages candSourcePkgs = revisionPkgMap.get(candSourceCap.getResource());
                List<Blame> candSourceBlames = null;
                // Check to see if the used package is exported.
                Blame candExportedBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                if (candExportedBlame != null)
                {
                    candSourceBlames = new ArrayList(1);
                    candSourceBlames.add(candExportedBlame);
                }
                else
                {
                    // If the used package is not exported, check to see if it
                    // is required.
                    candSourceBlames = candSourcePkgs.m_requiredPkgs.get(usedPkgName);
                    // Lastly, if the used package is not required, check to see if it
                    // is imported.
                    candSourceBlames = (candSourceBlames != null)
                        ? candSourceBlames : candSourcePkgs.m_importedPkgs.get(usedPkgName);
                }

                // If the used package cannot be found, then just ignore it
                // since it has no impact.
                if (candSourceBlames == null)
                {
                    continue;
                }

                List<Blame> usedCaps = currentPkgs.m_usedPkgs.get(usedPkgName);
                if (usedCaps == null)
                {
                    usedCaps = new ArrayList<Blame>();
                    currentPkgs.m_usedPkgs.put(usedPkgName, usedCaps);
                }
                for (Blame blame : candSourceBlames)
                {
                    if (blame.m_reqs != null)
                    {
                        List<Requirement> blameReqs2 = new ArrayList(blameReqs);
                        blameReqs2.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                        usedCaps.add(new Blame(blame.m_cap, blameReqs2));
                        mergeUses(env, current, currentPkgs, blame.m_cap, blameReqs2,
                            revisionPkgMap, allCandidates, cycleMap);
                    }
                    else
                    {
                        usedCaps.add(new Blame(blame.m_cap, blameReqs));
                        mergeUses(env, current, currentPkgs, blame.m_cap, blameReqs,
                            revisionPkgMap, allCandidates, cycleMap);
                    }
                }
            }
        }
    }

    private void checkPackageSpaceConsistency(
        Environment env,
        boolean isDynamicImporting,
        Resource revision,
        Candidates allCandidates,
        Map<Resource, Packages> revisionPkgMap,
        Map<Resource, Object> resultCache)
    {
        if (env.getWirings().containsKey(revision) && !isDynamicImporting)
        {
            return;
        }
        else if (resultCache.containsKey(revision))
        {
            return;
        }

        Packages pkgs = revisionPkgMap.get(revision);

        ResolutionException rethrow = null;
        Candidates permutation = null;
        Set<Requirement> mutated = null;

        // Check for conflicting imports from fragments.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                Blame sourceBlame = null;
                for (Blame blame : entry.getValue())
                {
                    if (sourceBlame == null)
                    {
                        sourceBlame = blame;
                    }
                    else if (!sourceBlame.m_cap.getResource().equals(blame.m_cap.getResource()))
                    {
                        // Try to permutate the conflicting requirement.
                        permutate(allCandidates, blame.m_reqs.get(0), m_importPermutations);
                        // Try to permutate the source requirement.
                        permutate(allCandidates, sourceBlame.m_reqs.get(0), m_importPermutations);
                        // Report conflict.
                        ResolutionException ex = new ResolutionException(
                            "Uses constraint violation. Unable to resolve bundle revision "
                            + Util.getSymbolicName(revision)
                            + " [" + revision
                            + "] because it is exposed to package '"
                            + entry.getKey()
                            + "' from bundle revisions "
                            + Util.getSymbolicName(sourceBlame.m_cap.getResource())
                            + " [" + sourceBlame.m_cap.getResource()
                            + "] and "
                            + Util.getSymbolicName(blame.m_cap.getResource())
                            + " [" + blame.m_cap.getResource()
                            + "] via two dependency chains.\n\nChain 1:\n"
                            + toStringBlame(env, sourceBlame)
                            + "\n\nChain 2:\n"
                            + toStringBlame(env, blame),
                            null,
                            Collections.singleton(blame.m_reqs.get(0)));
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "Candidate permutation failed due to a conflict with a "
                            + "fragment import; will try another if possible.",
                            ex);
                        throw ex;
                    }
                }
            }
        }

        for (Entry<String, Blame> entry : pkgs.m_exportedPkgs.entrySet())
        {
            String pkgName = entry.getKey();
            Blame exportBlame = entry.getValue();
            if (!pkgs.m_usedPkgs.containsKey(pkgName))
            {
                continue;
            }
            for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
            {
                if (!isCompatible(env, exportBlame.m_cap, usedBlame.m_cap, revisionPkgMap))
                {
                    // Create a candidate permutation that eliminates all candidates
                    // that conflict with existing selected candidates.
                    permutation = (permutation != null)
                        ? permutation
                        : allCandidates.copy();
                    rethrow = (rethrow != null)
                        ? rethrow
                        : new ResolutionException(
                            "Uses constraint violation. Unable to resolve bundle revision "
                            + Util.getSymbolicName(revision)
                            + " [" + revision
                            + "] because it exports package '"
                            + pkgName
                            + "' and is also exposed to it from bundle revision "
                            + Util.getSymbolicName(usedBlame.m_cap.getResource())
                            + " [" + usedBlame.m_cap.getResource()
                            + "] via the following dependency chain:\n\n"
                            + toStringBlame(env, usedBlame),
                            null,
                            null);

                    mutated = (mutated != null)
                        ? mutated
                        : new HashSet<Requirement>();

                    for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                    {
                        Requirement req = usedBlame.m_reqs.get(reqIdx);

                        // If we've already permutated this requirement in another
                        // uses constraint, don't permutate it again just continue
                        // with the next uses constraint.
                        if (mutated.contains(req))
                        {
                            break;
                        }

                        // See if we can permutate the candidates for blamed
                        // requirement; there may be no candidates if the revision
                        // associated with the requirement is already resolved.
                        SortedSet<Capability> candidates =
                            permutation.getCandidates(req);
                        if ((candidates != null) && (candidates.size() > 1))
                        {
                            mutated.add(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            // Continue with the next uses constraint.
                            break;
                        }
                    }
                }
            }

            if (rethrow != null)
            {
                if (mutated.size() > 0)
                {
                    m_usesPermutations.add(permutation);
                }
                m_logger.log(
                    Logger.LOG_DEBUG,
                    "Candidate permutation failed due to a conflict between "
                    + "an export and import; will try another if possible.",
                    rethrow);
                throw rethrow;
            }
        }

        // Check if there are any uses conflicts with imported packages.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                String pkgName = entry.getKey();
                if (!pkgs.m_usedPkgs.containsKey(pkgName))
                {
                    continue;
                }
                for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
                {
                    if (!isCompatible(env, importBlame.m_cap, usedBlame.m_cap, revisionPkgMap))
                    {
                        // Create a candidate permutation that eliminates any candidates
                        // that conflict with existing selected candidates.
                        permutation = (permutation != null)
                            ? permutation
                            : allCandidates.copy();
                        rethrow = (rethrow != null)
                            ? rethrow
                            : new ResolutionException(
                                "Uses constraint violation. Unable to resolve bundle revision "
                                + Util.getSymbolicName(revision)
                                + " [" + revision
                                + "] because it is exposed to package '"
                                + pkgName
                                + "' from bundle revisions "
                                + Util.getSymbolicName(importBlame.m_cap.getResource())
                                + " [" + importBlame.m_cap.getResource()
                                + "] and "
                                + Util.getSymbolicName(usedBlame.m_cap.getResource())
                                + " [" + usedBlame.m_cap.getResource()
                                + "] via two dependency chains.\n\nChain 1:\n"
                                + toStringBlame(env, importBlame)
                                + "\n\nChain 2:\n"
                                + toStringBlame(env, usedBlame),
                                null,
                                null);

                        mutated = (mutated != null)
                            ? mutated
                            : new HashSet();

                        for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                        {
                            Requirement req = usedBlame.m_reqs.get(reqIdx);

                            // If we've already permutated this requirement in another
                            // uses constraint, don't permutate it again just continue
                            // with the next uses constraint.
                            if (mutated.contains(req))
                            {
                                break;
                            }

                            // See if we can permutate the candidates for blamed
                            // requirement; there may be no candidates if the revision
                            // associated with the requirement is already resolved.
                            SortedSet<Capability> candidates =
                                permutation.getCandidates(req);
                            if ((candidates != null) && (candidates.size() > 1))
                            {
                                mutated.add(req);
                                Iterator it = candidates.iterator();
                                it.next();
                                it.remove();
                                // Continue with the next uses constraint.
                                break;
                            }
                        }
                    }
                }

                // If there was a uses conflict, then we should add a uses
                // permutation if we were able to permutate any candidates.
                // Additionally, we should try to push an import permutation
                // for the original import to force a backtracking on the
                // original candidate decision if no viable candidate is found
                // for the conflicting uses constraint.
                if (rethrow != null)
                {
                    // Add uses permutation if we mutated any candidates.
                    if (mutated.size() > 0)
                    {
                        m_usesPermutations.add(permutation);
                    }

                    // Try to permutate the candidate for the original
                    // import requirement; only permutate it if we haven't
                    // done so already.
                    Requirement req = importBlame.m_reqs.get(0);
                    if (!mutated.contains(req))
                    {
                        // Since there may be lots of uses constraint violations
                        // with existing import decisions, we may end up trying
                        // to permutate the same import a lot of times, so we should
                        // try to check if that the case and only permutate it once.
                        permutateIfNeeded(allCandidates, req, m_importPermutations);
                    }

                    m_logger.log(
                        Logger.LOG_DEBUG,
                        "Candidate permutation failed due to a conflict between "
                        + "imports; will try another if possible.",
                        rethrow);
                    throw rethrow;
                }
            }
        }

        resultCache.put(revision, Boolean.TRUE);

        // Now check the consistency of all revisions on which the
        // current revision depends. Keep track of the current number
        // of permutations so we know if the lower level check was
        // able to create a permutation or not in the case of failure.
        int permCount = m_usesPermutations.size() + m_importPermutations.size();
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                if (!revision.equals(importBlame.m_cap.getResource()))
                {
                    try
                    {
                        checkPackageSpaceConsistency(
                            env, false, importBlame.m_cap.getResource(),
                            allCandidates, revisionPkgMap, resultCache);
                    }
                    catch (ResolutionException ex)
                    {
                        // If the lower level check didn't create any permutations,
                        // then we should create an import permutation for the
                        // requirement with the dependency on the failing revision
                        // to backtrack on our current candidate selection.
                        if (permCount == (m_usesPermutations.size() + m_importPermutations.size()))
                        {
                            Requirement req = importBlame.m_reqs.get(0);
                            permutate(allCandidates, req, m_importPermutations);
                        }
                        throw ex;
                    }
                }
            }
        }
    }

    private static void permutate(
        Candidates allCandidates, Requirement req, List<Candidates> permutations)
    {
        SortedSet<Capability> candidates = allCandidates.getCandidates(req);
        if (candidates.size() > 1)
        {
            Candidates perm = allCandidates.copy();
            candidates = perm.getCandidates(req);
            Iterator it = candidates.iterator();
            it.next();
            it.remove();
            permutations.add(perm);
        }
    }

    private static void permutateIfNeeded(
        Candidates allCandidates, Requirement req, List<Candidates> permutations)
    {
        SortedSet<Capability> candidates = allCandidates.getCandidates(req);
        if (candidates.size() > 1)
        {
            // Check existing permutations to make sure we haven't
            // already permutated this requirement. This check for
            // duplicate permutations is simplistic. It assumes if
            // there is any permutation that contains a different
            // initial candidate for the requirement in question,
            // then it has already been permutated.
            boolean permutated = false;
            for (Candidates existingPerm : permutations)
            {
                Set<Capability> existingPermCands = existingPerm.getCandidates(req);
                if (!existingPermCands.iterator().next().equals(candidates.iterator().next()))
                {
                    permutated = true;
                }
            }
            // If we haven't already permutated the existing
            // import, do so now.
            if (!permutated)
            {
                permutate(allCandidates, req, permutations);
            }
        }
    }

    private static void calculateExportedPackages(
        Environment env,
        Resource revision,
        Candidates allCandidates,
        Map<Resource, Packages> revisionPkgMap)
    {
        Packages packages = revisionPkgMap.get(revision);
        if (packages != null)
        {
            return;
        }
        packages = new Packages(revision);

        // Get all exported packages.
        Wiring wiring = env.getWirings().get(revision);
        List<Capability> caps = (wiring != null)
            ? wiring.getResourceCapabilities(null)
            : revision.getCapabilities(null);
        Map<String, Capability> exports =
            new HashMap<String, Capability>(caps.size());
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                if (!cap.getResource().equals(revision))
                {
                    cap = new HostedCapability(revision, cap);
                }
                exports.put(
                    (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE),
                    cap);
            }
        }
        // Remove substitutable exports that were imported.
        // For resolved revisions BundleWiring.getCapabilities()
        // already excludes imported substitutable exports, but
        // for resolving revisions we must look in the candidate
        // map to determine which exports are substitutable.
        if (!exports.isEmpty())
        {
            if (wiring == null)
            {
                for (Requirement req : revision.getRequirements(null))
                {
                    if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        Set<Capability> cands = allCandidates.getCandidates(req);
                        if ((cands != null) && !cands.isEmpty())
                        {
                            String pkgName = (String) cands.iterator().next()
                                .getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                            exports.remove(pkgName);
                        }
                    }
                }
            }

            // Add all non-substituted exports to the revisions's package space.
            for (Entry<String, Capability> entry : exports.entrySet())
            {
                packages.m_exportedPkgs.put(
                    entry.getKey(), new Blame(entry.getValue(), null));
            }
        }

        revisionPkgMap.put(revision, packages);
    }

    private boolean isCompatible(
        Environment env, Capability currentCap, Capability candCap,
        Map<Resource, Packages> revisionPkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            if (currentCap.equals(candCap))
            {
                return true;
            }

            List<Capability> currentSources =
                getPackageSources(
                    env,
                    currentCap,
                    revisionPkgMap);
            List<Capability> candSources =
                getPackageSources(
                    env,
                    candCap,
                    revisionPkgMap);

            return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
        }
        return true;
    }

    private Map<Capability, List<Capability>> m_packageSourcesCache
        = new HashMap();

    private List<Capability> getPackageSources(
        Environment env, Capability cap, Map<Resource, Packages> revisionPkgMap)
    {
        // If it is a package, then calculate sources for it.
        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            List<Capability> sources = m_packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(
                    env, cap, revisionPkgMap, new ArrayList(), new HashSet());
                m_packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        // Otherwise, need to return generic capabilies that have
        // uses constraints so they are included for consistency
        // checking.
        String uses = cap.getDirectives().get(AbstractNamespace.CAPABILITY_USES_DIRECTIVE);
        if ((uses != null) && (uses.length() > 0))
        {
            return Collections.singletonList(cap);
        }

        return Collections.EMPTY_LIST;
    }

    private static List<Capability> getPackageSourcesInternal(
        Environment env, Capability cap, Map<Resource, Packages> revisionPkgMap,
        List<Capability> sources, Set<Capability> cycleMap)
    {
        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            if (cycleMap.contains(cap))
            {
                return sources;
            }
            cycleMap.add(cap);

            // Get the package name associated with the capability.
            String pkgName = cap.getAttributes()
                .get(PackageNamespace.PACKAGE_NAMESPACE).toString();

            // Since a revision can export the same package more than once, get
            // all package capabilities for the specified package name.
            Wiring wiring = env.getWirings().get(cap.getResource());
            List<Capability> caps = (wiring != null)
                ? wiring.getResourceCapabilities(null)
                : cap.getResource().getCapabilities(null);
            for (int capIdx = 0; capIdx < caps.size(); capIdx++)
            {
                if (caps.get(capIdx).getNamespace()
                        .equals(PackageNamespace.PACKAGE_NAMESPACE)
                    && caps.get(capIdx).getAttributes()
                        .get(PackageNamespace.PACKAGE_NAMESPACE).equals(pkgName))
                {
                    sources.add(caps.get(capIdx));
                }
            }

            // Then get any addition sources for the package from required bundles.
            Packages pkgs = revisionPkgMap.get(cap.getResource());
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSourcesInternal(env, blame.m_cap,
                        revisionPkgMap, sources, cycleMap);
                }
            }
        }

        return sources;
    }

    private static Resource getActualResource(Resource br)
    {
        if (br instanceof HostResource)
        {
            return ((HostResource) br).getHost();
        }
        return br;
    }

    private static Capability getActualCapability(Capability c)
    {
        if (c instanceof HostedCapability)
        {
            return ((HostedCapability) c).getOriginalCapability();
        }
        return c;
    }

    private static Requirement getActualRequirement(Requirement r)
    {
        if (r instanceof HostedRequirement)
        {
            return ((HostedRequirement) r).getOriginalRequirement();
        }
        return r;
    }

    private static Map<Resource, List<Wire>> populateWireMap(
        Environment env, Resource resource,
        Map<Resource, Packages> revisionPkgMap,
        Map<Resource, List<Wire>> wireMap,
        Candidates allCandidates)
    {
        Resource unwrappedResource = getActualResource(resource);
        if (!env.getWirings().containsKey(unwrappedResource)
            && !wireMap.containsKey(unwrappedResource))
        {
            wireMap.put(unwrappedResource, (List<Wire>) Collections.EMPTY_LIST);

            List<Wire> packageWires = new ArrayList<Wire>();
            List<Wire> bundleWires = new ArrayList<Wire>();
            List<Wire> capabilityWires = new ArrayList<Wire>();

            for (Requirement req : resource.getRequirements(null))
            {
                SortedSet<Capability> cands = allCandidates.getCandidates(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    Capability cand = cands.iterator().next();
                    // Ignore revisions that import themselves.
                    if (!resource.equals(cand.getResource()))
                    {
                        if (!env.getWirings().containsKey(cand.getResource()))
                        {
                            populateWireMap(env, cand.getResource(),
                                revisionPkgMap, wireMap, allCandidates);
                        }
                        Packages candPkgs = revisionPkgMap.get(cand.getResource());
                        Wire wire = new WireImpl(
                            unwrappedResource,
                            getActualRequirement(req),
                            getActualResource(cand.getResource()),
                            getActualCapability(cand));
                        if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                        {
                            packageWires.add(wire);
                        }
                        else if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
                        {
                            bundleWires.add(wire);
                        }
                        else
                        {
                            capabilityWires.add(wire);
                        }
                    }
                }
            }

            // Combine package wires with require wires last.
            packageWires.addAll(bundleWires);
            packageWires.addAll(capabilityWires);
            wireMap.put(unwrappedResource, packageWires);

            // Add host wire for any fragments.
            if (resource instanceof HostResource)
            {
                List<Resource> fragments = ((HostResource) resource).getFragments();
                for (Resource fragment : fragments)
                {
                    List<Wire> hostWires = wireMap.get(fragment);
                    if (hostWires == null)
                    {
                        hostWires = new ArrayList<Wire>();
                        wireMap.put(fragment, hostWires);
                    }
                    hostWires.add(
                        new WireImpl(
                            getActualResource(fragment),
                            fragment.getRequirements(
                                HostNamespace.HOST_NAMESPACE).get(0),
                            unwrappedResource,
                            unwrappedResource.getCapabilities(
                                HostNamespace.HOST_NAMESPACE).get(0)));
                }
            }
        }

        return wireMap;
    }

    private static Map<Resource, List<Wire>> populateDynamicWireMap(
        Environment env, Resource resource, Requirement dynReq,
        Map<Resource, Packages> revisionPkgMap,
        Map<Resource, List<Wire>> wireMap, Candidates allCandidates)
    {
        wireMap.put(resource, (List<Wire>) Collections.EMPTY_LIST);

        List<Wire> packageWires = new ArrayList<Wire>();

        // Get the candidates for the current dynamic requirement.
        SortedSet<Capability> candCaps = allCandidates.getCandidates(dynReq);
        // Record the dynamic candidate.
        Capability dynCand = candCaps.first();

        if (!env.getWirings().containsKey(dynCand.getResource()))
        {
            populateWireMap(env, dynCand.getResource(), revisionPkgMap,
                wireMap, allCandidates);
        }

        packageWires.add(
            new WireImpl(
                resource,
                dynReq,
                getActualResource(dynCand.getResource()),
                getActualCapability(dynCand)));

        wireMap.put(resource, packageWires);

        return wireMap;
    }

    private static Set<String> calculatePackageSpace(
        Environment env, Resource resource, Wiring wiring)
    {
        if (Util.isFragment(resource))
        {
            return Collections.EMPTY_SET;
        }
        Set<String> pkgSpace = new HashSet<String>();
        for (Wire wire : wiring.getRequiredResourceWires(null))
        {
            if (wire.getCapability().getNamespace()
                .equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                pkgSpace.add(
                    (String) wire.getCapability().getAttributes()
                        .get(PackageNamespace.PACKAGE_NAMESPACE));
            }
            else if (wire.getCapability().getNamespace()
                .equals(BundleNamespace.BUNDLE_NAMESPACE))
            {
                Set<String> pkgs = calculateExportedAndReexportedPackages(
                    env,
                    wire.getProvider(),
                    new HashSet<String>(),
                    new HashSet<Resource>());
                pkgSpace.addAll(pkgs);
            }
        }
        return pkgSpace;
    }

    private static Set<String> calculateExportedAndReexportedPackages(
        Environment env,
        Resource res,
        Set<String> pkgs,
        Set<Resource> cycles)
    {
        if (!cycles.contains(res))
        {
            cycles.add(res);

            // Add all exported packages.
            for (Capability cap : res.getCapabilities(null))
            {
                if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    pkgs.add((String)
                        cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                }
            }

            // Now check to see if any required bundles are required with reexport
            // visibility, since we need to include those packages too.
            for (Wire wire : env.getWirings().get(res).getRequiredResourceWires(null))
            {
                if (wire.getCapability().getNamespace().equals(
                    BundleNamespace.BUNDLE_NAMESPACE))
                {
                    String dir = wire.getRequirement()
                        .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                    if ((dir != null) && (dir.equals(Constants.VISIBILITY_REEXPORT)))
                    {
                        calculateExportedAndReexportedPackages(
                            env,
                            wire.getProvider(),
                            pkgs,
                            cycles);
                    }
                }
            }
        }

        return pkgs;
    }

    private static void dumpRevisionPkgMap(
        Environment env, Map<Resource, Packages> revisionPkgMap)
    {
        System.out.println("+++BUNDLE REVISION PKG MAP+++");
        for (Entry<Resource, Packages> entry : revisionPkgMap.entrySet())
        {
            dumpRevisionPkgs(env, entry.getKey(), entry.getValue());
        }
    }

    private static void dumpRevisionPkgs(
        Environment env, Resource resource, Packages packages)
    {
        Wiring wiring = env.getWirings().get(resource);
        System.out.println(resource
            + " (" + ((wiring != null) ? "RESOLVED)" : "UNRESOLVED)"));
        System.out.println("  EXPORTED");
        for (Entry<String, Blame> entry : packages.m_exportedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  IMPORTED");
        for (Entry<String, List<Blame>> entry : packages.m_importedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  REQUIRED");
        for (Entry<String, List<Blame>> entry : packages.m_requiredPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  USED");
        for (Entry<String, List<Blame>> entry : packages.m_usedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
    }

    private static String toStringBlame(Environment env, Blame blame)
    {
        StringBuffer sb = new StringBuffer();
        if ((blame.m_reqs != null) && !blame.m_reqs.isEmpty())
        {
            for (int i = 0; i < blame.m_reqs.size(); i++)
            {
                Requirement req = blame.m_reqs.get(i);
                sb.append("  ");
                sb.append(Util.getSymbolicName(req.getResource()));
                sb.append(" [");
                sb.append(req.getResource().toString());
                sb.append("]\n");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("    import: ");
                }
                else
                {
                    sb.append("    require: ");
                }
                sb.append(req.getDirectives().get(AbstractNamespace.REQUIREMENT_FILTER_DIRECTIVE));
                sb.append("\n     |");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("\n    export: ");
                }
                else
                {
                    sb.append("\n    provide: ");
                }
                if ((i + 1) < blame.m_reqs.size())
                {
                    Capability cap = Util.getSatisfyingCapability(
                        env,
                        blame.m_reqs.get(i + 1).getResource(),
                        blame.m_reqs.get(i));
                    if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(cap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE).toString());
                        Capability usedCap;
                        if ((i + 2) < blame.m_reqs.size())
                        {
                            usedCap = Util.getSatisfyingCapability(
                                env,
                                blame.m_reqs.get(i + 2).getResource(),
                                blame.m_reqs.get(i + 1));
                        }
                        else
                        {
                            usedCap = Util.getSatisfyingCapability(
                                env,
                                blame.m_cap.getResource(),
                                blame.m_reqs.get(i + 1));
                        }
                        sb.append("; uses:=");
                        sb.append(usedCap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE));
                    }
                    else
                    {
                        sb.append(cap);
                    }
                    sb.append("\n");
                }
                else
                {
                    Capability export = Util.getSatisfyingCapability(
                        env,
                        blame.m_cap.getResource(),
                        blame.m_reqs.get(i));
                    sb.append(export.getNamespace());
                    sb.append("=");
                    sb.append(export.getAttributes().get(export.getNamespace()).toString());
                    if (export.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
                        && !export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                            .equals(blame.m_cap.getAttributes().get(
                            		PackageNamespace.PACKAGE_NAMESPACE)))
                    {
                        sb.append("; uses:=");
                        sb.append(blame.m_cap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE));
                        sb.append("\n    export: ");
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(blame.m_cap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE).toString());
                    }
                    sb.append("\n  ");
                    sb.append(Util.getSymbolicName(blame.m_cap.getResource()));
                    sb.append(" [");
                    sb.append(blame.m_cap.getResource().toString());
                    sb.append("]");
                }
            }
        }
        else
        {
            sb.append(blame.m_cap.getResource().toString());
        }
        return sb.toString();
    }

    private static class Packages
    {
        private final Resource m_revision;
        public final Map<String, Blame> m_exportedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_importedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_requiredPkgs = new HashMap();
        public final Map<String, List<Blame>> m_usedPkgs = new HashMap();

        public Packages(Resource revision)
        {
            m_revision = revision;
        }
    }

    private static class Blame
    {
        public final Capability m_cap;
        public final List<Requirement> m_reqs;

        public Blame(Capability cap, List<Requirement> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        @Override
        public String toString()
        {
            return m_cap.getResource()
                + "." + m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                + (((m_reqs == null) || m_reqs.isEmpty())
                    ? " NO BLAME"
                    : " BLAMED ON " + m_reqs);
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
                && m_cap.equals(((Blame) o).m_cap);
        }
    }
}