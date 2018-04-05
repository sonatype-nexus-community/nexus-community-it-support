package org.sonatype.nexus.it.support;

import java.nio.file.Paths;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static java.time.Duration.ofSeconds;

public class NexusContainer
    extends GenericContainer
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String STARTED_REGEX = "Started Sonatype Nexus OSS(.*)\\n";

  private static final String PROJECT_NAME = "nexus-repository-apt-1.0.5"; // TODO: This cannot stay hard-coded

  public NexusContainer() {
    super(new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder.from("sonatype/nexus3:latest")
                .add("/plugin/", "/plugin/")
                .user("root")
                .run("mkdir -p /opt/sonatype/nexus/system/net/staticsnow/nexus-repository-apt/1.0.5/")
                .run(
                    "sed -i 's@nexus-repository-npm</feature>@nexus-repository-npm</feature>\\n        <feature prerequisite=\"false\" dependency=\"false\">nexus-repository-apt</feature>@g' /opt/sonatype/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/3.9.0-01/nexus-oss-feature-3.9.0-01-features.xml;")
                .run(
                    "sed -i 's@<feature name=\"nexus-repository-npm\"@<feature name=\"nexus-repository-apt\" description=\"net.staticsnow:nexus-repository-apt\" version=\"1.0.5\">\\n        <details>net.staticsnow:nexus-repository-apt</details>\\n        <bundle>mvn:net.staticsnow/nexus-repository-apt/1.0.5</bundle>\\n    </feature>\\n    <feature name=\"nexus-repository-npm\"@g' /opt/sonatype/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/3.9.0-01/nexus-oss-feature-3.9.0-01-features.xml;")
                .run("cp /plugin/target/nexus-community-it-plugin.jar /opt/sonatype/nexus/system/net/staticsnow/nexus-repository-apt/1.0.5/nexus-repository-apt-1.0.5.jar")
                .user("nexus"))
        .withFileFromPath("plugin/target/nexus-community-it-plugin.jar", Paths.get("./target/" + PROJECT_NAME + ".jar"))
    );

    setWaitStrategy(new LogMessageWaitStrategy().withRegEx(STARTED_REGEX).withStartupTimeout(ofSeconds(120L)));
    addExposedPort(8081);
  }

  @Override
  protected void containerIsStarted(final InspectContainerResponse containerInfo) {
    super.containerIsStarted(containerInfo);
    followOutput(new Slf4jLogConsumer(log));
  }
}
