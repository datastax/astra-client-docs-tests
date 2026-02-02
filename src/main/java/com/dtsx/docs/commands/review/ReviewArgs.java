package com.dtsx.docs.commands.review;

import com.dtsx.docs.config.args.BaseArgs;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@ToString
public class ReviewArgs extends BaseArgs<ReviewCtx> {
    @Option(
        names = { "-p", "--port" },
        description = "Port to run the dashboard on.",
        defaultValue = "${DASHBOARD_PORT:-3000}",
        paramLabel = "PORT"
    )
    public int $port;

    @Option(
        names = { "-sf", "--snapshots-folder" },
        description = "Path to the folder containing snapshots.",
        defaultValue = "${SNAPSHOTS_FOLDER:-snapshots}",
        paramLabel = "FOLDER"
    )
    public String $snapshotsFolder;

    @Option(
        names = { "--no-open" },
        description = "Don't automatically open the browser.",
        defaultValue = "${NO_OPEN:-false}"
    )
    public boolean $noOpen;

    @Option(
        names = { "-d", "--detached" },
        description = "Run the dashboard in detached mode (background process).",
        defaultValue = "${DETACHED:-false}"
    )
    public boolean $detached;

    @Override
    public ReviewCtx toCtx(CommandSpec spec) {
        return new ReviewCtx(this, spec);
    }
}
