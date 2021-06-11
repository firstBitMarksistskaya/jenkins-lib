import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import ru.pulsar.jenkins.library.configuration.JobConfiguration;

import java.io.File;
import java.io.StringWriter;

public class JobConfigurationSchemaGenerator {

  public static void main(String[] args) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);
    JsonSchema jsonSchema = generator.generateSchema(JobConfiguration.class);

    StringWriter json = new StringWriter();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.writeValue(json, jsonSchema);

    File jsonSchemaFile = new File("./resources/schema.json");
    mapper.writeValue(jsonSchemaFile, jsonSchema);

    System.out.println(json);
  }

}
