/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.issue;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class IssueWithRestartToRemoveRulePluginTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .addPlugin(ItUtils.locateTestPlugin("deprecated-xoo-rule-plugin"))
    .build();

  @Before
  public void resetData() {
    orchestrator.getDatabase().truncateInspectionTables();
  }

  /**
   * SONAR-4364
   */
  @Test
  public void scan_should_close_issue_on_more_existing_rule() {
    IssueClient issueClient = ItUtils.newWsClientForAnonymous(orchestrator).issueClient();

    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-deprecated-rule-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("with-deprecated-rule")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(scan);

    Issue issue = issueClient.find(IssueQuery.create().rules("deprecated-repo:deprecated-rule")).list().get(0);
    assertThat(issue.status()).isEqualTo("OPEN");
    assertThat(issue.resolution()).isNull();

    // Remove deprecated rule plugin with updatecenter web console because there's no way to do that with the Orchestrator API.
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("remove-rule-plugin", "/selenium/issue/remove-deprecated-rule-plugin.html").build()
    );
    orchestrator.restartSonar();

    // Re analyse the project in order to modify the status of the issue
    orchestrator.executeBuild(scan);

    issue = issueClient.find(IssueQuery.create().rules("deprecated-repo:deprecated-rule")).list().get(0);
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.resolution()).isEqualTo("REMOVED");
  }

  /**
   * SONAR-4559
   */
  @Test
  public void issues_on_removed_rule_should_be_displayed_in_issues_code_viewer() throws Exception {
    orchestrator.getDatabase().truncateInspectionTables();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/issue/suite/with-deprecated-rule-profile.xml"));
    SonarRunner scan = SonarRunner.create(ItUtils.locateProjectDir("shared/xoo-sample"))
      .setProfile("with-deprecated-rule")
      .setRunnerVersion("2.2.2");
    orchestrator.executeBuild(scan);

    // Remove deprecated rule plugin with updatecenter web console because there's no way to do that with the Orchestrator API.
    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("remove-rule-plugin", "/selenium/issue/remove-deprecated-rule-plugin.html").build()
    );
    orchestrator.restartSonar();

    orchestrator.executeSelenese(
      Selenese.builder().setHtmlTestsInClasspath("display-issues-code-viewer-on-removed-rule",
        // SONAR-4559
        "/selenium/issue/display-issues-code-viewer-on-removed-rule.html"
      ).build()
    );

    File logs = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logs)).doesNotContain("nil:NilClass");
  }

}
