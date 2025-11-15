# Eclipse E4 UI Event Reference

## Introduction

Eclipse E4 uses a publish/subscribe event model built on top of the OSGi EventAdmin service. The UI framework automatically publishes events when the UI model changes, allowing your application to react to changes in the workbench state. This reference documents the various UI events, their payloads, and common usage patterns.

**Related Documentation:**
- [Event Model](../Event_Model.md) - Overview of the E4 event model and dependency injection
- [Event Processing](../Event_Processing.md) - Design rationale for the publish/subscribe approach
- [UIEvents.java](../../bundles/org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/workbench/UIEvents.java) - Source code with all event topic constants

**External Tutorials:**
- [Eclipse 4 Model Events](https://www.vogella.com/tutorials/Eclipse4ModelEvents/article.html)
- [Eclipse 4 Event System](https://www.vogella.com/tutorials/Eclipse4EventSystem/article.html)

## Event Basics

### Subscribing to Events

There are two ways to subscribe to events:

**1. Dependency Injection (Recommended)**

```java
@Inject
@Optional
public void handleEvent(@UIEventTopic(UIEvents.Part.TOPIC_LABEL) Event event) {
    MPart part = (MPart) event.getProperty(UIEvents.EventTags.ELEMENT);
    String newLabel = (String) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    // Handle the event
}
```

**2. IEventBroker Subscription**

```java
@Inject
IEventBroker eventBroker;

private EventHandler handler = event -> {
    // Handle the event
};

void subscribe() {
    eventBroker.subscribe(UIEvents.Part.TOPIC_LABEL, handler);
}

void unsubscribe() {
    eventBroker.unsubscribe(handler);
}
```

### Event Properties (EventTags)

All UI model events include the following properties in the event object:

| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplicationElement` | The model element that changed |
| `TYPE` | `String` | Event type: `SET`, `ADD`, `ADD_MANY`, `REMOVE`, `REMOVE_MANY`, `MOVE` |
| `ATTNAME` | `String` | The attribute name that changed |
| `NEW_VALUE` | `Object` | The new value (for SET, ADD, ADD_MANY, MOVE events) |
| `OLD_VALUE` | `Object` | The old value (for SET, REMOVE, REMOVE_MANY, MOVE events) |
| `POSITION` | `Integer` | Position in collection (for ADD, REMOVE, MOVE events) |
| `WIDGET` | `Object` | The SWT widget associated with the element (if applicable) |

### Event Types

| Event Type | Description | Use With |
|------------|-------------|----------|
| `SET` | Single value changed | `NEW_VALUE`, `OLD_VALUE` |
| `ADD` | Single item added to collection | `NEW_VALUE`, `POSITION` |
| `ADD_MANY` | Multiple items added to collection | `NEW_VALUE` (Collection), `POSITION` |
| `REMOVE` | Single item removed from collection | `OLD_VALUE`, `POSITION` |
| `REMOVE_MANY` | Multiple items removed from collection | `OLD_VALUE` (Collection), `POSITION` (int[]) |
| `MOVE` | Item moved within collection | `NEW_VALUE` (element), `OLD_VALUE` (old position), `POSITION` (new position) |

### Utility Methods

The `UIEvents` class provides helper methods for working with events:

```java
// Check event type
if (UIEvents.isADD(event)) { /* handle add */ }
if (UIEvents.isREMOVE(event)) { /* handle remove */ }
if (UIEvents.isSET(event)) { /* handle set */ }
if (UIEvents.isMOVE(event)) { /* handle move */ }

// Handle collections in ADD_MANY/REMOVE_MANY events
for (Object item : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
    // Process each added item
}

// Check if a collection contains a specific element
if (UIEvents.contains(event, UIEvents.EventTags.NEW_VALUE, myElement)) {
    // Element is in the collection
}
```

---

## UI Lifecycle Events

Lifecycle events are manually published by specific workbench operations rather than automatically generated from model changes.

**Topic Base:** `org/eclipse/e4/ui/LifeCycle`

### Part Activation

**Topic:** `UIEvents.UILifeCycle.ACTIVATE`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/activate`

Published when an `MPart` is activated.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPart` | The part that was activated |

**Example:**
```java
@Inject
@Optional
public void partActivated(@UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event event) {
    MPart activatedPart = (MPart) event.getProperty(UIEvents.EventTags.ELEMENT);
    System.out.println("Part activated: " + activatedPart.getLabel());
}
```

**When Published:** When a part receives focus through user interaction or programmatic activation via `EPartService.activate()`.

---

### Bring to Top

**Topic:** `UIEvents.UILifeCycle.BRINGTOTOP`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/bringToTop`

Published when a UI element is brought to the top of its container.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element brought to top |

**Example:**
```java
@Inject
@Optional
public void elementBroughtToTop(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    // Handle element brought to top
}
```

**When Published:** When `EModelService.bringToTop()` is called.

---

### Perspective Events

#### Perspective Opened

**Topic:** `UIEvents.UILifeCycle.PERSPECTIVE_OPENED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/perspOpened`

Published when a perspective is opened.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPerspective` | The perspective that was opened |

**Example:**
```java
@Inject
@Optional
public void perspectiveOpened(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_OPENED) Event event) {
    MPerspective perspective = (MPerspective) event.getProperty(UIEvents.EventTags.ELEMENT);
    System.out.println("Perspective opened: " + perspective.getLabel());
}
```

---

#### Perspective Saved

**Topic:** `UIEvents.UILifeCycle.PERSPECTIVE_SAVED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/perpSaved`

Published when a perspective's state is saved.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPerspective` | The perspective that was saved |

**Example:**
```java
@Inject
@Optional
public void perspectiveSaved(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_SAVED) Event event) {
    MPerspective perspective = (MPerspective) event.getProperty(UIEvents.EventTags.ELEMENT);
    // Handle perspective save
}
```

---

#### Perspective Reset

**Topic:** `UIEvents.UILifeCycle.PERSPECTIVE_RESET`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/perspReset`

Published when a perspective is reset to its default state.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPerspective` | The perspective that was reset |

**Example:**
```java
@Inject
@Optional
public void perspectiveReset(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_RESET) Event event) {
    MPerspective perspective = (MPerspective) event.getProperty(UIEvents.EventTags.ELEMENT);
    System.out.println("Perspective reset: " + perspective.getLabel());
}
```

---

#### Perspective Switched

**Topic:** `UIEvents.UILifeCycle.PERSPECTIVE_SWITCHED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/perspSwitched`

Published when the active perspective changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPerspective` | The new active perspective |

**Example:**
```java
@Inject
@Optional
public void perspectiveSwitched(@UIEventTopic(UIEvents.UILifeCycle.PERSPECTIVE_SWITCHED) Event event) {
    MPerspective perspective = (MPerspective) event.getProperty(UIEvents.EventTags.ELEMENT);
    System.out.println("Switched to perspective: " + perspective.getLabel());
}
```

---

### Application Lifecycle Events

#### Application Startup Complete

**Topic:** `UIEvents.UILifeCycle.APP_STARTUP_COMPLETE`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/appStartupComplete`

Published when the application has completed startup.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplication` | The application |

**Example:**
```java
@Inject
@Optional
public void appStartupComplete(@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) Event event) {
    // Perform post-startup initialization
    System.out.println("Application startup complete");
}
```

**Alternative Usage with @EventTopics:**
```java
@EventTopics(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE)
public class StartupHandler implements EventHandler {
    @Override
    public void handleEvent(Event event) {
        // Handle startup
    }
}
```

---

#### Application Shutdown Started

**Topic:** `UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/appShutdownStarted`

Published when the application is beginning shutdown.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplication` | The application |

**Example:**
```java
@Inject
@Optional
public void appShutdownStarted(@UIEventTopic(UIEvents.UILifeCycle.APP_SHUTDOWN_STARTED) Event event) {
    // Perform cleanup before shutdown
    System.out.println("Application shutting down");
}
```

---

### Theme Events

#### Theme Changed

**Topic:** `UIEvents.UILifeCycle.THEME_CHANGED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/themeChanged`

Published when the active theme is changed.

**Event Properties:**
The event data may be null or contain theme-related information.

**Example:**
```java
@Inject
@Optional
public void themeChanged(@UIEventTopic(UIEvents.UILifeCycle.THEME_CHANGED) Event event) {
    // Update UI based on new theme
    System.out.println("Theme changed");
}
```

---

#### Theme Definition Changed

**Topic:** `UIEvents.UILifeCycle.THEME_DEFINITION_CHANGED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/themeDefinitionChanged`

Published when the theme definition (CSS, color definitions, etc.) has changed.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplication` | The application |

**Example:**
```java
@Inject
@Optional
public void themeDefinitionChanged(
        @UIEventTopic(UIEvents.UILifeCycle.THEME_DEFINITION_CHANGED) Event event) {
    // Reload theme definitions
}
```

---

### Activities Changed

**Topic:** `UIEvents.UILifeCycle.ACTIVITIES_CHANGED`  
**Full Topic:** `org/eclipse/e4/ui/LifeCycle/activitiesChanged`

Published when activities (capabilities) are enabled or disabled.

**Event Properties:**
Activity-related information.

**Example:**
```java
@Inject
@Optional
public void activitiesChanged(@UIEventTopic(UIEvents.UILifeCycle.ACTIVITIES_CHANGED) Event event) {
    // Update UI based on changed activities
}
```

---

## Element Container Events

Element containers (like `MPerspectiveStack`, `MPartStack`, `MPartSashContainer`) fire events when their children or selected element changes.

**Affected Types:** `MPerspectiveStack`, `MPartStack`, `MPartSashContainer`, `MTrimBar`, `MToolBar`, `MMenu`, etc.

### Selected Element Changed

**Topic:** `UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/ElementContainer/selectedElement/*`

Published when the selected element in a container changes (e.g., tab selection in a part stack).

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MElementContainer<?>` | The container whose selection changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `MUIElement` | The newly selected element |
| `OLD_VALUE` | `MUIElement` | The previously selected element |

**Example:**
```java
@Inject
@Optional
public void selectedElementChanged(
        @UIEventTopic(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    // Check if it's a part stack
    if (element instanceof MPartStack) {
        MPartStack stack = (MPartStack) element;
        MPart selectedPart = (MPart) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        if (selectedPart != null) {
            System.out.println("Selected part: " + selectedPart.getLabel());
        }
    }
    
    // Check if it's a perspective stack
    if (element instanceof MPerspectiveStack) {
        MPerspectiveStack perspStack = (MPerspectiveStack) element;
        MPerspective selectedPersp = (MPerspective) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        if (selectedPersp != null) {
            System.out.println("Selected perspective: " + selectedPersp.getLabel());
        }
    }
}
```

**Use Cases:**
- Track which part is active in a stack
- React to perspective switches
- Update UI based on selected element

---

### Children Changed

**Topic:** `UIEvents.ElementContainer.TOPIC_CHILDREN`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/ElementContainer/children/*`

Published when children are added to or removed from a container.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MElementContainer<?>` | The container whose children changed |
| `TYPE` | `String` | `ADD`, `ADD_MANY`, `REMOVE`, `REMOVE_MANY`, `MOVE` |
| `NEW_VALUE` | `MUIElement` or `Collection<?>` | Added elements |
| `OLD_VALUE` | `MUIElement` or `Collection<?>` | Removed elements |
| `POSITION` | `Integer` or `int[]` | Position of the change |

**Example:**
```java
@Inject
@Optional
public void childrenChanged(
        @UIEventTopic(UIEvents.ElementContainer.TOPIC_CHILDREN) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (!(element instanceof MPerspectiveStack)) {
        return;
    }
    
    MPerspectiveStack perspStack = (MPerspectiveStack) element;
    
    if (UIEvents.isADD(event)) {
        for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
            MPerspective added = (MPerspective) o;
            System.out.println("Perspective added: " + added.getLabel());
        }
    } else if (UIEvents.isREMOVE(event)) {
        for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.OLD_VALUE)) {
            MPerspective removed = (MPerspective) o;
            System.out.println("Perspective removed: " + removed.getLabel());
        }
    }
}
```

**Use Cases:**
- Track parts added/removed from stacks
- Monitor perspective creation/deletion
- Update UI when container structure changes

---

## UI Element Events

All UI elements (`MUIElement` and subclasses) can fire these events.

**Affected Types:** `MPart`, `MPerspective`, `MWindow`, `MToolBar`, `MMenu`, etc.

### To Be Rendered

**Topic:** `UIEvents.UIElement.TOPIC_TOBERENDERED`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/toBeRendered/*`

Published when an element's `toBeRendered` flag changes (controls whether the element is visible/rendered).

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose rendering state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New rendering state |
| `OLD_VALUE` | `Boolean` | Old rendering state |

**Example:**
```java
@Inject
@Optional
public void toBeRenderedChanged(
        @UIEventTopic(UIEvents.UIElement.TOPIC_TOBERENDERED) Event event) {
    
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    Boolean toBeRendered = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    if (element instanceof MPart) {
        MPart part = (MPart) element;
        System.out.println("Part " + part.getLabel() + 
            " rendering state: " + toBeRendered);
    }
}
```

**Use Cases:**
- Track when parts/perspectives become visible
- Lazy initialization of UI elements
- Cleanup when elements are hidden

---

### Visible

**Topic:** `UIEvents.UIElement.TOPIC_VISIBLE`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/visible/*`

Published when an element's `visible` flag changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose visibility changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New visibility state |
| `OLD_VALUE` | `Boolean` | Old visibility state |

**Example:**
```java
@Inject
@Optional
public void visibleChanged(
        @UIEventTopic(UIEvents.UIElement.TOPIC_VISIBLE) Event event) {
    
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    Boolean visible = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    System.out.println("Element visibility changed to: " + visible);
}
```

---

### Widget

**Topic:** `UIEvents.UIElement.TOPIC_WIDGET`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/widget/*`

Published when the SWT widget associated with a model element changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose widget changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Widget` | The new SWT widget |
| `OLD_VALUE` | `Widget` | The old SWT widget |

**Example:**
```java
@Inject
@Optional
public void widgetChanged(
        @UIEventTopic(UIEvents.UIElement.TOPIC_WIDGET) Event event) {
    
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    Object widget = event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    if (widget != null) {
        System.out.println("Widget created for: " + element.getElementId());
    } else {
        System.out.println("Widget disposed for: " + element.getElementId());
    }
}
```

**Use Cases:**
- Know when SWT widgets are created
- Perform widget-level customization after creation
- Cleanup when widgets are disposed

---

### On Top

**Topic:** `UIEvents.UIElement.TOPIC_ONTOP`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/onTop/*`

Published when an element's `onTop` flag changes (indicates if element should be shown on top).

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose onTop state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New onTop state |

---

### Parent

**Topic:** `UIEvents.UIElement.TOPIC_PARENT`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/parent/*`

Published when an element's parent container changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose parent changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `MElementContainer<?>` | The new parent |
| `OLD_VALUE` | `MElementContainer<?>` | The old parent |

---

### Container Data

**Topic:** `UIEvents.UIElement.TOPIC_CONTAINERDATA`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/containerData/*`

Published when an element's container data (layout data) changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUIElement` | The element whose container data changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `String` | New container data |

---

### All UI Element Changes

**Topic:** `UIEvents.UIElement.TOPIC_ALL`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UIElement/*`

Subscribe to this topic to receive all events for any attribute change on UI elements.

**Example:**
```java
@Inject
@Optional
public void uiElementChanged(
        @UIEventTopic(UIEvents.UIElement.TOPIC_ALL) Event event) {
    
    String attName = (String) event.getProperty(UIEvents.EventTags.ATTNAME);
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    
    System.out.println("UI Element changed - attribute: " + attName);
}
```

---

## UI Label Events

Elements that implement `MUILabel` (parts, perspectives, menus, toolbar items) fire these events.

**Affected Types:** `MPart`, `MPerspective`, `MMenuItem`, `MToolItem`, `MHandledMenuItem`, etc.

### Label Changed

**Topic:** `UIEvents.UILabel.TOPIC_LABEL`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UILabel/label/*`

Published when an element's label text changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUILabel` | The element whose label changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `String` | New label text |
| `OLD_VALUE` | `String` | Old label text |

**Example:**
```java
@Inject
@Optional
public void labelChanged(
        @UIEventTopic(UIEvents.UILabel.TOPIC_LABEL) Event event) {
    
    MUILabel element = (MUILabel) event.getProperty(UIEvents.EventTags.ELEMENT);
    String newLabel = (String) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    if (element instanceof MPart) {
        System.out.println("Part label changed to: " + newLabel);
    }
}
```

---

### Icon URI Changed

**Topic:** `UIEvents.UILabel.TOPIC_ICONURI`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UILabel/iconURI/*`

Published when an element's icon URI changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUILabel` | The element whose icon changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `String` | New icon URI |
| `OLD_VALUE` | `String` | Old icon URI |

**Example:**
```java
@Inject
@Optional
public void iconChanged(
        @UIEventTopic(UIEvents.UILabel.TOPIC_ICONURI) Event event) {
    
    MUILabel element = (MUILabel) event.getProperty(UIEvents.EventTags.ELEMENT);
    String newIconURI = (String) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    System.out.println("Icon changed to: " + newIconURI);
}
```

---

### Tooltip Changed

**Topic:** `UIEvents.UILabel.TOPIC_TOOLTIP`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UILabel/tooltip/*`

Published when an element's tooltip changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MUILabel` | The element whose tooltip changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `String` | New tooltip text |
| `OLD_VALUE` | `String` | Old tooltip text |

---

### All Label Changes

**Topic:** `UIEvents.UILabel.TOPIC_ALL`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/UILabel/*`

Subscribe to receive all label-related events (label, icon, tooltip).

**Example:**
```java
@Inject
@Optional
public void labelAttributeChanged(
        @UIEventTopic(UIEvents.UILabel.TOPIC_ALL) Event event) {
    
    String attName = (String) event.getProperty(UIEvents.EventTags.ATTNAME);
    Object newValue = event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    System.out.println("Label attribute changed: " + attName + " = " + newValue);
}
```

---

## Part Events

Specific to `MPart` elements.

### Closeable Changed

**Topic:** `UIEvents.Part.TOPIC_CLOSEABLE`  
**Full Topic:** `org/eclipse/e4/ui/model/basic/Part/closeable/*`

Published when a part's closeable state changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPart` | The part whose closeable state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New closeable state |

---

### Menus Changed

**Topic:** `UIEvents.Part.TOPIC_MENUS`  
**Full Topic:** `org/eclipse/e4/ui/model/basic/Part/menus/*`

Published when menus are added/removed from a part.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPart` | The part whose menus changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `MMenu` or `Collection<?>` | Added menus |
| `OLD_VALUE` | `MMenu` or `Collection<?>` | Removed menus |

---

### Toolbar Changed

**Topic:** `UIEvents.Part.TOPIC_TOOLBAR`  
**Full Topic:** `org/eclipse/e4/ui/model/basic/Part/toolbar/*`

Published when a part's toolbar changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPart` | The part whose toolbar changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `MToolBar` | New toolbar |
| `OLD_VALUE` | `MToolBar` | Old toolbar |

---

## Perspective Events

Specific to `MPerspective` elements.

### Windows Changed

**Topic:** `UIEvents.Perspective.TOPIC_WINDOWS`  
**Full Topic:** `org/eclipse/e4/ui/model/advanced/Perspective/windows/*`

Published when windows (detached windows) are added/removed from a perspective.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MPerspective` | The perspective whose windows changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `MWindow` or `Collection<?>` | Added windows |
| `OLD_VALUE` | `MWindow` or `Collection<?>` | Removed windows |

---

## Window Events

Specific to `MWindow` and `MTrimmedWindow` elements.

### Trim Bars Changed

**Topic:** `UIEvents.TrimmedWindow.TOPIC_TRIMBARS`  
**Full Topic:** `org/eclipse/e4/ui/model/basic/TrimmedWindow/trimBars/*`

Published when trim bars are added/removed from a window.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MTrimmedWindow` | The window whose trim bars changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `MTrimBar` or `Collection<?>` | Added trim bars |

---

### Main Menu Changed

**Topic:** `UIEvents.Window.TOPIC_MAINMENU`  
**Full Topic:** `org/eclipse/e4/ui/model/basic/Window/mainMenu/*`

Published when a window's main menu changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MWindow` | The window whose main menu changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `MMenu` | New main menu |

---

## Menu and Toolbar Events

### Item Enabled Changed

**Topic:** `UIEvents.Item.TOPIC_ENABLED`  
**Full Topic:** `org/eclipse/e4/ui/model/menu/Item/enabled/*`

Published when a menu/toolbar item's enabled state changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MItem` | The item whose enabled state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New enabled state |

**Example:**
```java
@Inject
@Optional
public void itemEnabledChanged(
        @UIEventTopic(UIEvents.Item.TOPIC_ENABLED) Event event) {
    
    MItem item = (MItem) event.getProperty(UIEvents.EventTags.ELEMENT);
    Boolean enabled = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    System.out.println("Item enabled: " + enabled);
}
```

---

### Item Selected Changed

**Topic:** `UIEvents.Item.TOPIC_SELECTED`  
**Full Topic:** `org/eclipse/e4/ui/model/menu/Item/selected/*`

Published when a menu/toolbar item's selected state changes (for check/radio items).

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MItem` | The item whose selected state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New selected state |

---

## Handler Events

### Handlers Changed

**Topic:** `UIEvents.HandlerContainer.TOPIC_HANDLERS`  
**Full Topic:** `org/eclipse/e4/ui/model/commands/HandlerContainer/handlers/*`

Published when handlers are added/removed from a handler container.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MHandlerContainer` | The container whose handlers changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `MHandler` or `Collection<?>` | Added handlers |
| `OLD_VALUE` | `MHandler` or `Collection<?>` | Removed handlers |

**Example:**
```java
@Inject
@Optional
public void handlersChanged(
        @EventTopic(UIEvents.HandlerContainer.TOPIC_HANDLERS) Event event) {
    
    if (UIEvents.isADD(event)) {
        for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
            MHandler handler = (MHandler) o;
            System.out.println("Handler added for command: " + 
                handler.getCommand().getCommandName());
        }
    }
}
```

---

## Context Events

### Context Changed

**Topic:** `UIEvents.Context.TOPIC_CONTEXT`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/Context/context/*`

Published when an element's Eclipse context changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MContext` | The element whose context changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `IEclipseContext` | New context |
| `OLD_VALUE` | `IEclipseContext` | Old context |

---

### Variables Changed

**Topic:** `UIEvents.Context.TOPIC_VARIABLES`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/Context/variables/*`

Published when context variables are added/removed.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MContext` | The element whose variables changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `String` or `Collection<?>` | Added variables |

---

## Dirtyable Events

### Dirty State Changed

**Topic:** `UIEvents.Dirtyable.TOPIC_DIRTY`  
**Full Topic:** `org/eclipse/e4/ui/model/ui/Dirtyable/dirty/*`

Published when a dirtyable element's (typically a part's) dirty state changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MDirtyable` | The element whose dirty state changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `Boolean` | New dirty state |
| `OLD_VALUE` | `Boolean` | Old dirty state |

**Example:**
```java
@Inject
@Optional
public void dirtyChanged(
        @UIEventTopic(UIEvents.Dirtyable.TOPIC_DIRTY) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (element instanceof MPart) {
        MPart part = (MPart) element;
        Boolean dirty = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        System.out.println("Part " + part.getLabel() + 
            " dirty state: " + dirty);
    }
}
```

**Use Cases:**
- Update save button enablement
- Display asterisk in part title
- Track unsaved changes

---

## Application Element Events

These events apply to all model elements (`MApplicationElement`).

### Tags Changed

**Topic:** `UIEvents.ApplicationElement.TOPIC_TAGS`  
**Full Topic:** `org/eclipse/e4/ui/model/application/ApplicationElement/tags/*`

Published when tags are added/removed from an element.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplicationElement` | The element whose tags changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `String` or `Collection<?>` | Added tags |
| `OLD_VALUE` | `String` or `Collection<?>` | Removed tags |

**Example:**
```java
@Inject
@Optional
public void tagsChanged(
        @UIEventTopic(UIEvents.ApplicationElement.TOPIC_TAGS) Event event) {
    
    MApplicationElement element = 
        (MApplicationElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (UIEvents.isADD(event)) {
        for (Object tag : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
            System.out.println("Tag added: " + tag);
        }
    }
}
```

**Use Cases:**
- React to minimized stack states (via tags)
- Track special element states
- Implement custom element behaviors based on tags

---

### Element ID Changed

**Topic:** `UIEvents.ApplicationElement.TOPIC_ELEMENTID`  
**Full Topic:** `org/eclipse/e4/ui/model/application/ApplicationElement/elementId/*`

Published when an element's ID changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplicationElement` | The element whose ID changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `String` | New element ID |
| `OLD_VALUE` | `String` | Old element ID |

---

### Persisted State Changed

**Topic:** `UIEvents.ApplicationElement.TOPIC_PERSISTEDSTATE`  
**Full Topic:** `org/eclipse/e4/ui/model/application/ApplicationElement/persistedState/*`

Published when entries in an element's persisted state map are added/changed/removed.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplicationElement` | The element whose persisted state changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, `SET` |
| `NEW_VALUE` | `Map.Entry<String, String>` | Map entry (for SET events) |
| `OLD_VALUE` | `Map.Entry<String, String>` | Old map entry (for SET events) |

**Example:**
```java
@Inject
@Optional
public void persistedStateChanged(
        @UIEventTopic(UIEvents.ApplicationElement.TOPIC_PERSISTEDSTATE) Event event) {
    
    MApplicationElement element = 
        (MApplicationElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (UIEvents.isSET(event)) {
        @SuppressWarnings("unchecked")
        Map.Entry<String, String> entry = 
            (Map.Entry<String, String>) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        System.out.println("Persisted state changed: " + 
            entry.getKey() + " = " + entry.getValue());
    }
}
```

---

### Transient Data Changed

**Topic:** `UIEvents.ApplicationElement.TOPIC_TRANSIENTDATA`  
**Full Topic:** `org/eclipse/e4/ui/model/application/ApplicationElement/transientData/*`

Published when entries in an element's transient data map are added/changed/removed.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MApplicationElement` | The element whose transient data changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, `SET` |
| `NEW_VALUE` | `Map.Entry<String, Object>` | Map entry (for SET events) |
| `OLD_VALUE` | `Map.Entry<String, Object>` | Old map entry (for SET events) |

---

## Command Events

### Command Changed

**Topic:** `UIEvents.HandledItem.TOPIC_COMMAND`  
**Full Topic:** `org/eclipse/e4/ui/model/menu/HandledItem/command/*`

Published when the command associated with a handled item changes.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MHandledItem` | The item whose command changed |
| `TYPE` | `String` | `SET` |
| `NEW_VALUE` | `MCommand` | New command |
| `OLD_VALUE` | `MCommand` | Old command |

---

## Binding Events

### Key Bindings Changed

**Topic:** `UIEvents.BindingTable.TOPIC_BINDINGS`  
**Full Topic:** `org/eclipse/e4/ui/model/commands/BindingTable/bindings/*`

Published when key bindings are added/removed from a binding table.

**Event Properties:**
| Property | Type | Description |
|----------|------|-------------|
| `ELEMENT` | `MBindingTable` | The binding table that changed |
| `TYPE` | `String` | `ADD`, `REMOVE`, etc. |
| `NEW_VALUE` | `MKeyBinding` or `Collection<?>` | Added bindings |
| `OLD_VALUE` | `MKeyBinding` or `Collection<?>` | Removed bindings |

---

## Renderer Events

### Request Enablement Update

**Topic:** `UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC`  
**Full Topic:** `org/eclipse/e4/ui/renderer/requestEnablementUpdate`

This is a special event that can be sent (not subscribed to in normal use) to request that toolbar items and menu items update their enablement state.

**Sending the Event:**

```java
@Inject
IEventBroker eventBroker;

// Request all toolbar/menu items to update enablement
eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID);

// Request specific element to update enablement
eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, "my.element.id");

// Using a custom selector
Selector selector = new Selector() {
    @Override
    public boolean select(MApplicationElement element) {
        // Return true for elements that should update
        return element.getTags().contains("myTag");
    }
};
eventBroker.send(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, selector);
```

**Use Cases:**
- Update toolbar/menu enablement after context changes
- Refresh UI after model changes that affect enablement
- Force re-evaluation of command enablement

---

## Best Practices

### 1. Use @Optional for Event Handlers

Always mark event handler methods with `@Optional` to prevent injection failures when the method is called before the event broker is available:

```java
@Inject
@Optional
public void handleEvent(@UIEventTopic(UIEvents.Part.TOPIC_LABEL) Event event) {
    // Handle event
}
```

### 2. Filter Events Early

Check the element type early in your event handler to avoid unnecessary processing:

```java
@Inject
@Optional
public void handleEvent(@UIEventTopic(UIEvents.UIElement.TOPIC_VISIBLE) Event event) {
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    // Filter out irrelevant elements early
    if (!(element instanceof MPart)) {
        return;
    }
    
    MPart part = (MPart) element;
    // Process part visibility change
}
```

### 3. Use Topic Wildcards Wisely

Subscribe to specific topics rather than wildcards when possible for better performance:

```java
// Good - specific topic
@UIEventTopic(UIEvents.Part.TOPIC_LABEL)

// Less efficient - receives all label events
@UIEventTopic(UIEvents.UILabel.TOPIC_ALL)
```

### 4. Handle Collection Events Properly

Use `UIEvents.asIterable()` to handle both single and collection events:

```java
if (UIEvents.isADD(event)) {
    for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
        MPart added = (MPart) o;
        // Process each added part
    }
}
```

### 5. Check Event Type

Always check the event type when subscribing to TOPIC_ALL:

```java
@UIEventTopic(UIEvents.UIElement.TOPIC_ALL)
public void handleAllEvents(Event event) {
    String attName = (String) event.getProperty(UIEvents.EventTags.ATTNAME);
    
    if (UIEvents.UIElement.VISIBLE.equals(attName)) {
        // Handle visibility change
    } else if (UIEvents.UIElement.TOBERENDERED.equals(attName)) {
        // Handle rendering change
    }
}
```

### 6. Avoid Cyclic Event Chains

Be careful not to modify the model in response to events if those modifications will trigger the same event, causing an infinite loop:

```java
@Inject
@Optional
public void handleLabelChange(@UIEventTopic(UIEvents.Part.TOPIC_LABEL) Event event) {
    MPart part = (MPart) event.getProperty(UIEvents.EventTags.ELEMENT);
    
    // BAD - this will trigger another label change event!
    // part.setLabel(part.getLabel() + " (modified)");
    
    // GOOD - check if change is needed first
    String newLabel = part.getLabel() + " (modified)";
    if (!newLabel.equals(part.getLabel())) {
        part.setLabel(newLabel);
    }
}
```

### 7. Unsubscribe from IEventBroker

If you subscribe using `IEventBroker.subscribe()`, remember to unsubscribe in your cleanup code:

```java
private EventHandler handler;

@PostConstruct
void init() {
    handler = event -> { /* handle event */ };
    eventBroker.subscribe(UIEvents.Part.TOPIC_LABEL, handler);
}

@PreDestroy
void cleanup() {
    if (handler != null) {
        eventBroker.unsubscribe(handler);
    }
}
```

---

## Common Use Cases

### Tracking Active Part

```java
@Inject
@Optional
public void trackActivePart(@UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event event) {
    MPart activePart = (MPart) event.getProperty(UIEvents.EventTags.ELEMENT);
    // Update your UI based on active part
}
```

### Monitoring Perspective Switches

```java
@Inject
@Optional
public void perspectiveChanged(
        @UIEventTopic(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (element instanceof MPerspectiveStack) {
        MPerspective perspective = 
            (MPerspective) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        if (perspective != null) {
            System.out.println("Switched to: " + perspective.getLabel());
        }
    }
}
```

### Updating UI on Dirty State Changes

```java
@Inject
@Optional
public void dirtyStateChanged(
        @UIEventTopic(UIEvents.Dirtyable.TOPIC_DIRTY) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (element instanceof MPart) {
        MPart part = (MPart) element;
        Boolean dirty = (Boolean) event.getProperty(UIEvents.EventTags.NEW_VALUE);
        
        // Update save button, add asterisk to title, etc.
        updateSaveAction(part, dirty);
    }
}
```

### Tracking Parts Added to Stack

```java
@Inject
@Optional
public void partsChanged(
        @UIEventTopic(UIEvents.ElementContainer.TOPIC_CHILDREN) Event event) {
    
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (!(element instanceof MPartStack)) {
        return;
    }
    
    if (UIEvents.isADD(event)) {
        for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
            MPart part = (MPart) o;
            System.out.println("Part added to stack: " + part.getLabel());
        }
    }
}
```

### Responding to Tag Changes

```java
@Inject
@Optional
public void tagsChanged(
        @UIEventTopic(UIEvents.ApplicationElement.TOPIC_TAGS) Event event) {
    
    MApplicationElement element = 
        (MApplicationElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    
    if (UIEvents.isADD(event)) {
        for (Object tag : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
            if ("Minimized".equals(tag) && element instanceof MPartStack) {
                // Handle stack minimization
            }
        }
    }
}
```

### Initializing When Widgets Are Created

```java
@Inject
@Optional
public void widgetCreated(
        @UIEventTopic(UIEvents.UIElement.TOPIC_WIDGET) Event event) {
    
    MUIElement element = (MUIElement) event.getProperty(UIEvents.EventTags.ELEMENT);
    Object widget = event.getProperty(UIEvents.EventTags.NEW_VALUE);
    
    if (widget != null && element instanceof MPart) {
        MPart part = (MPart) element;
        if ("my.part.id".equals(part.getElementId())) {
            // Perform widget-level initialization
            Composite composite = (Composite) widget;
            // ... customize the widget
        }
    }
}
```

---

## Debugging Events

### Logging All Events

To understand what events are being fired in your application:

```java
@Inject
@Optional
public void logAllEvents(@UIEventTopic(UIEvents.UIModelTopicBase + "/*") Event event) {
    String topic = (String) event.getProperty("event.topics");
    Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
    String type = (String) event.getProperty(UIEvents.EventTags.TYPE);
    String attName = (String) event.getProperty(UIEvents.EventTags.ATTNAME);
    
    System.out.println("Event: " + topic);
    System.out.println("  Type: " + type);
    System.out.println("  Attribute: " + attName);
    System.out.println("  Element: " + element);
}
```

### Event Properties Helper

```java
public void printEventDetails(Event event) {
    System.out.println("Event Properties:");
    for (String property : event.getPropertyNames()) {
        System.out.println("  " + property + " = " + event.getProperty(property));
    }
}
```

---

## Additional Resources

### Source Code

- [UIEvents.java](../../bundles/org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/workbench/UIEvents.java) - All topic constants and helper methods
- [UIEventPublisher.java](../../bundles/org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/internal/workbench/UIEventPublisher.java) - How events are generated from model changes

### Example Usage in Platform

- [PerspectiveSwitcher.java](../../bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/e4/ui/workbench/addons/perspectiveswitcher/PerspectiveSwitcher.java) - Tracks perspective changes
- [PartRenderingEngine.java](../../bundles/org.eclipse.e4.ui.workbench.swt/src/org/eclipse/e4/ui/internal/workbench/swt/PartRenderingEngine.java) - Responds to rendering events
- [HandlerProcessingAddon.java](../../bundles/org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/internal/workbench/addons/HandlerProcessingAddon.java) - Manages handler events

### Related Frameworks

- **OSGi EventAdmin** - The underlying event bus implementation
- **EMF Notification** - Where UI events originate (model changes)
- **Eclipse Dependency Injection** - Used for event handler injection

---

## Summary

The Eclipse E4 UI event system provides a powerful way to react to changes in the workbench model. Key points:

1. **UI model events** are automatically generated when EMF model elements change
2. **Lifecycle events** are manually published for specific operations (activation, perspective switches, etc.)
3. **Use dependency injection** with `@UIEventTopic` for cleaner code
4. **Filter events early** by checking element types and attributes
5. **Handle collections properly** using `UIEvents.asIterable()`
6. **Avoid event cycles** by checking state before making changes

For specific event details, always refer to the [UIEvents.java source code](../../bundles/org.eclipse.e4.ui.workbench/src/org/eclipse/e4/ui/workbench/UIEvents.java) which is the definitive reference for all available event topics and their structure.
