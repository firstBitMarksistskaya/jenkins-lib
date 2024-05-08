package ru.pulsar.jenkins.library.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import ru.pulsar.jenkins.library.IStepExecutor;
import ru.pulsar.jenkins.library.ioc.ContextRegistry;
import ru.pulsar.jenkins.library.ioc.IContext;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

  public static IStepExecutor getMockedStepExecutor() {
    IStepExecutor steps = mock(IStepExecutor.class);

    when(steps.isUnix()).thenReturn(SystemUtils.IS_OS_UNIX);

    when(steps.libraryResource(anyString())).thenAnswer(invocation -> {
      String path = invocation.getArgument(0);
      return FileUtils.readFileToString(
        new File("resources/" + path),
        StandardCharsets.UTF_8
      );
    });

    when(steps.env()).thenAnswer(invocation -> new EnvUtils());

    when(steps.readFile(anyString(), anyString())).thenAnswer(invocation -> {
      String file = invocation.getArgument(0);
      String encoding = invocation.getArgument(1);
      return FileUtils.readFileToString(new File(file), encoding);
    });

    when(steps.readFile(anyString())).thenAnswer(invocation -> {
      String file = invocation.getArgument(0);
      return FileUtils.readFileToString(new File(file), StandardCharsets.UTF_8);
    });

    when(steps.fileExists(anyString())).thenAnswer(invocation -> {
      String file = invocation.getArgument(0);
      return new File(file).exists();
    });

    return steps;
  }

  public static IContext setupMockedContext() {
    return setupMockedContext(getMockedStepExecutor());
  }

  public static IContext setupMockedContext(IStepExecutor steps) {
    IContext context = mock(IContext.class);
    when(context.getStepExecutor()).thenReturn(steps);

    ContextRegistry.registerContext(context);

    return context;
  }
}
