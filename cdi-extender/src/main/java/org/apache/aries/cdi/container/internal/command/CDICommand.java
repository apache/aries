/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.command;

import static java.util.stream.Collectors.*;

import java.util.Collection;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.aries.cdi.container.internal.CCR;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.ActivationDTO;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.service.cdi.runtime.dto.template.ActivationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ReferenceTemplateDTO;

public class CDICommand {

	public CDICommand(CCR ccr) {
		_ccr = ccr;
	}

	public String info(Bundle bundle) {
		try (Formatter f = new Formatter()) {
			Collection<ContainerDTO> containerDTOs = _ccr.getContainerDTOs(bundle);

			if (containerDTOs.isEmpty()) {
				f.format(NO_BUNDLES);
				return f.toString();
			}

			list0(f, containerDTOs.iterator().next(), false, true);

			return f.toString();
		}
	}

	public String list(Bundle... bundles) {
		try (Formatter f = new Formatter()) {
			Collection<ContainerDTO> containerDTOs = _ccr.getContainerDTOs(bundles);

			if (containerDTOs.isEmpty()) {
				f.format(NO_BUNDLES);
				return f.toString();
			}

			List<ContainerDTO> containerDTOList = containerDTOs.stream().sorted(
				(a, b) -> Long.compare(a.bundle.id,b.bundle.id)
			).collect(toList());

			for (Iterator<ContainerDTO> itr = containerDTOList.iterator(); itr.hasNext();) {
				ContainerDTO containerDTO = itr.next();

				list0(f, containerDTO, itr.hasNext(), false);
			}

			return f.toString();
		}
	}

	private void list0(Formatter f, ContainerDTO containerDTO, boolean hasNext, boolean verbose) {
		String curb = hasNext ? TLLS : CLLS;
		String prefix = hasNext ? PSSSSSSS : SSSSSSSS;

		f.format(
			"%s%s[%s]%n",
			curb,
			containerDTO.bundle.symbolicName,
			containerDTO.bundle.id);

		f.format(
			"%s%sCOMPONENTS%n",
			(hasNext ? PSSS : SSSS),
			curb);

		Map<Boolean, List<ComponentTemplateDTO>> componentTemplateDTOs = containerDTO.template.components.stream().collect(
			partitioningBy(c -> c.type == ComponentType.CONTAINER)
		);

		ComponentTemplateDTO componentTemplateDTO = componentTemplateDTOs.get(Boolean.TRUE).get(0);

		List<ComponentTemplateDTO> singleAndFactory = componentTemplateDTOs.get(Boolean.FALSE);

		list0(
			f,
			containerDTO,
			componentTemplateDTO,
			prefix,
			!singleAndFactory.isEmpty(),
			!singleAndFactory.isEmpty(),
			verbose);

		for (Iterator<ComponentTemplateDTO> itr2 = singleAndFactory.iterator(); itr2.hasNext();) {
			componentTemplateDTO = itr2.next();

			list0(
				f,
				containerDTO,
				componentTemplateDTO,
				prefix,
				itr2.hasNext(),
				false, verbose);
		}
	}

	private void list0(
		Formatter f, ContainerDTO containerDTO, ComponentTemplateDTO componentTemplateDTO, String prefix,
		boolean hasNext, boolean curb, boolean verbose) {
		Map<Boolean, List<ConfigurationTemplateDTO>> configMap = configMap(componentTemplateDTO);

		if (verbose) {
			f.format(
				"%s%sNAME: %s%n",
				prefix,
				(hasNext ? TLLS : CLLS),
				componentTemplateDTO.name);
			f.format(
				"%s%s%sTYPE: %s%n",
				prefix,
				(hasNext ? PSSS : SSSS),
				TLLS,
				componentTemplateDTO.type);
		}
		else {
			f.format(
				"%s%sNAME: %s (%s%s)%n",
				prefix,
				(hasNext ? TLLS : CLLS),
				componentTemplateDTO.name,
				componentTemplateDTO.type,
				factoryPid(configMap));
		}

		ComponentDTO componentDTO = containerDTO.components.stream().filter(
			c -> c.template.name.equals(componentTemplateDTO.name)
		).findFirst().orElse(null);

		if ((componentDTO != null) && !componentDTO.instances.isEmpty()) {
			Iterator<ComponentInstanceDTO> itr3 = componentDTO.instances.iterator();

			for (;itr3.hasNext();) {
				ComponentInstanceDTO instanceDTO = itr3.next();

				formatInstance(
					f,
					prefix,
					componentDTO,
					instanceDTO,
					pids(instanceDTO, configMap),
					hasNext,
					itr3.hasNext(),
					verbose);
			}
		}
		else {
			formatInstance(
				f,
				prefix,
				componentDTO,
				null,
				configMap.get(Boolean.FALSE).stream().map(c -> c.pid).collect(toList()).toString(),
				hasNext,
				false,
				verbose);
		}
	}

	private void formatInstance(
		Formatter f, String prefix, ComponentDTO componentDTO,
		ComponentInstanceDTO instanceDTO, String pids,
		boolean hasNext, boolean hasNext2, boolean verbose) {

		if (verbose) {
			f.format(
				"%s%s%sBEANS: %s%n",
				prefix,
				(hasNext ? PSSS : SSSS),
				TLLS,
				componentDTO.template.beans.toString());

			f.format(
				"%s%s%sCONFIGURATIONS%n",
				prefix,
				(hasNext ? PSSS : SSSS),
				TLLS);

			for (Iterator<ConfigurationTemplateDTO> itr = componentDTO.template.configurations.iterator();itr.hasNext();) {
				ConfigurationTemplateDTO conf = itr.next();

				ConfigurationDTO configurationDTO = null;

				if (instanceDTO != null) {
					configurationDTO = instanceDTO.configurations.stream().filter(
						c -> c.template.maximumCardinality == conf.maximumCardinality &&
							c.template.pid == conf.pid &&
							c.template.policy == conf.policy
					).findFirst().orElse(null);
				}

				f.format(
					"%s%s%sPID: %s%n",
					prefix,
					(hasNext ? PSSSPSSS : SSSSPSSS),
					(itr.hasNext() ? TLLS : CLLS),
					(configurationDTO != null ? configurationDTO.properties.get(Constants.SERVICE_PID) + STAR : conf.pid));
				f.format(
					"%s%s%s%sPOLICY: %s%n",
					prefix,
					(hasNext ? PSSSPSSS : SSSSPSSS),
					(itr.hasNext() ? PSSS : SSSS),
					((conf.maximumCardinality == MaximumCardinality.MANY) ? TLLS : CLLS),
					conf.policy);

				if (conf.maximumCardinality == MaximumCardinality.MANY) {
					f.format(
						"%s%s%sFACTORY PID: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						conf.pid);
				}
			}

			if (instanceDTO != null) {
				f.format(
					"%s%s%sCOMPONENT PROPERTIES*%n",
					prefix,
					(hasNext ? PSSS : SSSS),
					TLLS);

				for (Iterator<String> itr = instanceDTO.properties.keySet().iterator(); itr.hasNext();) {
					String key = itr.next();

					f.format(
						"%s%s%s%s=%s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? TLLS : CLLS),
						key,
						instanceDTO.properties.get(key));
				}
			}

			if (!componentDTO.template.references.isEmpty()) {
				f.format(
					"%s%s%sREFERENCES%n",
					prefix,
					(hasNext ? PSSS : SSSS),
					TLLS);

				for (Iterator<ReferenceTemplateDTO> itr = componentDTO.template.references.iterator(); itr.hasNext();) {
					ReferenceTemplateDTO dto = itr.next();

					ReferenceDTO referenceDTO = null;

					if (instanceDTO != null) {
						referenceDTO = instanceDTO.references.stream().filter(
							r -> r.template.maximumCardinality == dto.maximumCardinality &&
								r.template.minimumCardinality == dto.minimumCardinality &&
								r.template.name == dto.name &&
								r.template.policy == dto.policy &&
								r.template.policyOption == dto.policyOption &&
								r.template.serviceType == dto.serviceType &&
								r.template.targetFilter == dto.targetFilter
						).findFirst().orElse(null);
					}

					f.format(
						"%s%s%sNAME: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? TLLS : CLLS),
						dto.name);
					f.format(
						"%s%s%sSERVICE TYPE: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						dto.serviceType);
					f.format(
						"%s%s%sTARGET FILTER: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						(referenceDTO != null ? referenceDTO.targetFilter + STAR : dto.targetFilter));
					f.format(
						"%s%s%sMAX CARDINALITY: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						dto.maximumCardinality);
					f.format(
						"%s%s%sMIN CARDINALITY: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						(referenceDTO != null ? referenceDTO.minimumCardinality + STAR : dto.minimumCardinality));
					f.format(
						"%s%s%sPOLICY: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSSTLLS : SSSSTLLS),
						dto.policy);
					f.format(
						"%s%s%s%sPOLICY OPTION: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSS : SSSS),
						(referenceDTO != null ? TLLS : CLLS),
						dto.policyOption);

					if (referenceDTO != null) {
						f.format(
							"%s%s%sMATCHES: %s*%n",
							prefix,
							(hasNext ? PSSSPSSS : SSSSPSSS),
							(itr.hasNext() ? PSSSCLLS : SSSSCLLS),
							referenceDTO.matches);
					}
				}
			}

			if (!componentDTO.template.activations.isEmpty()) {
				f.format(
					"%s%s%sACTIVATIONS%n",
					prefix,
					(hasNext ? PSSS : SSSS),
					TLLS);

				for (Iterator<ActivationTemplateDTO> itr = componentDTO.template.activations.iterator(); itr.hasNext();) {
					ActivationTemplateDTO dto = itr.next();

					ActivationDTO activationDTO = null;

					if (instanceDTO != null) {
						activationDTO = instanceDTO.activations.stream().filter(
							a -> a.template.properties.equals(dto.properties) &&
								a.template.scope == dto.scope &&
								a.template.serviceClasses.equals(dto.serviceClasses)
						).findFirst().orElse(null);
					}

					f.format(
						"%s%s%sSERVICE TYPES: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? TLLS : CLLS),
						dto.serviceClasses);
					f.format(
						"%s%s%s%sSERVICE SCOPE: %s%n",
						prefix,
						(hasNext ? PSSSPSSS : SSSSPSSS),
						(itr.hasNext() ? PSSS : SSSS),
						(activationDTO != null ? TLLS : CLLS),
						dto.scope.toString().toLowerCase());

					if (activationDTO != null) {
						f.format(
							"%s%s%sSERVICE REFERENCE: %s%n",
							prefix,
							(hasNext ? PSSSPSSS : SSSSPSSS),
							(itr.hasNext() ? PSSSCLLS : SSSSCLLS),
							activationDTO.service + STAR);
					}
				}
			}

			f.format(
				"%s%s%sSTATE: %s*%n",
				prefix,
				(hasNext ? PSSS : SSSS),
				(hasNext2 ? TLLS : CLLS),
				state(componentDTO));

			return;
		}

		f.format(
			"%s%s%sSTATE: %s %s%n",
			prefix,
			(hasNext ? PSSS : SSSS),
			(hasNext2 ? TLLS : CLLS),
			state(componentDTO),
			pids);
	}

	private String pids(
		ComponentInstanceDTO instanceDTO,
		Map<Boolean, List<ConfigurationTemplateDTO>> configMap) {

		List<String> resolvedPids = instanceDTO.configurations.stream().map(
			c -> c.properties
		).filter(Objects::nonNull).map(
			p -> (String)p.get(Constants.SERVICE_PID)
		).collect(toList());

		return configMap.values().stream().flatMap(v -> v.stream()).map(c -> c.pid).map(
			c -> {
				String pid = resolvedPids.stream().filter(
					rp -> rp.startsWith(c + '~') || rp.startsWith(c + '.')
				).findFirst().orElse(null);

				if (pid != null) {
					return pid + STAR;
				}
				else if (resolvedPids.stream().anyMatch(rp -> rp.equals(c))) {
					return c + STAR;
				}
				return c;
			}
		).collect(toList()).toString();
	}

	private Map<Boolean, List<ConfigurationTemplateDTO>> configMap(ComponentTemplateDTO componentTemplateDTO) {
		return componentTemplateDTO.configurations.stream().filter(
			c -> c.pid != null
		).collect(
			partitioningBy(c -> c.maximumCardinality == MaximumCardinality.MANY)
		);
	}

	private String factoryPid(Map<Boolean, List<ConfigurationTemplateDTO>> configMap) {
		return configMap.get(Boolean.TRUE).stream().map(
			c -> c.pid
		).findFirst().map(
			c -> EQUAL + c
		).orElse(BLANK);
	}

	private Object state(ComponentDTO componentDTO) {
		if (componentDTO == null) {
			return NULL;
		}
		else if (!componentDTO.enabled) {
			return DISABLED;
		}
		else if (componentDTO.instances.size() == 0) {
			return WAITING;
		}
		return ACTIVE;
	}

	private static final String BLANK = "";
	private static final String ACTIVE = "active";
	private static final String DISABLED = "disabled";
	private static final String EQUAL = "=";
	private static final String NULL = "null";
	private static final String STAR = "*";
	private static final String WAITING = "waiting";
	private static final String CLLS = "└── ";
	private static final String PSSS = "│   ";
	private static final String SSSS = "    ";
	private static final String TLLS = "├── ";
	private static final String SSSSSSSS = "        ";
	private static final String PSSSSSSS = "│       ";
	private static final String PSSSPSSS = "│   │   ";
	private static final String PSSSTLLS = "│   ├── ";
	private static final String SSSSTLLS = "    ├── ";
	private static final String SSSSPSSS = "    │   ";
	private static final String SSSSCLLS = "    └── ";
	private static final String PSSSCLLS = "│   └── ";
	private static final String NO_BUNDLES = "No matching CDI bundles";

	private final CCR _ccr;

}