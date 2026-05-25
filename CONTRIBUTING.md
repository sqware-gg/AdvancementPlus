# Contributing

Thanks for improving this Minecraft plugin.

## Before Opening an Issue

Check the README, existing issues, and latest release first. For setup questions, include your server software, server version, Java version, plugin version, relevant config section, and console errors.

Do not paste real tokens, webhook URLs, private IPs, player personal data, or full private configs.

## Bug Reports

Good bug reports include:

- Plugin version.
- Server software and version.
- Java version.
- Other plugins that may interact with the feature.
- Exact commands or actions that reproduce the issue.
- Expected behavior and actual behavior.
- Relevant console log lines.

## Pull Requests

- Keep changes focused.
- Do not commit compiled jars, server folders, generated configs, or local IDE files.
- Preserve existing command and permission behavior unless the PR is explicitly changing it.
- Update README/config documentation when behavior changes.
- Run `mvn package` before submitting.

## Code Style

Prefer clear, boring Java over clever abstractions. Keep plugin behavior predictable for server owners and avoid changes that require server internals unless the project already depends on that API.
