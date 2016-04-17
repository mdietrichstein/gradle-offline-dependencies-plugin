# Gradle Offline Dependencies Plugin

This plugin resolves your project's dependencies (including the transitive ones) and downloads them to a local folder which may also be used as a local repository.

## Usage

```gradle
buildscript {
  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-1.0-SNAPSHOT.jar")
  }
}

apply plugin: 'offline-dependencies'

repositories {
  maven {
    url offlineRepositoryRoot // this property is set by the plugin, but can be changed by you
  }
}

offlineDependencies {
  repositories {
    ivy {
      url 'http://archiecobbs.github.io/ivyroundup/repo/modules/'
      layout 'pattern', {
        artifact '[organisation]/[module]/[revision]/packager.xml'
        ivy '[organisation]/[module]/[revision]/ivy.xml'
      }
    }
    mavenCentral()
  }

  configurations 'compile', 'debug'
  buildScriptConfigurations 'classpath'

  includeSources = true
  includeJavadocs = true
  includePoms = true
  includeIvyXmls = true
  includeBuildscriptDependencies = true
}
```