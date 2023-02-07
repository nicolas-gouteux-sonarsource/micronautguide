package example.micronaut;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import javax.validation.constraints.NotBlank;

@Controller("/hello/{name}") // <1>
public class HelloController {

    @Value("${greeting.message1}")
    private String message1;
    @Value("${greeting.message2}")
    private String message2;

    @Inject
    Environment environment;

    @Get // <2>
    @Operation(summary = "Greets a person", description = "A friendly greeting is returned" )
    @Produces(MediaType.TEXT_PLAIN) // <3>
    public String hello(@Parameter(description = "The name of the person", example="Margin") @NotBlank String name) {
        System.out.println(environment.getActiveNames());
        return message1 + " " + message2+ " " + name; // <4>
    }
}
