package com.wire.bots.sdk.server.resources;

import com.wire.xenon.backend.models.Version;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

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
        final Version version = new Version();

        if (path != null) {
            try (RandomAccessFile file = new RandomAccessFile(path, "r")) {
                version.version = file.readLine();
            } catch (Exception ignored) {
            }
        }

        if (version.version == null) {
            version.version = "development";
        }
        return version;
    }
}
