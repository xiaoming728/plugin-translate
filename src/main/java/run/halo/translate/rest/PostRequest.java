package run.halo.translate.rest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PostRequest(@Schema(requiredMode = REQUIRED) String postName,
                          @Schema(requiredMode = REQUIRED)List<String> categorys) {


}