package leasegate.sshgate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Where an ssh process is going and what it will run there, read from its own command line (taken
/// from the operating system): the first argument that is neither an option nor an option's value
/// is the destination, everything after it the remote command. Windows quoting: double quotes
/// group, a backslash before a quote escapes it.
record Destination(String host, String command) {

    /// ssh's options that take a value (OpenSSH 9.x).
    static final Set<Character> WITH_VALUE = Set.of('B', 'b', 'c', 'D', 'E', 'e', 'F', 'I', 'i', 'J', 'L', 'l', 'm', 'O', 'o', 'p', 'Q', 'R', 'S', 'W', 'w');

    static Destination of(String commandLine) {
        var argv = split(commandLine);
        for (var i = 1; i < argv.size(); i++) {
            var a = argv.get(i);
            if (a.equals("--")) { i++; if (i < argv.size()) return at(argv, i); break; }
            if (a.startsWith("-") && a.length() > 1) {
                var last = a.charAt(a.length() - 1);
                if (a.length() == 2 && WITH_VALUE.contains(last)) i++;
                continue;
            }
            return at(argv, i);
        }
        return new Destination(null, null);
    }

    static Destination at(List<String> argv, int i) {
        var rest = argv.subList(i + 1, argv.size());
        return new Destination(argv.get(i), rest.isEmpty() ? null : String.join(" ", rest));
    }

    static List<String> split(String s) {
        var out = new ArrayList<String>();
        var cur = new StringBuilder();
        var quoted = false;
        var any = false;
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == '"') { cur.append('"'); i++; any = true; }
            else if (c == '"') { quoted = !quoted; any = true; }
            else if (Character.isWhitespace(c) && !quoted) { if (any) { out.add(cur.toString()); cur.setLength(0); any = false; } }
            else { cur.append(c); any = true; }
        }
        if (any) out.add(cur.toString());
        return out;
    }
}
