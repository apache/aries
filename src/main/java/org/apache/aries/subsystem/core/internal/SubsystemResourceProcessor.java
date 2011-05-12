/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.subsystem.core.internal;

import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_CONTENT;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_DIRECTIVE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_EXPORTPACKAGE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_IMPORTPACKAGE;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_MANIFESTVERSION;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_RESOURCES;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_SYMBOLICNAME;
import static org.apache.aries.subsystem.SubsystemConstants.SUBSYSTEM_VERSION;
import static org.apache.aries.subsystem.core.internal.FileUtils.closeQuietly;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.aries.application.utils.manifest.ManifestHeaderProcessor;
import org.apache.aries.subsystem.Subsystem;
import org.apache.aries.subsystem.SubsystemConstants;
import org.apache.aries.subsystem.SubsystemException;
import org.apache.aries.subsystem.core.ResourceResolver;
import org.apache.aries.subsystem.spi.Resource;
import org.apache.aries.subsystem.spi.ResourceOperation;
import org.apache.aries.subsystem.spi.ResourceProcessor;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;
import org.osgi.util.tracker.ServiceTracker;

public class SubsystemResourceProcessor implements ResourceProcessor {
	private static final Version SUBSYSTEM_MANIFEST_VERSION = new Version("1.0");
	private static final long TIMEOUT = 30000;
	
	private final BundleContext bundleContext;
	private final Map<Resource, Subsystem> installed = new HashMap<Resource, Subsystem>();
	private final Map<String, ServiceTracker> trackers = new HashMap<String, ServiceTracker>();
	private final Map<Map<String, String>, Subsystem> updated = new HashMap<Map<String, String>, Subsystem>();
	
	public SubsystemResourceProcessor(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}
	
	public void process(final ResourceOperation operation) throws SubsystemException {
		switch (operation.getType()) {
		case INSTALL:
			installOrUpdate(operation);
			break;
		case START:
			start(operation);
			break;
		case STOP:
			stop(operation);
			break;
		case UNINSTALL:
			uninstall(operation);
			break;
		case UPDATE:
			installOrUpdate(operation);
			break;
		default:
			throw new SubsystemException("Unsupported resource opertaion type: " + operation.getType());
	}
	}
	
    private static void checkManifestHeaders(Manifest manifest, String ssn, String sv) {
        // Check subsystem manifest required headers
        String mfv = manifest.getMainAttributes().getValue(SUBSYSTEM_MANIFESTVERSION);
        if (mfv == null || mfv.length() == 0) {
            throw new SubsystemException("Invalid subsystem manifest version: " + mfv);
        }
        try {
            Version v = Version.parseVersion(mfv);
            if (!SUBSYSTEM_MANIFEST_VERSION.equals(v)) {
                throw new SubsystemException(
                        "Unsupported subsystem manifest version: " + mfv
                                + ". Supported "
                                + SubsystemConstants.SUBSYSTEM_MANIFESTVERSION
                                + " is " + SUBSYSTEM_MANIFEST_VERSION);
            }
        } 
        catch (IllegalArgumentException e) {
            throw new SubsystemException("Invalid subsystem manifest version: " + mfv, e);
        }
        if (ssn == null || ssn.length() == 0) {
            throw new SubsystemException("Invalid subsystem symbolic name: " + ssn);
        }
        // check attributes / directives on the subsystem symbolic name
        // TODO add any other symbolic name to check
        Clause[] ssnClauses = Parser.parseHeader(ssn);
        String ssDirective = ssnClauses[0].getDirective(SUBSYSTEM_DIRECTIVE);
        if (ssDirective != null && ssDirective.equalsIgnoreCase("false")) {
            throw new SubsystemException("Invalid " + SUBSYSTEM_DIRECTIVE
                    + " directive in " + SUBSYSTEM_SYMBOLICNAME + ": "
                    + ssDirective);
        }
        if (sv == null || sv.length() == 0) {
            throw new SubsystemException("Invalid subsystem version: " + sv);
        }
        try {
            new Version(sv);
        } 
        catch (IllegalArgumentException e) {
            throw new SubsystemException("Invalid subsystem version: " + sv, e);
        }
        // TODO: do we want to check other headers such as
        // subsystem-importpackage, subsystem-exportpackage, etc.
    }
    
	private static Map<String, String> computeSubsystemHeaders(Manifest manifest, String ssn, String sv) {
        // Grab all headers
        Map<String, String> headers = new HashMap<String, String>();
        Iterator it = manifest.getMainAttributes().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            String name = e.getKey().toString();
            String value = e.getValue().toString();
            headers.put(name, value);
        }
        // Create the required composite headers
        headers.put(BUNDLE_SYMBOLICNAME, getSubsystemSymbolicName(ssn));
        headers.put(BUNDLE_VERSION, sv);
        String subImportPkg = headers.get(SUBSYSTEM_IMPORTPACKAGE);
        String subExportPkg = headers.get(SUBSYSTEM_EXPORTPACKAGE);
        // TODO: compute other composite manifest entries
        // TODO: compute list of bundles
        return headers;
    }
	
    private Filter createFilter(String packages, String namespace) {
        if (namespace.equals(BundleRevision.PACKAGE_NAMESPACE)) {
            // split packages
            List<String> pkgs = ManifestHeaderProcessor.split(packages, ",");
            StringBuffer sb = new StringBuffer();
            sb.append("(|");
            for (String pkg : pkgs) {
                sb.append("(" + BundleRevision.PACKAGE_NAMESPACE + "=" + pkg + ")");
            }
            sb.append(")");
            try {
                return FrameworkUtil.createFilter(sb.toString());
            } 
            catch (InvalidSyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (namespace.equals("scope.share.service")) {
            // split packages
            List<String> pkgs = ManifestHeaderProcessor.split(packages, ",");
            StringBuffer sb = new StringBuffer();
            sb.append("(|");
            for (String pkg : pkgs) {
                sb.append("(scope.share.service=" + pkg + ")");
            }
            sb.append(")");
            try {
                return FrameworkUtil.createFilter(sb.toString());
            } 
            catch (InvalidSyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }
	
	private Subsystem findSubsystem(Resource resource, Subsystem s) {
        for (Subsystem subsystem : s.getChildren()) {
            if (resource.getAttributes().get(Resource.LOCATION_ATTRIBUTE).equals(subsystem.getLocation())) {
                return subsystem;
            }
        }
        return null;
    }
	
	private Manifest getManifest(ResourceOperation operation) throws Exception {
		Manifest manifest = (Manifest)operation.getContext().get("manifest");
        if (manifest == null) {
            // Unpack the subsystem archive into a temporary directory
            File dir = File.createTempFile("subsystem", "", null);
            if (dir == null || !dir.delete() || !dir.mkdir()) {
                throw new Exception("Unable to create temporary dir");
            }
            String location = String.valueOf(operation.getResource().getAttributes().get(Resource.LOCATION_ATTRIBUTE));
            FileUtils.unpackArchive(new URL(location).openStream(), dir);
            manifest = new Manifest();
            InputStream mis = new FileInputStream(new File(dir, JarFile.MANIFEST_NAME));
            try {
                manifest.read(mis);
            } 
            finally {
                closeQuietly(mis);
            }
        }
        return manifest;
	}
	
	private <T> T getService(Class<T> clazz) throws InvalidSyntaxException, InterruptedException {
		return getService(clazz, null);
	}

	private <T> T getService(Class<T> clazz, String filter) throws InvalidSyntaxException, InterruptedException {
		Filter flt;
		if (filter != null) {
		    if (!filter.startsWith("(")) {
		        flt = bundleContext.createFilter("(&(" + Constants.OBJECTCLASS
		                + "=" + clazz.getName() + ")(" + filter + "))");
		    } 
		    else {
		        flt = bundleContext.createFilter("(&(" + Constants.OBJECTCLASS
		                + "=" + clazz.getName() + ")" + filter + ")");
		    }
		} 
		else {
		    flt = bundleContext.createFilter("(" + Constants.OBJECTCLASS + "="
		            + clazz.getName() + ")");
		}
		ServiceTracker tracker = trackers.get(flt.toString());
		if (tracker == null) {
		    tracker = new ServiceTracker(bundleContext, flt, null);
		    tracker.open();
		    trackers.put(flt.toString(), tracker);
		}
		T t = (T) tracker.waitForService(TIMEOUT);
		if (t == null) {
		    throw new SubsystemException("No service available: " + flt);
		}
		return t;
	}
	
	// if the ssn already contains COMPOSITE_DIRECTIVE or SUBSYSTEM_DIRECTIVE
    // directive
    // let's not add them again
    private static String getSubsystemSymbolicName(String ssn) {
        Clause[] ssnClauses = Parser.parseHeader(ssn);
        String ssDirective = ssnClauses[0].getDirective(SUBSYSTEM_DIRECTIVE);
        if (ssDirective == null ) {
            ssn = ssn /*+ ";"
                    + SUBSYSTEM_DIRECTIVE + ":=true"*/;
        } 
        return ssn;
    }
    
    private void installOrUpdate(final ResourceOperation operation) {
    	try {
            SubsystemImpl subsystemInContext = (SubsystemImpl)operation.getContext().get("subsystem");
            final Region subsystemInContextRegion = subsystemInContext.getRegion();
            ResourceResolver resolver = getService(ResourceResolver.class);
            Manifest manifest = getManifest(operation);
            List<Resource> resource = new ArrayList<Resource>();
            String resourceHeader = manifest.getMainAttributes().getValue(SUBSYSTEM_RESOURCES);
            Clause[] resourceClauses = Parser.parseHeader(resourceHeader);
            for (Clause c : resourceClauses) {
                Resource r = resolver.find(c.toString());
                resource.add(r);
            }
            List<Resource> content = new ArrayList<Resource>();
            String contentHeader = manifest.getMainAttributes().getValue(SUBSYSTEM_CONTENT);
            Clause[] contentClauses = Parser.parseHeader(contentHeader);
            for (Clause c : contentClauses) {
                Resource r = resolver.find(c.toString());
                content.add(r);
            }
            List<Resource> previous = new ArrayList<Resource>();
            // TODO: convert resources before calling the resolver?
            List<Resource> additional = resolver.resolve(content, resource);
            // check manifest header to see if they are valid
            String ssn = manifest.getMainAttributes().getValue(SUBSYSTEM_SYMBOLICNAME);
            String sv = manifest.getMainAttributes().getValue(SUBSYSTEM_VERSION);
            checkManifestHeaders(manifest, ssn, sv);
            Map<String, String> headers = computeSubsystemHeaders(manifest, ssn, sv);
            // Check existing bundles
            Subsystem subsystemToProcess = findSubsystem(operation.getResource(), subsystemInContext);
            final Region childRegion;
            if (subsystemToProcess == null) {
                // brand new install
            	String location = String.valueOf(operation.getResource().getAttributes().get(Resource.LOCATION_ATTRIBUTE));
                childRegion = subsystemInContextRegion.getRegionDigraph().createRegion(headers.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + headers.get(Constants.BUNDLE_VERSION));
                setupSharePolicies(subsystemInContextRegion, childRegion, headers);
  //              ScopeAdmin childScopeAdmin = getService(ScopeAdmin.class, "ScopeId=" + childScopeUpdate.getScope().getId());
                subsystemToProcess = new SubsystemImpl(childRegion, headers, subsystemInContext, location);
                bundleContext.registerService(Subsystem.class.getName(), subsystemToProcess, DictionaryBuilder.build("SubsystemParentId", subsystemInContext.getSubsystemId(), "SubsystemId", subsystemToProcess.getSubsystemId()));
                installed.put(operation.getResource(), subsystemToProcess);
            } 
            else {
            	childRegion = null;
                // update
                // capture data before update
                Map<String, String> subsystemHeaders = subsystemToProcess.getHeaders();
                String previousContentHeader = (String) subsystemHeaders.get(SUBSYSTEM_CONTENT);
                Clause[] previousContentClauses = Parser.parseHeader(previousContentHeader);
                for (Clause c : previousContentClauses) {
                    Resource r = resolver.find(c.toString());
                    previous.add(r);
                }
                ((SubsystemImpl)subsystemToProcess).updateHeaders(headers);
                updated.put(subsystemHeaders, subsystemToProcess);
            }
            // content is installed in the scope, so need to find the subsystemAdmin for the scope first
            long scopeId = subsystemToProcess.getSubsystemId();
            Subsystem childSubsystem = getService(Subsystem.class, "SubsystemId=" + scopeId);
            for (final Resource r : previous) {
                boolean stillHere = false;
                for (Resource r2 : content) {
                    if (r2.getAttributes().get(Resource.SYMBOLIC_NAME_ATTRIBUTE).equals(r.getAttributes().get(Resource.SYMBOLIC_NAME_ATTRIBUTE))
                            && r2.getAttributes().get(Resource.VERSION_ATTRIBUTE).equals(r.getAttributes().get(Resource.VERSION_ATTRIBUTE))) {
                        stillHere = true;
                        break;
                    }
                }
                if (!stillHere) {
                	// TODO Get this from the service registry.
                	new BundleResourceProcessor(bundleContext).process(new ResourceOperation() {
						public void completed() {
							// TODO Send event.
						}

						public Coordination getCoordination() {
							return operation.getCoordination();
						}

						public Resource getResource() {
							return r;
						}

						public Map<String, Object> getContext() {
							Map<String, Object> context = new HashMap<String, Object>();
							context.put("region", childRegion == null ? subsystemInContextRegion : childRegion);
							return context;
						}

						public Type getType() {
							return Type.UNINSTALL;
						}
                	});
                }
            }
            // additional resource is installed outside of the subsystem.
            for (final Resource r : additional) {
            	// TODO Get this from the service registry.
            	new BundleResourceProcessor(bundleContext).process(new ResourceOperation() {
					public void completed() {
						// TODO Send event.
					}

					public Coordination getCoordination() {
						return operation.getCoordination();
					}

					public Resource getResource() {
						return r;
					}

					public Map<String, Object> getContext() {
						Map<String, Object> context = new HashMap<String, Object>();
						context.put("region", childRegion == null ? subsystemInContextRegion : childRegion);
						return context;
					}

					public Type getType() {
						return Type.INSTALL;
					}
            	});
            }
            for (final Resource r : content) {
            	// TODO Get this from the service registry.
            	new BundleResourceProcessor(bundleContext).process(new ResourceOperation() {
					public void completed() {
						// TODO Send event.
					}

					public Coordination getCoordination() {
						return operation.getCoordination();
					}

					public Resource getResource() {
						return r;
					}

					public Map<String, Object> getContext() {
						Map<String, Object> context = new HashMap<String, Object>();
						context.put("region", childRegion == null ? subsystemInContextRegion : childRegion);
						return context;
					}

					public Type getType() {
						return Type.INSTALL;
					}
            	});
            }
        } 
        catch (Exception e) {
            throw new SubsystemException("Unable to process subsystem", e);
        }
    }
    
    private void setupSharePolicies(Region parent, Region child, Map<String, String> headers) throws Exception {
    	String importPackage = headers.get(SubsystemConstants.SUBSYSTEM_IMPORTPACKAGE);
        String exportPackage = headers.get(SubsystemConstants.SUBSYSTEM_EXPORTPACKAGE);
        String importService = headers.get(SubsystemConstants.SUBSYSTEM_IMPORTSERVICE);
        String exportService = headers.get(SubsystemConstants.SUBSYSTEM_EXPORTSERVICE);
        RegionFilterBuilder builder = child.getRegionDigraph().createRegionFilterBuilder();
        if (importPackage != null && !importPackage.trim().isEmpty()) {
        	builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, importPackage);
        }
        if (importService != null && !importService.trim().isEmpty()) {
        	builder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, importService);
        }
        child.connectRegion(parent, builder.build());
        builder = parent.getRegionDigraph().createRegionFilterBuilder();
        if (exportPackage != null && !exportPackage.trim().isEmpty()) {
        	builder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, exportPackage);
        }
        if (exportService != null && !exportService.trim().isEmpty()) {
        	builder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, exportService);
        }
        parent.connectRegion(child, builder.build());
    }
    
    private void start(final ResourceOperation operation) {
    	final Subsystem subsystem = (Subsystem)operation.getContext().get("subsystem");
    	subsystem.start();
    	operation.getCoordination().addParticipant(new Participant() {
			public void ended(Coordination arg0) throws Exception {
				operation.completed();
			}

			public void failed(Coordination arg0) throws Exception {
				subsystem.stop();
			}
    	});
    }
    
    private void stop(final ResourceOperation operation) {
    	final Subsystem subsystem = (Subsystem)operation.getContext().get("subsystem");
    	subsystem.stop();
    	operation.getCoordination().addParticipant(new Participant() {
			public void ended(Coordination arg0) throws Exception {
				operation.completed();
			}

			public void failed(Coordination arg0) throws Exception {
				subsystem.start();
			}
    	});
    }
    
    private void uninstall(final ResourceOperation operation) {
    	final Subsystem subsystem = (Subsystem)operation.getContext().get("subsystem");
    	subsystem.uninstall();
    	operation.getCoordination().addParticipant(new Participant() {
			public void ended(Coordination arg0) throws Exception {
				operation.completed();
			}

			public void failed(Coordination arg0) throws Exception {
				// TODO Rollback?
			}
    	});
    }
}