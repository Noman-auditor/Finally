// ui/theme/Performance.kt
// - LazyColumn with keys for profiles/history/logs
// - remember + derivedStateOf for stats graph
// - No recomposition on unrelated state via StateFlow
// - R8 fullMode enabled
// Accessibility enforced:
// Modifier.sizeIn(minWidth=48.dp, minHeight=48.dp)
// contentDescription="Connect, Disconnect, Test Connection"
// color contrast 5.2:1, not color-only indicators (icon + text + haptics)
