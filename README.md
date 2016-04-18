# Gradle Offline Dependencies Plugin

This plugin resolves your project's dependencies (including the transitive ones) and stores all artifacts alongside your project's code. This way you can always build your project without having to fetch dependencies from remote servers.

## Usage

To use the plugin, add the plugin as a buildscript dependency:
```gradle
buildscript {
  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-1.0-SNAPSHOT.jar")
  }
}
```

Then apply it to your project:
```gradle
apply plugin: 'offline-dependencies'
```

The plugin creates a local maven repository where all dependency artifacts will be stored for offline use. To use this repository, add the following:
```gradle
repositories {
  maven {
    url offlineRepositoryRoot
  }
}
```

The ```offlineRepositoryRoot``` property is set by the plugin and defaults to ```${project.projectDir}/offline-repository```.
This property is a standard groovy property and may be changed to whatever path suits your needs. Typically this location will be somewhere alongside your project and commited to version control.

Next, configure the repositories you want do fetch dependencies from:
```gradle
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
}
```

## Example

```gradle
buildscript {
  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-1.0-SNAPSHOT.jar")
  }
}

apply plugin: 'offline-dependencies'

repositories {
  maven {
    url offlineRepositoryRoot
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
