/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2017-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.it.support;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static java.lang.Long.compare;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.list;
import static java.time.Duration.ofSeconds;

public class NexusContainer
    extends GenericContainer
{
  public static final int NXRM_PORT = 8081;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String STARTED_REGEX = "Started Sonatype Nexus OSS(.*)\\n";

  public NexusContainer() {
    this(automaticallyFindPluginJar().toString());
  }

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
    addExposedPort(NXRM_PORT);
  }

  @Override
  protected void containerIsStarted(final InspectContainerResponse containerInfo) {
    super.containerIsStarted(containerInfo);

    followOutput(new Slf4jLogConsumer(log));

    installPlugin();

    log.info("Nexus Repository Manager is running on port " + getMappedPort(NXRM_PORT));
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

  /**
   * Attempts to find the latest (last built) jar in the target folder. If this fails you will need to specify the
   * jar path and pass it to the constructor of NexusContainer.
   *
   * @return path of latest jar
   */
  private static Path automaticallyFindPluginJar() {
    try {
      return list(Paths.get("./target/"))
          .filter(p -> p.toString().contains("jar"))
          .filter(p -> !p.toString().contains("sources"))
          .sorted((f1, f2) -> {
            try {
              return compare(getLastModifiedTime(f1).toMillis(), getLastModifiedTime(f2).toMillis());
            }
            catch (Exception e) {
              throw new RuntimeException("Failed to get last modified time of file");
            }
          })
          .findFirst().get();
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to automatically detect plugin jar");
    }
  }
}
