package leasegate.sshgate;

import java.nio.file.Path;

/// Where sshgate keeps things, and its two pipes: one for status, one ssh speaks the agent
/// protocol to (`IdentityAgent` in ~/.ssh/config).
final class Places {

    static final String
        PRODUCT = "sshgate",
        PIPE = System.getProperty("sshgate.pipe", "leasegate-sshgate"),
        AGENT_PIPE = PIPE + "-agent";

    static Path home() {
        var override = System.getProperty("sshgate.home");
        return override != null ? Path.of(override) : Path.of(System.getProperty("user.home"), "leasegate", "sshgate");
    }

    static Path settings() { return home().resolve("settings.toml"); }

    static Path runDir() { return home().resolve("run"); }

    static Path keysDir() { return home().resolve("keys"); }

    /// The spelling Windows OpenSSH accepts in `IdentityAgent`: forward slashes. Measured
    /// 2026-10-20 with OpenSSH_for_Windows_9.5: `//./pipe/<name>` reaches the agent, while
    /// `\\.\pipe\<name>` (any escaping) gives "No such file or directory".
    static String agentPath() { return "//./pipe/" + AGENT_PIPE; }

    private Places() {}
}
