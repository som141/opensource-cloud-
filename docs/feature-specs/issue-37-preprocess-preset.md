# Issue 37. Preprocess Preset Domain

## Goal

Implement the backend preset contract used by future Job creation and Worker execution. The API server owns preset
metadata and validation. Actual OpenCV processing remains Worker responsibility.

## Overall Order

1. Define shared built-in preset names.
2. Define built-in preset metadata, steps, and parameter definitions.
3. Expose preset list API.
4. Expose preset detail API.
5. Expose parameter validation API.
6. Add custom preset skeleton entity.
7. Add custom preset create/list/delete APIs.
8. Document API and Worker preset contract.
9. Add service/controller/entity tests.

## Functional Units

### Built-In Presets

- `A4_SCAN_300DPI`
- `LOW_CONTRAST_SCAN`
- `RECEIPT`
- `NOISY_SCAN`
- `AUTO`

### Parameter Validation

- Unknown parameters are rejected.
- Missing parameters are resolved from defaults when defaults exist.
- Integer/decimal parameters validate range.
- Boolean parameters accept only `true` or `false`.
- Enum parameters validate allowed values.

### Custom Presets

- Custom presets belong to the current user.
- Custom presets are derived from a built-in base preset.
- Custom preset parameters are validated against the base preset.
- Delete is a soft delete.
- Job/Worker connection for custom presets is intentionally deferred.

## API Surface

- `GET /api/v1/preprocess/presets`
- `GET /api/v1/preprocess/presets/{presetName}`
- `POST /api/v1/preprocess/presets/validate`
- `POST /api/v1/preprocess/presets/custom`
- `GET /api/v1/preprocess/presets/custom`
- `DELETE /api/v1/preprocess/presets/custom/{presetId}`

## Out Of Scope

- API-side OpenCV execution.
- Worker preset registry implementation.
- Job creation integration.
- Frontend custom preset UI.

## Verification

- Registry tests cover required built-in preset names.
- Validator tests cover defaults, unknown parameters, range errors, and enum errors.
- Service tests cover list/detail/validate and custom preset lifecycle.
- Controller tests cover common response codes for custom create/delete.
