package com.dtsx.docs.commands.review;

import com.dtsx.docs.config.ArgUtils;
import com.dtsx.docs.config.ctx.BaseCtx;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;

@Getter
public class ReviewCtx extends BaseCtx {
    private final int port;
    private final Path snapshotsFolder;
    private final Path dashboardFolder;
    private final boolean openBrowser;
    private final boolean detached;

    public ReviewCtx(ReviewArgs args, CommandSpec spec) {
        super(spec);

        this.port = validatePort(args.$port);
        this.snapshotsFolder = ArgUtils.requirePath(cmd, args.$snapshotsFolder, "snapshots folder", "-sf", "SNAPSHOTS_FOLDER");
        this.dashboardFolder = Path.of("snapshot-dashboard");
        this.openBrowser = !args.$noOpen;
        this.detached = args.$detached;
    }

    private int validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new ParameterException(cmd, "Invalid port number: " + port + ". Port must be between 1 and 65535.");
        }

        try (val _ = new ServerSocket(port)) {
            return port;
        } catch (IOException e) {
            throw new ParameterException(cmd,
                "Port " + port + " is already in use.\n\n" +
                "Please either:\n" +
                "  1. Stop the process using port " + port + ", or\n" +
                "  2. Specify a different port: dh review -p <port>"
            );
        }
    }
}
