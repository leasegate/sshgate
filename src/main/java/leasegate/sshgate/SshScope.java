package leasegate.sshgate;

import leasegate.core.access.Scope;

/// What an ssh login reaches: one key, on one host. `host` null means any host the key may reach.
/// `detail` -- the command, when one was given -- is shown to the human but not matched.
public record SshScope(String key, String host, String detail, Integer requestedMinutes) implements Scope<SshScope> {

    @Override public boolean covers(SshScope r) {
        return key.equalsIgnoreCase(r.key) && (host == null || host.equalsIgnoreCase(r.host));
    }

    @Override public SshScope narrowedTo(SshScope r) { return new SshScope(key, r.host != null ? r.host : host, r.detail, r.requestedMinutes); }

    @Override public int narrowness() { return 1 + (host != null ? 1 : 0); }
}
