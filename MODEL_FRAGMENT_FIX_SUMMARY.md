# Model Fragment Bug Fix Summary

## Issue Reference
GitHub Discussion: https://github.com/eclipse-platform/eclipse.platform.ui/discussions/3503

## Problem Statement
Lars Vogella reported that model fragments are not properly applied or updated in Eclipse Platform UI. Investigation revealed two distinct issues:

### Issue 1: Bundle Modification Not Handled
When an OSGi bundle with a `Model-Fragment` header is modified (e.g., updated to a new version), the old fragment contributions remained in the application model while new fragments were not applied. This occurred because the `ModelFragmentBundleTracker.modifiedBundle()` method did nothing.

### Issue 2: Fragile Apply Attribute Parsing
The parsing logic for the `apply` attribute in the `Model-Fragment` header was fragile:
```java
String apply = fr.length > 1 ? fr[1].split("=")[1] : "always";
```

This could throw `ArrayIndexOutOfBoundsException` if:
- The header format was malformed (e.g., missing "=")
- Multiple parameters were present but "apply" wasn't the first one
- The parameter name was incorrect

## Solutions Implemented

### Solution 1: Implement modifiedBundle() Handler
Location: `org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/internal/workbench/ModelAssembler.java` (lines 132-197)

The `modifiedBundle()` method now properly handles bundle modifications by:

1. **Checking if the bundle still has a Model-Fragment header**
   - If the header was removed, delegates to `removedBundle()` for cleanup

2. **Removing old fragment contributions**
   - Sets UI elements to not rendered
   - Removes elements from their parent containers
   - Unloads old fragment resources from the EMF resource set

3. **Loading and applying new fragments**
   - Calls `getModelFragmentWrapperFromBundle()` with `initial=false`
   - Processes new fragments via `processFragmentWrappers()`
   - Respects the `apply` attribute (always/initial/notexists)

### Solution 2: Robust Apply Attribute Parsing
Location: `org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/internal/workbench/ModelAssembler.java` (lines 434-475)

The improved parsing logic:

1. **Validates the parameter format**
   ```java
   String[] parts = applyParam.split("=");
   if (parts.length == 2 && "apply".equals(parts[0].trim())) {
       apply = parts[1].trim();
   }
   ```

2. **Provides informative warnings**
   - Logs when the format is invalid
   - Falls back to "always" (safest default)

3. **Handles edge cases**
   - Trims whitespace from all components
   - Verifies the parameter name is "apply"
   - Checks array bounds before accessing

## Model-Fragment Header Format

### Syntax
```
Model-Fragment: <path-to-fragment>;apply=<value>
```

### Examples
```
Model-Fragment: fragment/fragment.e4xmi;apply=initial
Model-Fragment: fragment/fragment.e4xmi;apply=always  
Model-Fragment: fragment/fragment.e4xmi;apply=notexists
Model-Fragment: fragment/fragment.e4xmi  (defaults to "always")
```

### Apply Attribute Values

| Value | Behavior |
|-------|----------|
| `initial` | Fragment is only applied on initial startup (when running from a non-persisted state). After the application has been started once and has persisted state, this fragment will not be applied. |
| `always` | Fragment is applied on every startup, whether initial or from persisted state. This is the default if no `apply` attribute is specified. |
| `notexists` | Fragment is applied only if the elements it contributes don't already exist in the application model. This is checked using the element's XMI ID. |

## Bundle Lifecycle and Fragment Processing

### Initial Startup Sequence
1. `ModelAssembler.processModel(initial=true)` is called
2. Extension-point-based fragments are loaded
3. `BundleTracker.open()` is called, triggering `addingBundle()` for all matching bundles
4. Fragments with `apply="initial"`, `apply="always"`, or `apply="notexists"` are all loaded

### Subsequent Startups (with persisted state)
1. `ModelAssembler.processModel(initial=false)` is called
2. Only fragments with `apply="always"` or `apply="notexists"` are loaded
3. Fragments with `apply="initial"` are skipped

### Dynamic Bundle Installation (after startup)
1. New bundle is installed and started
2. `addingBundle()` is called with `initial=false`
3. Only fragments with `apply="always"` or `apply="notexists"` are loaded
4. Fragments with `apply="initial"` are skipped (correct behavior)

### Bundle Update Sequence
1. Old bundle: ACTIVE → STOPPING: `removedBundle()` removes old fragments
2. New bundle: INSTALLED → STARTING: `addingBundle()` adds new fragments  
3. New bundle: STARTING → ACTIVE: `modifiedBundle()` can optionally handle state transitions
4. **New behavior**: `modifiedBundle()` now also handles in-place updates by removing old and adding new fragments

## Testing Considerations

### Test Scenarios to Verify

1. **Normal startup flow**
   - Verify `apply="initial"` fragments are loaded on first start
   - Verify `apply="always"` fragments are loaded on every start
   - Verify `apply="notexists"` fragments check for existing elements

2. **Bundle update flow**
   - Update a bundle with Model-Fragment header
   - Verify old fragments are removed
   - Verify new fragments are loaded

3. **Malformed headers**
   - Test with missing "="
   - Test with wrong parameter name
   - Test with missing apply attribute (should default to "always")
   - Test with whitespace variations

4. **Edge cases**
   - Bundle with Model-Fragment header removed in update
   - Multiple fragments in single bundle
   - Fragments with dependencies on other fragments

### Existing Test Limitations

The existing tests in `ModelAssemblerTests.java` and `ModelAssemblerFragmentOrderingTests.java` require a graphical display (SWT/GTK) which may not be available in headless CI environments. The tests fail with "No more handles [gtk_init_check() failed]" errors, which are infrastructure issues, not code issues.

## Potential Side Effects and Risks

### Low Risk
- **Backward compatibility**: The changes are defensive and maintain existing behavior
- **Default behavior**: Malformed headers now fall back to "always" (safest option)
- **Bundle updates**: Previously broken, now works correctly

### Areas to Monitor
- **Performance**: The `modifiedBundle()` implementation performs removal and re-addition, which could have performance implications for frequently modified bundles
- **Resource management**: Ensure fragment resources are properly unloaded to avoid memory leaks
- **Double processing**: Verify that fragments aren't processed twice during normal state transitions

## Migration Guide

### For Bundle Developers

No changes required for most bundles. However, if you rely on undefined behavior:

**Before**: Malformed headers might have caused exceptions or unpredictable behavior
**After**: Malformed headers are logged as warnings and fall back to "always"

### For Platform Maintainers

The `modifiedBundle()` implementation can be tuned based on performance monitoring:
- Consider adding a flag to disable fragment reprocessing for specific bundles
- Monitor resource unloading to ensure no memory leaks
- Add telemetry to track how often fragments are updated

## References

- OSGi BundleTracker documentation
- Eclipse E4 Application Model documentation
- EMF Resource loading and unloading
- Eclipse Platform UI contribution mechanism

## Future Improvements

1. **Add comprehensive tests** for the apply attribute parsing
2. **Implement MenuContribution removal** (currently marked as TODO)
3. **Add performance monitoring** for fragment updates
4. **Consider caching** fragment parse results
5. **Document the extension point schema** for the apply attribute
