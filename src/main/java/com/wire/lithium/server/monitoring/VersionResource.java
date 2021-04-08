package com.wire.lithium.server.monitoring;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.RandomAccessFile;

@Api
@Path("/version")
@Produces(MediaType.APPLICATION_JSON)
public class VersionResource {
    @GET
    @ApiOperation(value = "Returns version of the running code.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, response = Version.class, message = "Version")
    })
    public Response get() {
        return Response
                .ok(getVersion())
                .build();
    }

    private Version getVersion() {
        final String path = System.getenv("RELEASE_FILE_PATH");

        String version = null;
        if (path != null) {
            try (final RandomAccessFile file = new RandomAccessFile(path, "r")) {
                version = file.readLine();
            } catch (Exception ignored) {
            }
        }

        if (version == null) {
            version = "development";
        }
        return new Version(version);
    }

    static class Version {
        @NotNull
        @NotEmpty
        public final String version;

        public Version(String version) {
            this.version = version;
        }
    }
}
