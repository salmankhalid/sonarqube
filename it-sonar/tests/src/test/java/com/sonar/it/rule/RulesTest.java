/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */

package com.sonar.it.rule;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleParam;
import org.sonar.wsclient.services.RuleQuery;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RulesTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(ItUtils.locateTestPlugin("beta-rule-plugin"))
    .addPlugin(ItUtils.locateTestPlugin("deprecated-rule-plugin"))
    .addPlugin(ItUtils.xooPlugin())
    .build();

  @Test
  public void test_rule_template() {
    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("rule-template",
        "/selenium/rule/rules/copy_and_edit_rule_template.html",
        "/selenium/rule/rules/copy_and_delete_rule_template.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_search_rules() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/RulesTest/Sonar_way_java-profile.xml"));

    Selenese selenese = Selenese
      .builder()
      .setHtmlTestsInClasspath("search-rules",
        "/selenium/rule/search-rules/rule_search.html",
        //SONAR-3936
        "/selenium/rule/search-rules/rule_search_verify_form_values_on_first_call.html",
        // SONAR-3936
        "/selenium/rule/search-rules/search_and_display_inactive_rules.html",
        // SONAR-3966
        "/selenium/rule/search-rules/search_by_plugin.html",
        "/selenium/rule/search-rules/search_by_rule_priority.html",
        "/selenium/rule/search-rules/search_by_rule_activation.html",
        "/selenium/rule/search-rules/search_by_rule_title.html",
        // SONAR-3879
        "/selenium/rule/search-rules/search_by_status.html",
        "/selenium/rule/search-rules/expand_and_collapse.html",
        // SONAR-3880
        "/selenium/rule/search-rules/search_by_creation_date.html",
        // SONAR-4193
        "/selenium/rule/search-rules/display-link-to-another-rule-in-description-rule.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void should_edit_rules() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/com/sonar/it/rule/RulesTest/rule-with-parameters-profile.xml"));
    Selenese selenese = Selenese.builder()
      .setHtmlTestsInClasspath("edit-rules",
        "/selenium/rule/edit_rules/edit-string.html",
        "/selenium/rule/edit_rules/edit-text.html", // SONAR-1995
        "/selenium/rule/edit_rules/edit-integer.html", // SONAR-3432
        "/selenium/rule/edit_rules/edit-float.html",
        "/selenium/rule/edit_rules/edit-boolean.html", // SONAR-4568
        "/selenium/rule/edit_rules/update-parameter-twice-to-null-value.html" // SONAR-4568
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * http://jira.codehaus.org/browse/SONAR-2958
   * SONAR-5023
   */
  @Test
  public void ws_description_field_should_not_be_null() {
    RuleQuery query = new RuleQuery("java").setRepositories("checkstyle").setSearchText("com.puppycrawl.tools.checkstyle.checks.naming.AbstractClassNameCheck");
    Rule rule = orchestrator.getServer().getWsClient().find(query);
    assertThat(rule.getDescription().length(), greaterThan(10));

    assertThat(rule.getParams().size(), greaterThanOrEqualTo(1));
    // the parameter "format" has no description
    assertThat(rule.getParams(), hasItem(new RuleParamMatcher("format", null)));

    // SONAR-5023
    assertThat(rule.getParams(), hasItem(new RuleParamMatcher("ignoreModifier",
      "Controls whether to ignore checking for the abstract modifier on classes that match the name. Default is false.")));
  }

  private static class RuleParamMatcher extends BaseMatcher {

    private String ruleName;
    private String ruleDescription;

    private RuleParamMatcher(String ruleName, String ruleDescription) {
      this.ruleName = ruleName;
      this.ruleDescription = ruleDescription;
    }

    @Override
    public boolean matches(Object o) {
      RuleParam rp = (RuleParam) o;
      return ruleName.equals(rp.getName()) &&
        ((ruleDescription != null && ruleDescription.equals(rp.getDescription()))
        || (ruleDescription == null && rp.getDescription() == null));
    }

    @Override
    public void describeTo(Description description) {

    }

  }

}
