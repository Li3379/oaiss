<!-- Parent: ../AGENTS.md -->
# Util Directory — Stateless Utility Classes

> Pure-function utility classes with no Spring context dependency.
> All methods are `static`; classes have `private` constructors.

Generated: 2026-05-19 | Updated: 2026-05-19

## Key Files

| File | Purpose | Key Methods |
|------|---------|-------------|
| `DateUtils.java` | Date formatting, parsing, and calculation helpers | `format()`, `parse()`, `daysBetween()`, `isExpired()` |
| `ValidationUtils.java` | Input validation helpers for common formats | `isValidEmail()`, `isValidPhone()`, `isNotBlank()`, `isValidPageRequest()` |
| `EncryptionUtils.java` | RSA digital-signature operations for carbon reports | `sign()`, `verify()`, `generateKeyPair()`, `publicKeyToString()`, `stringToPublicKey()` |
| `CarbonFormulaUtils.java` | Emission-factor calculation formulas | `calculateEmission()`, `calculateReduction()`, `emissionFactorOf()` |

## Design Principles

- **Stateless**: No instance fields; all methods are `static`.
- **Private constructors**: Prevents accidental instantiation.
- **No Spring dependency**: Utilities are plain Java; they do not inject beans or use `@Component`.
- **Immutable I/O**: Methods accept inputs and return new values; no mutation of arguments.

## Domain Notes

- **EncryptionUtils**: RSA key pairs are generated per-enterprise for digital signatures on carbon reports. Public keys are stored in the database; private keys are held client-side.
- **CarbonFormulaUtils**: Emission factors are lookup-based (not hardcoded). The `emissionFactorOf()` method resolves the factor from a predefined mapping by fuel/activity type.

## AI Agent Guidelines

- **Keep utilities pure**: Do not add Spring `@Autowired` or `@Value` to utility classes. If a utility needs context, promote it to a `@Service` or `@Component` instead.
- **Adding a new utility method**: Place it in the existing class if it fits the category. Only create a new file for a distinct domain concern.
- **Testing**: Utility methods should have 100% unit-test coverage due to their pure-function nature. Test both happy paths and edge cases (null, empty, boundary values).
- **EncryptionUtils caveats**: RSA operations are CPU-intensive for large payloads; use them only for signing digests, not encrypting bulk data.
- **CarbonFormulaUtils**: When adding new emission factors, update both the factor mapping and the corresponding unit tests to validate the formula.
