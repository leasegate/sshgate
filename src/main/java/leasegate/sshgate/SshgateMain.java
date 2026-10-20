package leasegate.sshgate;

import java.nio.file.Files;

/// `sshgate` with no arguments starts the tray. `sshgate config` prints the ~/.ssh/config lines
/// that send ssh to it; `sshgate pubkey <key>` prints a key's public line.
public final class SshgateMain {

    public static void main(String[] args) {
        if (args.length == 0) { SshgateApp.main(args); return; }
        System.exit(switch (args[0]) {
            case "config" -> { System.out.print(ConfigLines.text(Settings.load(Places.settings()))); yield 0; }
            case "pubkey" -> {
                try { System.out.print(Files.readString(Places.keysDir().resolve(args[1] + ".pub"))); yield 0; }
                catch (Exception e) { System.err.println("sshgate: no public key " + (args.length > 1 ? args[1] : "(name one)")); yield 1; }
            }
            default -> {
                System.err.println("usage: sshgate                 start the tray (it holds the keys)");
                System.err.println("       sshgate config          the ~/.ssh/config lines that send ssh to it");
                System.err.println("       sshgate pubkey <key>    a key's public line");
                yield 2;
            }
        });
    }

    private SshgateMain() {}
}
