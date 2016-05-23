package io.pry.gradle.offline_dependencies

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.ivy.IvyModule
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.language.java.artifact.JavadocArtifact
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.util.GFileUtils

import static io.pry.gradle.offline_dependencies.Utils.addToMultimap

class UpdateOfflineRepositoryTask extends DefaultTask {

  @Input GString root
  @Input Set<String> configurationNames
  @Input Set<String> buildscriptConfigurationNames
  @Input boolean includeSources
  @Input boolean includeJavadocs
  @Input boolean includePoms
  @Input boolean includeIvyXmls
  @Input boolean includeBuildscriptDependencies

  @TaskAction
  void run() {
    withRepositoryFiles { repositoryFiles ->
      // copy collected files to destination directory
      repositoryFiles.each { id, files ->
        def directory = moduleDirectory(id)
        GFileUtils.mkdirs(directory)
        files.each { File file ->  GFileUtils.copyFile(file, new File(directory, file.name)) }
      }
    }
  }

  @InputFiles
  Set<File> getInputFiles() {
    withRepositoryFiles { it.values().collect().flatten() as Set<File> }
  }

  @OutputFiles
  Set<File> getOutputFiles() {
    withRepositoryFiles { repositoryFiles ->
      def outputFiles = [] as Set<File>

      repositoryFiles.each { id, files ->
        files.each { File file ->  outputFiles.add(new File(moduleDirectory(id), file.name)) }
      }

      return outputFiles
    }
  }

  // configurations
  private Set<Configuration> getConfigurations() {
    Set<Configuration> configurations = []

    if(this.getConfigurationNames()) {
      def configurationNames = this.getConfigurationNames()

      logger.trace("Trying to resolve the following project configurations: '${configurationNames.join(",")}'")

      configurationNames.each { name ->
        try {
          configurations.add(project.configurations.getByName(name))
        } catch(UnknownConfigurationException e) {
          logger.warn("Unable to resolve project configuration with name '${name}'")
        }
      }
    } else {
      logger.trace("No project configurations specified, defaulting to all configurations")
      configurations.addAll(project.configurations)
    }

    if(this.getIncludeBuildscriptDependencies()) {
      if(this.getBuildscriptConfigurationNames()) {
        def configurationNames = this.getBuildscriptConfigurationNames()

        logger.trace("Trying to resolve the following buildscript configurations: '${configurationNames.join(",")}'")

        configurationNames.each { name ->
          try {
            configurations.add(project.buildscript.configurations.getByName(name))
          } catch(UnknownConfigurationException e) {
            logger.warn("Unable to resolve buildscript configuration with name '${name}'")
          }
        }
      } else {
        logger.trace("No buildscript configurations specified, defaulting to all configurations")
        configurations.addAll(project.buildscript.configurations)
      }
    } else {
      logger.trace("Skipping buildscript configurations")
    }

    if(!configurations) {
      logger.warn('No configurations found. There are no dependencies to resolve.')
    }

    return configurations
  }

  // collect everything
  private Map<ModuleComponentIdentifier, Set<File>> collectRepositoryFiles(Set<Configuration> configurations) {
    Set<ResolvedArtifact> artifacts = []

    // collect artifacts
    for (configuration in configurations) {
      artifacts.addAll(configuration.resolvedConfiguration.resolvedArtifacts)
    }

    Set<ModuleComponentIdentifier> componentIds = []
    Map<ModuleComponentIdentifier, Set<File>> repositoryFiles = [:]

    for (artifact in artifacts) {
      def componentId =
          new DefaultModuleComponentIdentifier(
              artifact.moduleVersion.id.group,
              artifact.moduleVersion.id.name,
              artifact.moduleVersion.id.version
          )

      componentIds.add(componentId)

      logger.trace("Adding artifact for component'{}' (location '{}')", componentId, artifact.file)
      addToMultimap(repositoryFiles, componentId, artifact.file)
    }

    // collect sources and javadocs
    if(this.getIncludeSources() || this.getIncludeJavadocs()) {
      def jvmArtifacts = project.dependencies.createArtifactResolutionQuery()
          .forComponents(componentIds)
          .withArtifacts(JvmLibrary, SourcesArtifact, JavadocArtifact)
          .execute()

      for (component in jvmArtifacts.resolvedComponents) {
        if(this.getIncludeSources()) {
          def sources = component.getArtifacts(SourcesArtifact)
          if (!sources?.empty) {
            sources*.file.each { File source ->
              logger.trace("Adding sources for component'{}' (location '{}')", component.id, source)
              addToMultimap(repositoryFiles, component.id, source)
            }
          }
        }

        if(this.getIncludeJavadocs()) {
          def javadocs = component.getArtifacts(JavadocArtifact)
          if (!javadocs?.empty) {
            javadocs*.file.each { File javadoc ->
              logger.trace("Adding javadocs for component'{}' (location '{}')", component.id, javadoc)
              addToMultimap(repositoryFiles, component.id, javadoc)
            }
          }
        }
      }
    }

    // collect maven poms (for immediate component ids and parents)
    if(this.getIncludePoms()) {
      collectPoms(componentIds, [] as Set, repositoryFiles)
    }

    // collect ivy xml files
    if(this.getIncludeIvyXmls()) {
      collectIvyXmls(componentIds, repositoryFiles)
    }

    return repositoryFiles
  }

  // adds pom artifacts and their parents for the givens component ids
  private void collectPoms(Set<ComponentIdentifier> componentIds, Set<ComponentIdentifier> resolvedIds, Map<ComponentIdentifier, Set<File>> repositoryFiles) {
    logger.trace("Collecting pom files")

    def mavenArtifacts = project.dependencies.createArtifactResolutionQuery()
        .forComponents(componentIds)
        .withArtifacts(MavenModule, MavenPomArtifact)
        .execute()

    def parentIds = [] as Set<ComponentIdentifier>

    for (component in mavenArtifacts.resolvedComponents) {
      resolvedIds.add(component.id)

      def poms = component.getArtifacts(MavenPomArtifact)
      if (poms?.empty) {
        continue
      }

      def pomFile = poms.first().file as File
      logger.trace("Adding pom for component'{}' (location '{}')", component.id, pomFile)
      addToMultimap(repositoryFiles, component.id, pomFile)

      def parentId = findParentId(pomFile)
      
      if (parentId == null) {
        continue
      }

      if(!resolvedIds.contains(parentId)) {
        parentIds.add(parentId)
      }
    }

    def newIds = parentIds - resolvedIds
    if(!newIds.empty) {
      collectPoms(newIds, resolvedIds, repositoryFiles)
    }
  }

  private DefaultModuleComponentIdentifier findParentId(File pomFile) {
    def pom = new XmlSlurper().parse(pomFile)
    def parent = pom.parent

    if(parent) {
      return new DefaultModuleComponentIdentifier(
          parent.groupId as String,
          parent.artifactId as String,
          parent.version as String
      )
    }

    return null
  }

  private void collectIvyXmls(Set<ComponentIdentifier> componentIds, Map<ComponentIdentifier, Set<File>> repositoryFiles) {
    logger.trace("Collecting ivy xmls")

    def ivyArtifacts = project.dependencies.createArtifactResolutionQuery()
        .forComponents(componentIds)
        .withArtifacts(IvyModule, IvyDescriptorArtifact)
        .execute()

    for (component in ivyArtifacts.resolvedComponents) {
      def ivyXmls = component.getArtifacts(IvyDescriptorArtifact)

      if (ivyXmls?.empty) {
        continue
      }

      def ivyXml = ivyXmls.first().file as File
      logger.trace("Adding ivy artifact for component'{}' (location '{}')", component.id, ivyXml)
      addToMultimap(repositoryFiles, component.id, ivyXml)
    }
  }

  /*
    Activate online repositories and collect dependencies.
    Switch back to local repository afterwards.
  */
  private def withRepositoryFiles(Closure<Map<ModuleComponentIdentifier, Set<File>>> callback) {
    def originalRepositories = project.repositories.collect()

    project.repositories.clear()

    OfflineDependenciesExtension extension =
        project.extensions.getByName(OfflineDependenciesPlugin.EXTENSION_NAME) as OfflineDependenciesExtension

    project.repositories.addAll(extension.repositoryHandler)

    def files = collectRepositoryFiles(getConfigurations())

    project.repositories.clear()
    project.repositories.addAll(originalRepositories)

    callback(files)
  }

  // return the offline-repository target directory for the given component (naming follows maven conventions)
  File moduleDirectory(ModuleComponentIdentifier ci) {
    new File("${getRoot()}".toString(), "${ci.group.tokenize(".").join("/")}/${ci.module}/${ci.version}")
  }
}

