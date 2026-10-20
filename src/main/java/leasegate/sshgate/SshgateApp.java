package leasegate.sshgate;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import leasegate.core.LeaseEngine;
import leasegate.core.LeaseServer;
import leasegate.core.Replies;
import leasegate.core.access.*;
import leasegate.sshagent.AgentServer;
import leasegate.sshagent.VaultKeys;
import leasegate.transport.LeaseDeniedException;
import leasegate.transport.Links;
import leasegate.ui.*;

/// The sshgate tray: the passphrase opens the gate and its vault, then the agent ssh logs in
/// through. Every login is approved in the window or by a grant made there.
public final class SshgateApp extends Application {

    Settings settings;
    PassphraseGate gate;

    public static void main(String[] args) {
        System.setProperty("apple.awt.UIElement", "true");
        launch(args);
    }

    @Override public void init() {
        Platform.setImplicitExit(false);
        settings = Settings.load(Places.settings());
        gate = new PassphraseGate(Places.home().resolve("passphrase.json"));
    }

    @Override public void start(Stage stage) {
        if (alreadyRunning()) { System.err.println("sshgate is already running"); Platform.exit(); return; }
        stage.setTitle(Places.PRODUCT);
        var vault = VaultUnlock.open(gate, Places.home().resolve("vault.json"), Places.PRODUCT);
        var keys = new VaultKeys(vault, Places.keysDir());
        keys.writeAllPublic();

        var rules = new SshRules(settings);
        var brand = new Brand(Places.PRODUCT, "#8a4b00", GateIcon.image(SshgateApp.class, "brand.png", "#8a4b00"));
        var engine = new LeaseEngine<SshScope>(rules, new BlanketModeApproval<>(new FxApproval<>(gate, rules, brand), gate),
            new KeyedAppStore<>(Places.home().resolve("keyed-apps.json"), rules));
        engine.trustApproval(new FxTrust<>(gate, rules, brand));
        engine.onExpiring(e -> Platform.runLater(() -> ExpiryWarningWindow.show(engine, gate, brand, e)));
        engine.startSweep();

        var control = LeaseServer.open(Places.runDir(), Places.PIPE);
        var agentPipe = LeaseServer.open(Places.runDir(), Places.AGENT_PIPE);
        var agent = new AgentServer(keys::keys, new ServerPolicy(engine, keys, settings));
        Thread.ofPlatform().name("sshgate-control").daemon().start(() -> LeaseServer.serve(control, "sshgate", a -> {
            try (var io = a.io()) { io.readFrame(); io.writeFrame(Replies.wrap(Replies.M.createObjectNode().put("ok", true).put("product", Places.PRODUCT))); }
        }));
        Thread.ofPlatform().name("sshgate-agent").daemon().start(() -> agent.serve(agentPipe, "sshgate-agent"));

        var entries = settings.getKey().stream().map(k -> new KeysWindow.Entry(k.getId(), k.getLabel())).toList();
        var tray = new TrayShell<>(stage, engine, gate, brand,
            () -> keys.all().size() + " key(s) · " + engine.activeGrants().size() + " approval(s)");
        tray.addItem(TrayShell.item("SSH keys…", () -> KeysWindow.show(Places.PRODUCT, entries, keys,
                "Add it to the server's ~/.ssh/authorized_keys (from a session that already works), then test before removing the old key.", () -> {})))
            .addItem(TrayShell.item("ssh config lines…", () -> ConfigLines.show(settings)))
            .quit("Quit sshgate (logins with its keys stop until it starts again)", vault::close)
            .install();
    }

    static boolean alreadyRunning() {
        try (var io = Links.open(Places.PRODUCT, Places.runDir(), Places.PIPE)) {
            io.writeFrame("{\"gate\":{\"v\":2,\"op\":\"ping\"}}");
            return io.readFrame() != null;
        } catch (LeaseDeniedException e) { return false; }
    }
}
