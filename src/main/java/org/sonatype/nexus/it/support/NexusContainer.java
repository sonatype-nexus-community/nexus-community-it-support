package org.sonatype.nexus.it.support;

import java.io.IOException;
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

  public NexusContainer(final String pluginJarPath) {
    super(new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder.from("sonatype/nexus3:latest")
                .add("/plugin/", "/plugin/")
                .user("root")
                .run("sed -i 's@(wrap),@(wrap), ssh,@g' /opt/sonatype/nexus/etc/karaf/org.apache.karaf.features.cfg")
                .run("sed -i 's@-Dkaraf.startLocalConsole=false@-Dkaraf.startLocalConsole=false\\n-Dkaraf.startRemoteShell=true@g' /opt/sonatype/nexus/bin/nexus.vmoptions")
                .run("cp /plugin/target/nexus-community-it-plugin.jar /opt/sonatype/nexus/deploy/nexus-community-it-plugin.jar")
                .run("yum install sshpass -y")
                .run("yum install openssh-clients -y")
                .user("nexus"))
        .withFileFromPath("plugin/target/nexus-community-it-plugin.jar", Paths.get(pluginJarPath))
    );

    setWaitStrategy(new LogMessageWaitStrategy().withRegEx(STARTED_REGEX).withStartupTimeout(ofSeconds(120L)));
    addExposedPort(8081);
  }

  @Override
  protected void containerIsStarted(final InspectContainerResponse containerInfo) {
    super.containerIsStarted(containerInfo);

    followOutput(new Slf4jLogConsumer(log));

    installPlugin();
  }

  private void installPlugin() {
    try {
      execInContainer("sshpass", "-p", "admin123", "ssh", "127.0.0.1", "-p",
          "8022", "-o", "StrictHostKeyChecking=no", "-l", "admin",
          "bundle:install file:///plugin/target/nexus-community-it-plugin.jar");

      String id = execInContainer("sshpass", "-p", "admin123", "ssh", "127.0.0.1", "-p",
          "8022", "-o", "StrictHostKeyChecking=no", "-l", "admin",
          "bundle:list | grep Installed").getStdout().split("\\|")[0];

      execInContainer("sshpass", "-p", "admin123", "ssh", "127.0.0.1", "-p",
          "8022", "-o", "StrictHostKeyChecking=no", "-l", "admin",
          "bundle:start " + id);
    }
    catch (IOException | InterruptedException e) {
      log.error("", e);
    }
  }
}
