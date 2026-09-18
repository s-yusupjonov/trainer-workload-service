package com.gym.workload.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Entry point picked up by {@code mvn test} (matches the default Surefire {@code *Test.java}
 * pattern). Delegates to the Cucumber JUnit Platform engine, which discovers every
 * {@code .feature} file under {@code src/test/resources/features} and wires the glue code
 * in {@code com.gym.workload.cucumber}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.gym.workload.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {
}
