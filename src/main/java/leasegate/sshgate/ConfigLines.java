package leasegate.sshgate;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import leasegate.ui.ApprovalParts;
import leasegate.ui.FKeys;

/// The lines that send ssh to the gate for each host, to add to that host's block in
/// ~/.ssh/config: the gate's agent, the key's PUBLIC file, and only that key. sshgate does not edit
/// the config itself; the file is the human's.
final class ConfigLines {

    static String text(Settings s) {
        var out = new ArrayList<String>();
        for (var k : s.getKey()) {
            out.add("# " + k.getId() + ": " + k.getLabel() + " -- add to the Host block of " + (k.getHosts().isEmpty() ? "each host" : String.join(", ", k.getHosts())));
            out.add("    IdentityAgent " + Places.agentPath());
            out.add("    IdentityFile " + Places.keysDir().resolve(k.getId() + ".pub").toString().replace('\\', '/'));
            out.add("    IdentitiesOnly yes");
            out.add("");
        }
        return String.join("\n", out);
    }

    static void show(Settings s) {
        var stage = new Stage();
        var area = new TextArea(text(s));
        area.setEditable(false);
        area.setPrefRowCount(10);
        area.setStyle("-fx-font-family: Consolas, monospace;");
        Runnable copy = () -> { var c = new ClipboardContent(); c.putString(area.getText()); Clipboard.getSystemClipboard().setContent(c); };
        var keys = new FKeys().bind(KeyCode.F2, "Copy", "#2e6da8", copy).bind(KeyCode.ESCAPE, "Close", "#777", stage::close);
        var root = new VBox(10, new Label("Put these lines in ~/.ssh/config, replacing that host's old IdentityFile line:"), area, keys.bar,
            ApprovalParts.small("F2 copy · Esc close", "#777"));
        root.setPadding(new Insets(14));
        root.setPrefWidth(640);
        var scene = new Scene(root);
        keys.install(scene);
        stage.setTitle("sshgate: ssh config lines");
        stage.setScene(scene);
        stage.show();
    }

    private ConfigLines() {}
}
