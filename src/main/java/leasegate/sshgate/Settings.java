package leasegate.sshgate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import datapotter.datahelper.Data;

/// `~/leasegate/sshgate/settings.toml`: which keys the gate holds and where each may be used. No
/// key material here; the keys are in the vault.
@Data
public final class Settings extends Settings_A {
    List<ServerKey> key = List.of();

    public Optional<ServerKey> byId(String id) { return key.stream().filter(k -> k.getId().equalsIgnoreCase(id)).findFirst(); }

    static final TomlMapper TOML = TomlMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    public static Settings load(Path file) {
        if (!Files.exists(file)) throw new IllegalStateException("sshgate has no settings yet: write " + file + " (see the README)");
        try { return TOML.readValue(file.toFile(), Settings.class); }
        catch (Exception e) { throw new IllegalStateException("cannot read " + file + ": " + e.getMessage(), e); }
    }
}
