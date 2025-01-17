// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.k8s.operator.test;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.k8s.operator.cluster.GerritCluster;
import com.google.gerrit.k8s.operator.cluster.GerritClusterReconciler;
import com.google.gerrit.k8s.operator.gerrit.Gerrit;
import com.google.gerrit.k8s.operator.gerrit.GerritReconciler;
import com.google.gerrit.k8s.operator.gitgc.GitGarbageCollection;
import com.google.gerrit.k8s.operator.gitgc.GitGarbageCollectionReconciler;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

public class AbstractGerritOperatorE2ETest {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  protected static final KubernetesClient client = getKubernetesClient();
  public static final String IMAGE_PULL_SECRET_NAME = "image-pull-secret";
  public static final TestProperties testProps = new TestProperties();

  protected GerritReconciler gerritReconciler = Mockito.spy(new GerritReconciler(client));
  protected TestGerritCluster gerritCluster;

  @RegisterExtension
  protected LocallyRunOperatorExtension operator =
      LocallyRunOperatorExtension.builder()
          .waitForNamespaceDeletion(true)
          .withReconciler(new GerritClusterReconciler(client))
          .withReconciler(gerritReconciler)
          .withReconciler(new GitGarbageCollectionReconciler(client))
          .build();

  @BeforeEach
  void setup() {
    Mockito.reset(gerritReconciler);
    createImagePullSecret(client, operator.getNamespace());
    this.gerritCluster = new TestGerritCluster(client, operator.getNamespace());
    gerritCluster.deploy();
  }

  @AfterEach
  void cleanup() {
    client.resources(Gerrit.class).inNamespace(operator.getNamespace()).delete();
    client.resources(GerritCluster.class).inNamespace(operator.getNamespace()).delete();
    client.resources(GitGarbageCollection.class).inNamespace(operator.getNamespace()).delete();
  }

  private static KubernetesClient getKubernetesClient() {
    Config config;
    try {
      String kubeconfig = System.getenv("KUBECONFIG");
      if (kubeconfig != null) {
        config = Config.fromKubeconfig(Files.readString(Path.of(kubeconfig)));
        return new KubernetesClientBuilder().withConfig(config).build();
      }
      logger.atWarning().log("KUBECONFIG variable not set. Using default config.");
    } catch (IOException e) {
      logger.atSevere().log("Failed to load kubeconfig. Trying default", e);
    }
    return new KubernetesClientBuilder().build();
  }

  private static void createImagePullSecret(KubernetesClient client, String namespace) {
    StringBuilder secretBuilder = new StringBuilder();
    secretBuilder.append("{\"auths\": {\"");
    secretBuilder.append(testProps.getRegistry());
    secretBuilder.append("\": {\"auth\": \"");
    secretBuilder.append(
        Base64.getEncoder()
            .encodeToString(
                String.format("%s:%s", testProps.getRegistryUser(), testProps.getRegistryPwd())
                    .getBytes()));
    secretBuilder.append("\"}}}");
    String data = Base64.getEncoder().encodeToString(secretBuilder.toString().getBytes());

    Secret imagePullSecret =
        new SecretBuilder()
            .withType("kubernetes.io/dockerconfigjson")
            .withNewMetadata()
            .withName(IMAGE_PULL_SECRET_NAME)
            .withNamespace(namespace)
            .endMetadata()
            .withData(Map.of(".dockerconfigjson", data))
            .build();
    client.resource(imagePullSecret).createOrReplace();
  }
}
