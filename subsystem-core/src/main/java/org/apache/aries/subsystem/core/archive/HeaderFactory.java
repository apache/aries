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
package org.apache.aries.subsystem.core.archive;

public class HeaderFactory {
//	public static final String APPLICATIONCONTENT_HEADER = ApplicationContentHeader.NAME;
//	public static final String APPLICATIONSYMBOLICNAME_HEADER = ApplicationSymbolicNameHeader.NAME;
//	public static final String APPLICATIONVERSION_HEADER = ApplicationVersionHeader.NAME;
//	public static final String FEATURECONTENT_HEADER = FeatureContentHeader.NAME;
//	public static final String FEATURESYMBOLICNAME_HEADER = FeatureSymbolicNameHeader.NAME;
//	public static final String FEATUREVERSION_HEADER = FeatureVersionHeader.NAME;
//	
//	private static final String REGEX = '(' + Grammar.NAME + "):\\ (" + Grammar.CLAUSE + ")(?:,(" + Grammar.CLAUSE + "))*";
//	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
//	public static Header createHeader(String header) {
//		Matcher matcher = PATTERN.matcher(header);
//		if (!matcher.matches())
//			throw new IllegalArgumentException("Invalid header: " + header);
//		String name = matcher.group(1);
//		Collection<Clause> clauses = new HashSet<Clause>(matcher.groupCount());
//		for (int i = 2; i <= matcher.groupCount(); i++) {
//			String group = matcher.group(i);
//			if (group == null) continue;
//			AbstractClause clause = new AbstractClause(group);
//			clauses.add(clause);
//		}
//		if (FEATURESYMBOLICNAME_HEADER.equals(name))
//			return new FeatureSymbolicNameHeader(clauses);
//		if (FEATUREVERSION_HEADER.equals(name))
//			return new FeatureVersionHeader(clauses);
//		if (FEATURECONTENT_HEADER.equals(name))
//			return new FeatureContentHeader(clauses);
//		if (APPLICATIONSYMBOLICNAME_HEADER.equals(name))
//			return new ApplicationSymbolicNameHeader(clauses);
//		if (APPLICATIONVERSION_HEADER.equals(name))
//			return new ApplicationVersionHeader(clauses);
//		if (APPLICATIONCONTENT_HEADER.equals(name))
//			return new ApplicationContentHeader(clauses);
//		return new GenericHeader(name, clauses);
//	}
	
//	private static final String REGEX = '(' + Grammar.CLAUSE + ")(?:,(" + Grammar.CLAUSE + "))*";
//	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	public static Header<?> createHeader(String name, String value) {
//		Matcher matcher = PATTERN.matcher(value);
//		if (!matcher.matches())
//			throw new IllegalArgumentException("Invalid header: " + name + ": " + value);
//		Collection<Clause> clauses = new HashSet<Clause>(matcher.groupCount());
//		for (int i = 2; i <= matcher.groupCount(); i++) {
//			String group = matcher.group(i);
//			if (group == null) continue;
//			AbstractClause clause = new AbstractClause(group);
//			clauses.add(clause);
//		}
//		if (name.equals(SubsystemConstants.FEATURE_SYMBOLICNAME))
//			return new FeatureSymbolicNameHeader(value);
//		if (name.equals(SubsystemConstants.FEATURE_VERSION))
//			return new FeatureVersionHeader(value);
//		if (name.equals(SubsystemConstants.FEATURE_CONTENT))
//			return new FeatureContentHeader(value);
//		if (name.equals(SubsystemConstants.APPLICATION_SYMBOLICNAME))
//			return new ApplicationSymbolicNameHeader(value);
//		if (name.equals(SubsystemConstants.APPLICATION_VERSION))
//			return new ApplicationVersionHeader(value);
//		if (name.equals(SubsystemConstants.APPLICATION_CONTENT))
//			return new ApplicationContentHeader(value);
		if (name.equals(SubsystemSymbolicNameHeader.NAME))
			return new SubsystemSymbolicNameHeader(value);
		if (name.equals(SubsystemVersionHeader.NAME))
			return new SubsystemVersionHeader(value);
		if (name.equals(SubsystemContentHeader.NAME))
			return new SubsystemContentHeader(value);
		if (name.equals(SubsystemTypeHeader.NAME))
			return new SubsystemTypeHeader(value);
		if (ExportPackageHeader.NAME.equals(name))
			return new ExportPackageHeader(value);
		if (ImportPackageHeader.NAME.equals(name))
			return new ImportPackageHeader(value);
		if (DeployedContentHeader.NAME.equals(name))
			return new DeployedContentHeader(value);
		if (ProvisionResourceHeader.NAME.equals(name))
			return new ProvisionResourceHeader(value);
		if (SubsystemManifestVersionHeader.NAME.equals(name))
			return new SubsystemManifestVersionHeader(value);
		if (RequireCapabilityHeader.NAME.equals(name))
			return new RequireCapabilityHeader(value);
		if (SubsystemImportServiceHeader.NAME.equals(name))
			return new SubsystemImportServiceHeader(value);
		if (RequireBundleHeader.NAME.equals(name))
			return new RequireBundleHeader(value);
		if (ProvideCapabilityHeader.NAME.equals(name))
			return new ProvideCapabilityHeader(value);
		if (SubsystemExportServiceHeader.NAME.equals(name))
			return new SubsystemExportServiceHeader(value);
		if (BundleSymbolicNameHeader.NAME.equals(name))
			return new BundleSymbolicNameHeader(value);
		if (BundleVersionHeader.NAME.equals(name))
			return new BundleVersionHeader(value);
		if (PreferredProviderHeader.NAME.equals(name))
			return new PreferredProviderHeader(value);
		if (AriesSubsystemParentsHeader.NAME.equals(name))
			return new AriesSubsystemParentsHeader(value);
		if (BundleRequiredExecutionEnvironmentHeader.NAME.equals(name))
			return new BundleRequiredExecutionEnvironmentHeader(value);
		if (SubsystemLocalizationHeader.NAME.equals(name))
			return new SubsystemLocalizationHeader(value);
		return new GenericHeader(name, value);
			
	}
}
