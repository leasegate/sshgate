package leasegate.sshgate;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import leasegate.core.access.*;

import static leasegate.core.access.ScopeLine.*;

/// sshgate's rules: a login to a server is a shell on it, so it is never a "low risk" request. One
/// named host earns an ordinary approval; a key allowed on any host needs the passphrase and is
/// capped at an hour.
final class SshRules implements LeaseRules<SshScope> {

    static final int ANY_HOST_CAP = 60;

    final Settings settings;

    SshRules(Settings settings) { this.settings = settings; }

    @Override public String product() { return Places.PRODUCT; }

    @Override public DurationCaps.Result capByScope(DurationCaps.Result base, SshScope s) {
        return s.host() == null && base.minutes() > ANY_HOST_CAP ? new DurationCaps.Result(ANY_HOST_CAP, "a key for any host") : base;
    }

    @Override public int keyedMaxMinutes(SshScope s) { return 240; }

    @Override public int riskBase(SshScope s) { return s.host() == null ? 2 : 1; }

    @Override public boolean needsPassphrase(SshScope s) { return s.host() == null; }

    @Override public List<ScopeLine> describe(SshScope s) {
        var out = new ArrayList<ScopeLine>();
        out.add(s.host() == null ? wide("server: ANY the key opens") : caution("server: " + s.host() + " -- a shell there can do anything that account can"));
        out.add(narrow("key: " + s.key() + settings.byId(s.key()).map(k -> " (" + k.getLabel() + ")").orElse("")));
        if (s.detail() != null) out.add(note("this request: " + s.detail()));
        return out;
    }

    @Override public ScopeLine summary(SshScope s) {
        return new ScopeLine(s.key() + "  ·  " + (s.host() == null ? "any host" : s.host()), s.host() == null ? Tone.WIDE : Tone.CAUTION);
    }

    @Override public void writeScope(ObjectNode n, SshScope s) {
        n.put("key", s.key());
        if (s.host() != null) n.put("host", s.host());
    }

    @Override public SshScope readScope(JsonNode n) { return new SshScope(n.path("key").asText(), n.hasNonNull("host") ? n.get("host").asText() : null, null, null); }

    @Override public SshScope stored(SshScope s) { return new SshScope(s.key(), s.host(), null, null); }
}
