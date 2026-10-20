package leasegate.sshgate;

import java.nio.file.Files;

/// `sshgate` with no arguments starts the tray. `sshgate setup` puts sshgate's own ssh config in
/// front of OpenSSH (one Include line in ~/.ssh/config, see SshSetup); `sshgate config` prints what
/// that file holds; `sshgate pubkey <key>` prints a key's public line.
public final class SshgateMain {

    public static void main(String[] args) {
        if (args.length == 0) { SshgateApp.main(args); return; }
        System.exit(switch (args[0]) {
            case "setup" -> setup(args.length > 1 && args[1].equals("--undo"));
            case "config" -> { System.out.print(ConfigLines.text(Settings.load(Places.settings()))); yield 0; }
            case "pubkey" -> {
                try { System.out.print(Files.readString(Places.keysDir().resolve(args[1] + ".pub"))); yield 0; }
                catch (Exception e) { System.err.println("sshgate: no public key " + (args.length > 1 ? args[1] : "(name one)")); yield 1; }
            }
            default -> {
                System.err.println("usage: sshgate                 start the tray (it holds the keys)");
                System.err.println("       sshgate setup [--undo]  sshgate's own ssh config, included from ~/.ssh/config by one line");
                System.err.println("       sshgate config          the lines that send ssh to it, per host");
                System.err.println("       sshgate pubkey <key>    a key's public line");
                yield 2;
            }
        });
    }

    static int setup(boolean undo) {
        try {
            var done = undo ? SshSetup.undo() : SshSetup.install(Settings.load(Places.settings()));
            done.forEach(d -> System.out.println((undo ? "undone: " : "set: ") + d));
            if (done.isEmpty()) System.out.println(undo ? "nothing of sshgate's was set" : "already set");
            return 0;
        } catch (Exception e) { System.err.println("sshgate: " + e.getMessage()); return 1; }
    }

    private SshgateMain() {}
}
