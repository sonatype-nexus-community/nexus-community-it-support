# Nexus repository IT support
[![DepShield Badge](https://depshield.sonatype.org/badges/sonatype-nexus-community/nexus-community-it-support/depshield.svg)](https://depshield.github.io)

Utilises https://www.testcontainers.org to build a Nexus repository manager instance with the 
plugin installed.

## Usage 
```java
@Rule
public NexusContainer nexus = new NexusContainer();
```

Note: This requires your plugin to already be built as it uses the built jar for installing into NXRM

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of ours
to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do NOT file Sonatype support tickets related to Helm support in regard to this plugin
* DO file issues here on GitHub, so that the community can pitch in

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin and the Nexus platform, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
