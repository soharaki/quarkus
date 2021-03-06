package io.quarkus.bootstrap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * A representation of the Quarkus dependency model for a given application.
 *
 * @author Alexey Loubyansky
 */
public class AppModel implements Serializable {

    public static final String PARENT_FIRST_ARTIFACTS = "parent-first-artifacts";
    public static final String RUNNER_PARENT_FIRST_ARTIFACTS = "runner-parent-first-artifacts";
    public static final String EXCLUDED_ARTIFACTS = "excluded-artifacts";
    public static final String LESSER_PRIORITY_ARTIFACTS = "lesser-priority-artifacts";

    private static final Logger log = Logger.getLogger(AppModel.class);

    private final AppArtifact appArtifact;

    /**
     * The deployment dependencies, less the runtime parts. This will likely go away
     */
    private final List<AppDependency> deploymentDeps;
    /**
     * The deployment dependencies, including all transitive dependencies. This is used to build an isolated class
     * loader to run the augmentation
     */
    private final List<AppDependency> fullDeploymentDeps;

    /**
     * The runtime dependencies of the application, including the runtime parts of all extensions.
     */
    private final List<AppDependency> runtimeDeps;

    private final Set<AppArtifactKey> parentFirstArtifacts;

    /**
     * These artifacts have effect on the RunnerClassLoader
     */
    private final Set<AppArtifactKey> runnerParentFirstArtifacts;

    private final Set<AppArtifactKey> lesserPriorityArtifacts;

    /**
     * Artifacts that are present in the local maven project.
     *
     * These may be used by dev mode to make decisions about the final packaging for mutable jars
     */
    private final Set<AppArtifactKey> localProjectArtifacts;

    private final Map<String, String> platformProperties;

    private AppModel(AppArtifact appArtifact, List<AppDependency> runtimeDeps, List<AppDependency> deploymentDeps,
            List<AppDependency> fullDeploymentDeps, Set<AppArtifactKey> parentFirstArtifacts,
            Set<AppArtifactKey> runnerParentFirstArtifacts, Set<AppArtifactKey> lesserPriorityArtifacts,
            Set<AppArtifactKey> localProjectArtifacts) {
        this(appArtifact, runtimeDeps, deploymentDeps, fullDeploymentDeps, parentFirstArtifacts, runnerParentFirstArtifacts,
                lesserPriorityArtifacts,
                localProjectArtifacts, Collections.emptyMap());
    }

    private AppModel(AppArtifact appArtifact, List<AppDependency> runtimeDeps, List<AppDependency> deploymentDeps,
            List<AppDependency> fullDeploymentDeps, Set<AppArtifactKey> parentFirstArtifacts,
            Set<AppArtifactKey> runnerParentFirstArtifacts, Set<AppArtifactKey> lesserPriorityArtifacts,
            Set<AppArtifactKey> localProjectArtifacts,
            Map<String, String> platformProperties) {
        this.appArtifact = appArtifact;
        this.runtimeDeps = runtimeDeps;
        this.deploymentDeps = deploymentDeps;
        this.fullDeploymentDeps = fullDeploymentDeps;
        this.parentFirstArtifacts = parentFirstArtifacts;
        this.runnerParentFirstArtifacts = runnerParentFirstArtifacts;
        this.lesserPriorityArtifacts = lesserPriorityArtifacts;
        this.localProjectArtifacts = localProjectArtifacts;
        this.platformProperties = platformProperties;
    }

    public Map<String, String> getPlatformProperties() {
        return platformProperties;
    }

    public AppArtifact getAppArtifact() {
        return appArtifact;
    }

    /**
     * Dependencies that the user has added that have nothing to do with Quarkus (3rd party libs, additional modules etc)
     */
    public List<AppDependency> getUserDependencies() {
        return runtimeDeps;
    }

    /**
     * Dependencies of the -deployment artifacts from the quarkus extensions, and all their transitive dependencies.
     *
     */
    @Deprecated
    public List<AppDependency> getDeploymentDependencies() {
        return deploymentDeps;
    }

    public List<AppDependency> getFullDeploymentDeps() {
        return fullDeploymentDeps;
    }

    public Set<AppArtifactKey> getParentFirstArtifacts() {
        return parentFirstArtifacts;
    }

    public Set<AppArtifactKey> getRunnerParentFirstArtifacts() {
        return runnerParentFirstArtifacts;
    }

    public Set<AppArtifactKey> getLesserPriorityArtifacts() {
        return lesserPriorityArtifacts;
    }

    public Set<AppArtifactKey> getLocalProjectArtifacts() {
        return localProjectArtifacts;
    }

    @Override
    public String toString() {
        return "AppModel{" +
                "appArtifact=" + appArtifact +
                ", deploymentDeps=" + deploymentDeps +
                ", fullDeploymentDeps=" + fullDeploymentDeps +
                ", runtimeDeps=" + runtimeDeps +
                ", parentFirstArtifacts=" + parentFirstArtifacts +
                ", runnerParentFirstArtifacts=" + runnerParentFirstArtifacts +
                '}';
    }

    public static class Builder {

        private AppArtifact appArtifact;

        private final List<AppDependency> deploymentDeps = new ArrayList<>();
        private final List<AppDependency> fullDeploymentDeps = new ArrayList<>();
        private final List<AppDependency> runtimeDeps = new ArrayList<>();
        private final Set<AppArtifactKey> parentFirstArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> runnerParentFirstArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> excludedArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> lesserPriorityArtifacts = new HashSet<>();
        private final Set<AppArtifactKey> localProjectArtifacts = new HashSet<>();
        private Map<String, String> platformProperties = Collections.emptyMap();

        public Builder setAppArtifact(AppArtifact appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public Builder addPlatformProperties(Map<String, String> platformProperties) {
            if (this.platformProperties.isEmpty()) {
                this.platformProperties = platformProperties;
            } else {
                this.platformProperties.putAll(platformProperties);
            }
            return this;
        }

        public Builder addDeploymentDep(AppDependency dep) {
            this.deploymentDeps.add(dep);
            return this;
        }

        public Builder addDeploymentDeps(List<AppDependency> deps) {
            this.deploymentDeps.addAll(deps);
            return this;
        }

        public Builder addFullDeploymentDep(AppDependency dep) {
            this.fullDeploymentDeps.add(dep);
            return this;
        }

        public Builder addFullDeploymentDeps(List<AppDependency> deps) {
            this.fullDeploymentDeps.addAll(deps);
            return this;
        }

        public Builder addRuntimeDep(AppDependency dep) {
            this.runtimeDeps.add(dep);
            return this;
        }

        public Builder addRuntimeDeps(List<AppDependency> deps) {
            this.runtimeDeps.addAll(deps);
            return this;
        }

        public Builder addParentFirstArtifact(AppArtifactKey deps) {
            this.parentFirstArtifacts.add(deps);
            return this;
        }

        public Builder addParentFirstArtifacts(List<AppArtifactKey> deps) {
            this.parentFirstArtifacts.addAll(deps);
            return this;
        }

        public Builder addRunnerParentFirstArtifact(AppArtifactKey deps) {
            this.runnerParentFirstArtifacts.add(deps);
            return this;
        }

        public Builder addRunnerParentFirstArtifacts(List<AppArtifactKey> deps) {
            this.runnerParentFirstArtifacts.addAll(deps);
            return this;
        }

        public Builder addExcludedArtifact(AppArtifactKey deps) {
            this.excludedArtifacts.add(deps);
            return this;
        }

        public Builder addExcludedArtifacts(List<AppArtifactKey> deps) {
            this.excludedArtifacts.addAll(deps);
            return this;
        }

        public Builder addLesserPriorityArtifact(AppArtifactKey deps) {
            this.lesserPriorityArtifacts.add(deps);
            return this;
        }

        public Builder addLocalProjectArtifact(AppArtifactKey deps) {
            this.localProjectArtifacts.add(deps);
            return this;
        }

        public Builder addLocalProjectArtifacts(Collection<AppArtifactKey> deps) {
            this.localProjectArtifacts.addAll(deps);
            return this;
        }

        public Builder addLesserPriorityArtifacts(List<AppArtifactKey> deps) {
            this.lesserPriorityArtifacts.addAll(deps);
            return this;
        }

        /**
         * Sets the parent first and excluded artifacts from a descriptor properties file
         *
         * @param props The quarkus-extension.properties file
         */
        public void handleExtensionProperties(Properties props, String extension) {
            String parentFirst = props.getProperty(PARENT_FIRST_ARTIFACTS);
            if (parentFirst != null) {
                String[] artifacts = parentFirst.split(",");
                for (String artifact : artifacts) {
                    parentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                }
            }
            String runnerParentFirst = props.getProperty(RUNNER_PARENT_FIRST_ARTIFACTS);
            if (runnerParentFirst != null) {
                String[] artifacts = runnerParentFirst.split(",");
                for (String artifact : artifacts) {
                    runnerParentFirstArtifacts.add(new AppArtifactKey(artifact.split(":")));
                }
            }
            String excluded = props.getProperty(EXCLUDED_ARTIFACTS);
            if (excluded != null) {
                String[] artifacts = excluded.split(",");
                for (String artifact : artifacts) {
                    excludedArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is excluding %s", extension, artifact);
                }
            }
            String lesserPriority = props.getProperty(LESSER_PRIORITY_ARTIFACTS);
            if (lesserPriority != null) {
                String[] artifacts = lesserPriority.split(",");
                for (String artifact : artifacts) {
                    lesserPriorityArtifacts.add(new AppArtifactKey(artifact.split(":")));
                    log.debugf("Extension %s is making %s a lesser priority artifact", extension, artifact);
                }
            }
        }

        public AppModel build() {
            Predicate<AppDependency> includePredicate = s -> {
                //we never include the ide launcher in the final app model
                if (s.getArtifact().getGroupId().equals("io.quarkus")
                        && s.getArtifact().getArtifactId().equals("quarkus-ide-launcher")) {
                    return false;
                }
                return !excludedArtifacts.contains(s.getArtifact().getKey());
            };
            List<AppDependency> runtimeDeps = this.runtimeDeps.stream().filter(includePredicate).collect(Collectors.toList());
            List<AppDependency> deploymentDeps = this.deploymentDeps.stream().filter(includePredicate)
                    .collect(Collectors.toList());
            List<AppDependency> fullDeploymentDeps = this.fullDeploymentDeps.stream().filter(includePredicate)
                    .collect(Collectors.toList());
            AppModel appModel = new AppModel(appArtifact, runtimeDeps, deploymentDeps, fullDeploymentDeps,
                    parentFirstArtifacts, runnerParentFirstArtifacts, lesserPriorityArtifacts, localProjectArtifacts,
                    platformProperties);
            log.debugf("Created AppModel %s", appModel);
            return appModel;

        }
    }
}
