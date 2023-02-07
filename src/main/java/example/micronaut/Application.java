package example.micronaut;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
  info = @Info(
    title = "My App",
    version = "0.1",
    description = "What a cool app",
    license = @License(name = "Copyright (C) 2022-2022 SooonarSource SA", url = "sooonarsource.com"),
    contact = @Contact(name = "Marchin", email = "marchin AT sooonarsource DOT com")
  )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}