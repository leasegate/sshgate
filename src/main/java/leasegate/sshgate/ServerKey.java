package leasegate.sshgate;

import java.util.List;

import datapotter.datahelper.Data;

/// One key the gate holds, named after what it opens. `hosts` are the ssh destinations (as typed:
/// an alias from ~/.ssh/config, a host name, user@host) it may sign for; a login anywhere else is
/// refused without asking. Empty means any host, and every request then needs the passphrase.
@Data
public final class ServerKey extends ServerKey_A {
    String id;
    String label;
    List<String> hosts = List.of();
}
