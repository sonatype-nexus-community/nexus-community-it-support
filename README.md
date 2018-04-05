# Nexus repository IT support
Utilises https://www.testcontainers.org to build a Nexus repository manager instance with the 
plugin installed.

## Usage 
```java
@Rule
public NexusContainer nexus = new NexusContainer();
```

Note: This requires your plugin to already be built as it uses the built jar for installing into NXRM
