# sshgate

**Server SSH keys that no program can read, and a human who sees every login.**

An administrator's laptop usually holds the keys to servers as files in `~/.ssh`. Any program that runs as that user can read them, and a shell on a server can do anything its account can. With AI agents and automated tools running commands on the same laptop, that is one prompt injection away from someone else's shell on your server.

sshgate is a [leasegate](https://github.com/leasegate/leasegate) gate for SSH:

- The keys live in sshgate's encrypted vault; sshgate is an **ssh agent** on its own named pipe, and the private key never leaves it.
- Each key names the hosts it may be used for. A login anywhere else is refused without a window.
- Every login goes through the leasegate approval window: which process is asking (and its whole chain, up to the agent or terminal that started it), which host, what command. The human chooses how far up the chain the approval reaches and for how long; it ends when that process exits.
- Ordinary `ssh` keeps working: `~/.ssh/config` points the host at sshgate's agent and at the key's *public* file.

## Setup

1. `~/leasegate/sshgate/settings.toml`:

   ```toml
   windows = 'glow'            # optional: 'glow' (default) or 'native', how sshgate's windows are framed
   resizableWindows = 'glow'   # optional: 'native' keeps Windows' snap and edge resizing for the resizable ones

   [[key]]
   id = 'myserver'
   label = 'the build server'
   hosts = ['myserver']     # destinations as you type them: an alias, a host name, user@host
   ```

2. Start `sshgate.exe` (the tray). Set the passphrase and **write down the recovery key** it shows once.
3. Tray menu, **SSH keys…**: make a key inside sshgate and add its public half to the server's `~/.ssh/authorized_keys` (from a session that already works), or import an existing key file once and delete the file.
4. Tray menu, **ssh config lines…** (or `sshgate config`): put the lines in the host's block of `~/.ssh/config`:

   ```
   Host myserver
       HostName server.example.org
       IdentityAgent //./pipe/leasegate-sshgate-agent
       IdentityFile ~/leasegate/sshgate/keys/myserver.pub
       IdentitiesOnly yes
   ```

   Forward slashes: Windows OpenSSH does not accept `\\.\pipe\` in `IdentityAgent`.

5. `ssh myserver` -- the approval window appears.

Keep a way into each server that does not depend on this laptop (the provider's console or rescue system, or a second key kept offline), before moving the only key into any vault.

## Building

JDK 25, Maven, [leasegate](https://github.com/leasegate/leasegate) installed first. `mvn package`; `mvn -Djrexe -Djr.installDir=<folder on PATH> package` for the exe. Windows only for now.

## License

Apache License 2.0.
