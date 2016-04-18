# Gradle Offline Dependencies Plugin

This plugin resolves your project dependency artifacts (jar, javadoc, pom, etc.), including transitive ones, and stores them alongside your code. This way you can always build your project without having to fetch dependencies from remote servers.

## Usage

To use the plugin, add it as a buildscript dependency:
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
    mavenCentral()
  }
}
```

## Tasks

Currently the plugin only exposes a single task:

### updateOfflineRepository
```> gradle updateOfflineRepository```

Downloads dependency artifacts and stores them locally

## Plugin Properties

The offline-dependencies Plugin defines the following properties which may be configured within the ```offlineDependencies``` section:

* ```includeSources```: Download sources (default is ```true```)
* ```includeJavadocs```: Download javadocs (default is ```true```)
* ```includePoms```:  Download pom.xml artifacts (default is ```true```)
* ```includeIvyXmls```:  Download ivy.xml artifacts (default is ```true```)
* ```includeBuildscriptDependencies```: Download dependencies defined in the ```buildscript``` section (default is ```true```)
* ```configurations```: Project confgurations for which dependency artifacts should be downloaded (defaults to all project configurations)
* ```buildScriptConfigurations```: Buildscript configurations for which dependency artifacts should be downloaded (defaults to all  buildscript configurations)


## Example build.gradle

```gradle
apply plugin: 'java'

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

  includeSources = true
  includeJavadocs = true
  includePoms = true
  includeIvyXmls = true
  includeBuildscriptDependencies = false
  
  configurations 'compile', 'debug'
  buildScriptConfigurations 'classpath'
}

version = '1.0'
sourceCompatibility = 1.8

dependencies {
  compile 'com.fasterxml.jackson.core:jackson-databind:2.7.1'
  compile 'com.google.guava:guava:19.0'
}
```
