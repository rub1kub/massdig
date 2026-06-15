# Contributing

Thanks for your interest in MassDig.

## Development Setup

Requirements:

- JDK 25
- Git

Build:

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

## Code Style

- Keep changes focused and easy to review.
- Prefer readable Java over clever abstractions.
- Keep user-facing text localized in both `en_us.json` and `ru_ru.json`.
- Avoid adding behavior that attempts to bypass server rules or anti-cheat systems.

## Pull Request Checklist

- The project builds successfully.
- New user-facing strings are localized.
- README or CHANGELOG is updated when behavior changes.
- Server-impacting behavior is documented clearly.

## Responsible Use

MassDig is intended for allowed quality-of-life use. Please do not submit changes whose primary purpose is evading server-side moderation, anti-cheat systems, or server rules.
