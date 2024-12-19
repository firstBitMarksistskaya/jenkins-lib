import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

import com.github.victools.jsonschema.module.jackson.JacksonOption;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class JobConfigurationSchemaGenerator {

  public static void main(String[] args) {

    SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
            .with(new JacksonModule(JacksonOption.FLATTENED_ENUMS_FROM_JSONVALUE, JacksonOption.FLATTENED_ENUMS_FROM_JSONPROPERTY));

    configBuilder.forFields().withDefaultResolver(field -> {
              JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
              return annotation == null || annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();
            });

    SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
    JsonNode jsonSchema = generator.generateSchema(JobConfiguration.class);

    String outputPath = "./resources/schema.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
      writer.write(jsonSchema.toPrettyString());
      System.out.println(jsonSchema.toPrettyString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
