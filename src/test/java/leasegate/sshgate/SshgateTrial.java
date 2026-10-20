package leasegate.sshgate;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import leasegate.core.LeaseServer;
import leasegate.sshagent.AgentKey;
import leasegate.sshagent.AgentServer;

/// The destination parser, and the one thing everything else rests on: that Windows OpenSSH,
/// given `IdentityAgent \\.\pipe\<name>` (as ~/.ssh/config would say), really asks the gate's
/// agent. A throwaway key is offered to a public ssh server, which turns it down; that it was
/// offered from the agent is the proof. Nothing logs in anywhere.
/// java --enable-native-access=ALL-UNNAMED -cp <classes> leasegate.sshgate.SshgateTrial
public class SshgateTrial {

    static int failed;

    public static void main(String[] args) throws Exception {
        check("a plain alias", Destination.of("ssh.exe myserver").equals(new Destination("myserver", null)));
        check("options with values are skipped", Destination.of("\"C:\\Windows\\System32\\OpenSSH\\ssh.exe\" -p 2222 -i \"C:\\k s\\x.pub\" -o IdentitiesOnly=yes admin@box.example.org uptime -s")
            .equals(new Destination("admin@box.example.org", "uptime -s")));
        check("joined options and -- work", Destination.of("ssh -x -oPermitLocalCommand=no -- host scp -t /tmp").equals(new Destination("host", "scp -t /tmp")));
        check("no destination at all", Destination.of("ssh -V").host() == null);

        var dir = Files.createTempDirectory("sshgate-trial");
        var key = AgentKey.generate("throwaway");
        var pub = dir.resolve("t.pub");
        Files.writeString(pub, key.publicLine() + "\n");
        var pipe = "sshgate-trial-" + ProcessHandle.current().pid();
        var listed = new AtomicInteger();
        var listening = LeaseServer.open(dir, pipe);
        Thread.ofPlatform().daemon().start(() -> new AgentServer(() -> { listed.incrementAndGet(); return List.of(key); }, r -> false).serve(listening, "trial"));
        var p = new ProcessBuilder("C:\\Windows\\System32\\OpenSSH\\ssh.exe", "-v", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new",
            "-o", "IdentityAgent=//./pipe/" + pipe, "-o", "IdentitiesOnly=yes", "-i", pub.toString(), "-T", "git@github.com")
            .redirectErrorStream(true).start();
        var out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        out.lines().filter(l -> l.contains("agent") || l.contains("Offering") || l.contains("denied")).forEach(l -> System.out.println("    " + l));
        check("ssh asked the agent named by IdentityAgent for its keys", listed.get() >= 1);
        check("...and offered the agent's key", out.contains("Offering public key") && out.contains(key.fingerprint()));

        System.out.println(failed == 0 ? "ALL CHECKS PASSED" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void check(String label, boolean ok) {
        if (!ok) failed++;
        System.out.println((ok ? "ok - " : "FAILED - ") + label);
    }
}
