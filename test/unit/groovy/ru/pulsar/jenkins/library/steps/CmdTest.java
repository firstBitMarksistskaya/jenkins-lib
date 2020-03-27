package ru.pulsar.jenkins.library.steps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.MockStepExecutor;
import ru.pulsar.jenkins.library.ioc.ContextRegistry;
import ru.pulsar.jenkins.library.ioc.IContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CmdTest {

  private IStepExecutor steps;

  @BeforeEach
  void setUp() {
    IContext context = mock(IContext.class);
    steps = spy(new MockStepExecutor());

    when(context.getStepExecutor()).thenReturn(steps);

    ContextRegistry.registerContext(context);
  }

  @Test
  void runOk() {
    // given
    final String script = "echo hello";
    Cmd cmd = new Cmd(script);

    // when
    int run = cmd.run();

    // then
    verify(steps).isUnix();
    verify(steps).bat(contains(script), eq(false), anyString());

    assertThat(run).isEqualTo(0);
  }

  @Test
  void runFailNoReturn() {
    // given
    final String script = "false";
    Cmd cmd = new Cmd(script);

    String thrownText = "failed";
    when(steps.bat(anyString(), anyBoolean(), anyString())).thenThrow(new Error(thrownText));
    when(steps.sh(anyString(), anyBoolean(), anyString())).thenThrow(new Error(thrownText));

    // when
    Throwable thrown = catchThrowable(cmd::run);
    assertThat(thrown).hasMessageContaining(thrownText);

    // then
    verify(steps).isUnix();
    assertThat(steps).satisfiesAnyOf(
      steps -> verify(steps).bat(contains(script), eq(false), anyString()),
      steps -> verify(steps).sh(contains(script), eq(false), anyString())
    );
  }

  @Test
  void runPassAndReturn() {
    // given
    final String script = "false";
    Cmd cmd = new Cmd(script, true);

    when(steps.bat(anyString(), anyBoolean(), anyString())).thenReturn(1);
    when(steps.sh(anyString(), anyBoolean(), anyString())).thenReturn(1);

    // when
    int run = cmd.run();

    // then
    verify(steps).isUnix();
    assertThat(steps).satisfiesAnyOf(
      steps -> verify(steps).bat(contains(script), eq(true), anyString()),
      steps -> verify(steps).sh(contains(script), eq(true), anyString())
    );

    assertThat(run).isEqualTo(1);
  }
}