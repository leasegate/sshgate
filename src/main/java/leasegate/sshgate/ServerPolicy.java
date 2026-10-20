package leasegate.sshgate;

import leasegate.core.LeaseEngine;
import leasegate.core.Outcome;
import leasegate.sshagent.SignPolicy;
import leasegate.sshagent.VaultKeys;

/// Whether an ssh process may log in with one of the gate's keys: the key must be allowed on the
/// host the process is going to (else it is refused with no window), and then the engine answers
/// from a grant or asks the human.
final class ServerPolicy implements SignPolicy {

    final LeaseEngine<SshScope> engine;
    final VaultKeys keys;
    final Settings settings;

    ServerPolicy(LeaseEngine<SshScope> engine, VaultKeys keys, Settings settings) {
        this.engine = engine;
        this.keys = keys;
        this.settings = settings;
    }

    @Override public boolean allow(Request r) {
        var id = keys.owner(r.key());
        if (id.isEmpty()) return false;
        var dest = Destination.of(r.sshCommandLine());
        var allowed = settings.byId(id.get()).map(k -> k.getHosts()).orElse(java.util.List.of());
        if (!allowed.isEmpty() && (dest.host() == null || allowed.stream().noneMatch(h -> h.equalsIgnoreCase(dest.host())))) {
            System.err.println("sshgate: key " + id.get() + " is not for " + dest.host() + " (only " + allowed + "), refused");
            return false;
        }
        var detail = "ssh " + dest.host() + (dest.command() == null ? " (a shell)" : " " + dest.command());
        return engine.attest(r.peer(), r.chain(), new SshScope(id.get(), dest.host(), detail, null), detail) instanceof Outcome.Allowed;
    }
}
