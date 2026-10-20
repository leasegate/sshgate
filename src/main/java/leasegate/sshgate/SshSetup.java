package leasegate.sshgate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import leasegate.sshagent.SshConfigInclude;

/// `sshgate setup`: sshgate's own ssh config file -- for each key with hosts, a `Host` block naming
/// the agent pipe, the key's PUBLIC file and IdentitiesOnly -- pulled into `~/.ssh/config` by one
/// marked `Include` line at its top (SshConfigInclude: the same pattern gitgate uses, and each gate
/// touches only its own line and refuses a host another gate serves). The user's own Host blocks
/// keep HostName, Port and User. The file is rewritten at every tray start and whenever
/// settings.toml changes.
final class SshSetup {

    static final String GATE = "sshgate";

    static Path own() { return Places.home().resolve("ssh_config"); }

    static Path sshConfig() { return Path.of(System.getProperty("user.home"), ".ssh", "config"); }

    static List<SshConfigInclude.Block> blocks(Settings s) {
        var out = new ArrayList<SshConfigInclude.Block>();
        for (var k : s.getKey()) {
            if (k.getHosts().isEmpty()) continue;   // a key for any host has no Host line to go under
            out.add(new SshConfigInclude.Block(k.getHosts(), List.of(
                "IdentityAgent " + Places.agentPath(),
                "IdentityFile " + Places.keysDir().resolve(k.getId() + ".pub").toString().replace('\\', '/'),
                "IdentitiesOnly yes")));
        }
        return out;
    }

    static List<String> writeFiles(Settings s) throws Exception {
        var blocks = blocks(s);
        var clash = SshConfigInclude.conflicts(sshConfig(), GATE, blocks);
        if (!clash.isEmpty()) throw new IllegalStateException("sshgate's hosts clash with another gate: " + String.join("; ", clash) + ". Change the hosts in " + Places.settings() + ".");
        SshConfigInclude.writeOwn(own(), GATE, Places.settings().toString(), blocks);
        var done = new ArrayList<String>();
        done.add(own().toString());
        for (var k : s.getKey()) if (k.getHosts().isEmpty()) done.add("key " + k.getId() + " has no hosts, so it has no Host block (add it to a Host block in ~/.ssh/config by hand)");
        return done;
    }

    static List<String> install(Settings s) throws Exception {
        var done = writeFiles(s);
        if (SshConfigInclude.install(sshConfig(), GATE, own())) done.add(sshConfig() + ": " + SshConfigInclude.include(own()) + " (backup config.before-sshgate)");
        return done;
    }

    static List<String> undo() throws Exception {
        return SshConfigInclude.undo(sshConfig(), GATE) ? List.of(sshConfig() + ": sshgate's Include line removed") : List.of();
    }

    static boolean installed() {
        try { return Files.exists(sshConfig()) && Files.readString(sshConfig()).contains(SshConfigInclude.marker(GATE)); }
        catch (Exception e) { return false; }
    }

    private SshSetup() {}
}
