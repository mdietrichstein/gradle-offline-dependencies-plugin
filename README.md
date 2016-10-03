# Gradle Offline Dependencies Plugin

This plugin resolves your project dependency artifacts (jar, javadoc, pom, etc.), including transitive ones, and stores them alongside your code. This way you can always build your project without having to fetch dependencies from remote servers.

## How to build

Check out the code, run `./gradlew build` and fetch the resulting jar from `build/libs/offline-dependencies-plugin-0.2-SNAPSHOT.jar`.

## Usage

To use the plugin, add it as a buildscript dependency:
```gradle
buildscript {
  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-0.2-SNAPSHOT.jar")
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
`> ./gradlew updateOfflineRepository`

Downloads dependency artifacts and stores them locally

## Plugin Properties

The offline-dependencies Plugin defines the following properties which may be configured within the ```offlineDependencies``` section:

* ```includeSources```: Download sources (default is ```true```)
* ```includeJavadocs```: Download javadocs (default is ```true```)
* ```includePoms```:  Download pom.xml artifacts (default is ```true```)
* ```includeIvyXmls```:  Download ivy.xml artifacts (default is ```true```)
* ```includeBuildscriptDependencies```: Download dependencies defined in the ```buildscript``` section (default is ```true```). Buildscript dependencies need special handling. See __Handling Buildscript Dependencies__ below for details
* ```configurations```: Project confgurations for which dependency artifacts should be downloaded (defaults to all project configurations)
* ```buildScriptConfigurations```: Buildscript configurations for which dependency artifacts should be downloaded (defaults to all  buildscript configurations)

## Handling Buildscript Dependencies

The are two issues when it comes to buildscript dependencies. Both stem from the fact that gradle applies the  `offline-dependencies` plugin only after the  `buildscript` block has been evaluated.

The first issue is that you can not use the default `offlineRepositoryRoot` property within a buildscript block, the reason being that the `offline-dependencies` plugin hasn't had a chance to set the property by itself yet.

You can get around that limitation by setting the property yourself, e.g. by supplying it as a command line parameter:

`./gradlew updateOfflineRepository -PofflineRepositoryRoot=./offline-repository` 

Alternatively yout can create en entry for this property in your project's `gradle.properties` file.

The second issue is that gradle won't be able to resolve your buildscript dependencies when running the `updateOfflineRepository` task for the first time. You can solve this issue by specifying the offline repository alongside the remote repositories, e.g.

```gradle
buildscript {
  repositories {
    maven {
      url offlineRepositoryRoot
    }
    mavenCentral()
    jcenter()
  }

  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-0.2-SNAPSHOT.jar")
    classpath "some.other.buildscript:dependency:1.0.0"
  }
}
```

Just make sure that the `offlineRepositoryRoot` repository is first in the list.

## Example build.gradle

```gradle
apply plugin: 'offline-dependencies'
apply plugin: 'java'

buildscript {
  dependencies {
    classpath files("../your/path/to/offline-dependencies-plugin-0.2-SNAPSHOT.jar")
  }
}

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
