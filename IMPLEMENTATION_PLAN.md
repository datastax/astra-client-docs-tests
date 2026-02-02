# Snapshot Review Dashboard - Implementation Plan

## Overview

A web-based dashboard to streamline reviewing snapshot test results. The dashboard uses flat sequential navigation through all approved files across all test roots, with contextual display showing both test root and approved file position.

## Architecture

### Technology Stack
- **Backend**: Node.js + TypeScript + Express
- **Frontend**: Vanilla HTML + CSS + JavaScript (no frameworks)
- **Diff Library**: jsdiff (client-side, included via CDN)
- **Port**: Configurable via `PORT` environment variable (default: 3000)
- **Snapshots Directory**: Configurable via `SNAPSHOTS_DIR` environment variable (required)

### Project Structure

```
snapshot-dashboard/
├── src/
│   ├── server.ts              # Express server entry point
│   ├── scanner.ts             # Filesystem scanner for snapshot files
│   ├── types.ts               # TypeScript type definitions
│   └── routes/
│       ├── test-roots.ts      # GET /api/test-roots (all test roots with changes)
│       ├── approve.ts         # POST /api/approve
│       └── reject.ts          # POST /api/reject
├── public/
│   ├── index.html             # Main dashboard UI
│   ├── styles.css             # Styling
│   └── app.js                 # Frontend logic (includes client-side diffing)
├── package.json
├── tsconfig.json
└── README.md
```

## Core Concepts & Terminology

### Test Root
A directory containing client language files and a `meta.yml` file. Each test root can have multiple approved files.

### Approved File (ApprovedFile)
A single `*.approved.txt` file. Two types:
- **Shared**: `shared.approved.txt` - shared across multiple languages
- **Language-Specific**: `<language>.approved.txt` (e.g., `java.approved.txt`) - specific to one language

### Diff Group (DiffGroup)
A group of received files that need to be reviewed against an approved file:
- **For shared approved files**: Multiple `shared.<language>.received.txt` files with **identical content** are grouped together
- **For language-specific approved files**: Always exactly 1 diff group containing the single `<language>.received.txt` file (treated as single-item group for UI consistency and future extensibility)

**Example Hierarchy:**
```
Test Root: delete-many/colls/all
├── Shared Approved File: shared.approved.txt
│   ├── Diff Group 1: [java, python] (identical content: "Hello, Earth!")
│   └── Diff Group 2: [typescript] (different content: "Goodbye!")
├── Language Approved File: java.approved.txt
│   └── Diff Group: [java] (single-item group)
└── Language Approved File: python.approved.txt
    └── Diff Group: [python] (single-item group)
```

## Sorting Rules

### Test Roots
Sort lexicographically by `rootName` (ascending):
```
delete-many/colls/all
delete-many/colls/filter
delete-many/tables/all
estimate-count/colls
find/colls/filter
projection/colls/nested
```

### Approved Files (within each test root)
1. **Shared first**: `shared.approved.txt` always comes first
2. **Language-specific second**: Sorted alphabetically by language name
```
1. shared.approved.txt
2. csharp.approved.txt
3. java.approved.txt
4. python.approved.txt
5. typescript.approved.txt
```

### Languages (within diff groups)
Sort alphabetically (ascending):
```
[csharp, java, python, typescript]
```

**Example Sorted Output:**
```
Test Root 1: delete-many/colls/all
  - shared.approved.txt
  - java.approved.txt
  - python.approved.txt
  - typescript.approved.txt

Test Root 2: delete-many/colls/filter
  - shared.approved.txt
  - csharp.approved.txt
  - java.approved.txt

Test Root 3: estimate-count/colls
  - shared.approved.txt
```

## Data Models

### TestRoot
```typescript
interface TestRoot {
  rootName: string;                           // e.g., "delete-many/colls/all"
  displayName: string;                        // Human-readable name
  approvedFiles: (SharedApprovedFile | LanguageApprovedFile)[]; // Sorted: shared first, then language-specific alphabetically
}
```

### Base ApprovedFile Interface
```typescript
interface BaseApprovedFile {
  id: string;                    // Stable path-based ID: "filename@rootName"
  filePath: string;              // Path to *.approved.txt
  fileContent: string | null;    // Content (null if doesn't exist)
  hasFile: boolean;              // Whether approved file exists
  diffGroups: DiffGroup[];       // All approved files have diff groups (1+ items)
}
```

**ID Format Examples:**
- `"shared.approved.txt@delete-many/colls/all"`
- `"java.approved.txt@delete-many/colls/all"`
- `"python.approved.txt@projection/colls/nested"`

### SharedApprovedFile
```typescript
interface SharedApprovedFile extends BaseApprovedFile {
  type: 'shared';
  // diffGroups inherited from base - contains multiple groups
}
```

### LanguageApprovedFile
```typescript
interface LanguageApprovedFile extends BaseApprovedFile {
  type: 'language';
  language: string;              // e.g., "java", "python", "typescript"
  // diffGroups inherited from base - always contains exactly 1 group
}
```

### DiffGroup
```typescript
interface DiffGroup {
  id: string;                    // Unique identifier within approved file
  languages: string[];           // Languages with identical content (1+ items), sorted alphabetically
  receivedFiles: string[];       // Paths to received files (1+ items)
  receivedContent: string;       // Common content across all files in group
}
```

**Note**: For language-specific approved files, the single DiffGroup will have:
- `languages: ["java"]` (single language)
- `receivedFiles: ["/path/to/java.received.txt"]` (single file)
- `receivedContent: "..."` (content of that single file)

## Concurrent Modification Handling

### Global Last-Modified Tracking

The docs testing CLI maintains a `last-modified.txt` file in the snapshots directory that is updated whenever snapshots are modified. The dashboard uses this for global staleness detection.

**File Location**: `${SNAPSHOTS_DIR}/last-modified.txt`

**Content Format**: ISO 8601 timestamp
```
2026-02-02T05:54:06.684Z
```

### Client-Side Validation Flow

1. **Initial Load**:
   - Fetch `/api/test-roots`
   - Response includes `lastModified` timestamp from `last-modified.txt`
   - Store in client state

2. **Before Approve/Reject**:
   - Client sends stored `lastModified` timestamp in request
   - Server reads current `last-modified.txt`
   - If timestamps don't match: return error `{ error: 'STALE_DATA', message: 'Snapshots have been modified. Please refresh.' }`
   - If match: proceed with operation

3. **After Successful Operation**:
   - Server returns new `lastModified` timestamp
   - Client updates stored timestamp
   - Client advances to next approved file

4. **On Stale Data Error**:
   - Show modal: "Snapshots have changed. Refreshing..."
   - Reload page to fetch latest state

### Benefits
- **Global consistency**: Detects ANY change to snapshots directory
- **Simple implementation**: Single file check, no per-file tracking
- **Clear UX**: User knows when refresh is needed
- **Race-safe**: Timestamp comparison is atomic

## UI Navigation Flow

### Flat Sequential Navigation

**Navigation Model:**
- All approved files across all test roots form a **flat sequential list**
- Use arrow keys for navigation:
  - **Left/Right arrows**: Navigate between approved files (sequential, crosses test root boundaries)
  - **Up/Down arrows**: Navigate between diff groups within current approved file
- Automatically crosses test root boundaries when using left/right

**Example Sequence (with sorting applied):**
```
1. delete-many/colls/all/shared.approved.txt (2 diff groups)
2. delete-many/colls/all/java.approved.txt (1 diff group)
3. delete-many/colls/all/python.approved.txt (1 diff group)
4. delete-many/colls/filter/shared.approved.txt (3 diff groups)
5. estimate-count/colls/shared.approved.txt (1 diff group)
6. projection/colls/nested/shared.approved.txt (1 diff group)
```

**Display Format:**
```
Test Root 3 of 12 (Approved File 2 of 3)
delete-many/colls/all — python.approved.txt
```

Where:
- "Test Root 3 of 12" = current test root's position among all test roots
- "Approved File 2 of 3" = current approved file's position within its test root
- Shows test root name and approved file name

### Diff Group Selection (within current approved file)
- Sidebar lists all diff groups for the currently active approved file
- Use up/down arrows to navigate between diff groups
- Number diff groups 0-9 for quick access (click or press number)
- Main view shows the selected diff group's diff
- **For language-specific files**: Sidebar shows single diff group (e.g., "[0] Java")
- **For shared files**: Sidebar shows multiple diff groups (e.g., "[0] Java, Python (2 langs)", "[1] TypeScript (1 lang)")

### Layout

```
┌─────────────────────────────────────────────────────────────┐
│ Snapshot Review Dashboard                                    │
│ Test Root 3 of 12 (Approved File 2 of 3)                    │
│ delete-many/colls/all — python.approved.txt                 │
├──────────────┬──────────────────────────────────────────────┤
│              │                                               │
│  Sidebar     │           Main Diff View                      │
│              │                                               │
│  Diff Groups │  ┌─────────────────────────────────────────┐ │
│              │  │ Diff Group 0                            │ │
│  [0] Python  │  │ Language: python                        │ │
│      ◄───    │  │ Command: dh test python -f ...          │ │
│              │  └─────────────────────────────────────────┘ │
│              │                                               │
│              │  ┌─────────────────────────────────────────┐ │
│              │  │ - def old_function():                   │ │
│              │  │ + def new_function():                   │ │
│              │  │       return True                       │ │
│              │  └─────────────────────────────────────────┘ │
│              │                                               │
│              │  [Approve (a)] [Reject (r)]                  │
│              │  [← →] Navigate Files  [↑ ↓] Navigate Groups│
│              │  [Toggle Raw (v)] [Toggle Whitespace (w)]    │
│              │                                               │
└──────────────┴──────────────────────────────────────────────┘
```

**Sidebar for Shared Approved File (multiple diff groups):**
```
Diff Groups
─────────────────
[0] Java, Python
    (2 langs) ◄───
[1] TypeScript
    (1 lang)
```

**Sidebar for Language-Specific Approved File (single diff group):**
```
Diff Groups
─────────────────
[0] Python ◄───
```

## Core Algorithms

### 1. Filesystem Scanner (`scanner.ts`)

**Purpose**: Discover all test roots with changes and organize them.

**Algorithm**:
1. Read `${SNAPSHOTS_DIR}/last-modified.txt` to get global timestamp
2. Recursively scan the snapshots directory
3. For each directory with `*.received.txt` files:
   - Check for `shared.approved.txt` and `shared.<language>.received.txt` files
     - Group received files by content hash
     - Create SharedApprovedFile with stable ID: `"shared.approved.txt@{rootName}"`
     - Create multiple DiffGroups with languages sorted alphabetically
   - Check for `<language>.approved.txt` and `<language>.received.txt` pairs
     - Create LanguageApprovedFile with stable ID: `"{language}.approved.txt@{rootName}"`
     - Create single DiffGroup
4. Sort approved files: shared first, then language-specific alphabetically
5. Group ApprovedFiles by test root
6. Sort test roots lexicographically by rootName
7. Return list of TestRoots with lastModified timestamp

**Key Functions**:
- `scanTestRoots(snapshotsDir: string): Promise<{ testRoots: TestRoot[], lastModified: string }>`
- `readLastModified(snapshotsDir: string): Promise<string>` - Read global timestamp
- `createStableId(filename: string, rootName: string): string` - Generate path-based ID
- `groupSharedReceivedFiles(files: string[]): Promise<Map<string, string[]>>` - Group by content hash
- `createLanguageDiffGroup(language: string, receivedFile: string, content: string): DiffGroup` - Create single-item group
- `sortApprovedFiles(files: ApprovedFile[]): ApprovedFile[]` - Sort shared first, then language-specific alphabetically
- `sortTestRoots(testRoots: TestRoot[]): TestRoot[]` - Sort lexicographically by rootName
- `sortLanguages(languages: string[]): string[]` - Sort alphabetically
- `readFileContent(path: string): Promise<string | null>`

### 2. Diff Group Creation

**For Shared Approved Files:**
1. Find all `shared.<language>.received.txt` files in a directory
2. Read content of each file
3. Calculate SHA-256 hash of content
4. Group files by identical hash
5. For each group, create one DiffGroup object
6. **Sort languages alphabetically** within each diff group
7. Sort groups by number of languages (descending)
8. Assign to parent SharedApprovedFile object with stable ID

**For Language-Specific Approved Files:**
1. Find `<language>.received.txt` file
2. Read content
3. Create single DiffGroup with:
   - `languages: [language]`
   - `receivedFiles: [receivedFilePath]`
   - `receivedContent: content`
4. Assign to parent LanguageApprovedFile object with stable ID

### 3. Flat List Construction (Frontend)

**Algorithm**:
1. Receive test roots and lastModified from API (already sorted)
2. Store lastModified in client state
3. Flatten into sequential list of approved files:
   ```javascript
   const flatList = [];
   testRoots.forEach((testRoot, testRootIndex) => {
     testRoot.approvedFiles.forEach((approvedFile, fileIndex) => {
       flatList.push({
         approvedFile,
         testRoot,
         testRootIndex,
         fileIndexInTestRoot: fileIndex,
         totalFilesInTestRoot: testRoot.approvedFiles.length
       });
     });
   });
   ```
4. Navigate using simple index into flatList
5. Display contextual information from metadata

### 4. Client-Side Diffing (Frontend)

**Algorithm**:
1. Receive approved and received content from API
2. Use jsdiff library: `Diff.diffLines(approved, received, { ignoreWhitespace: false })`
3. Render diff with syntax highlighting
4. Toggle whitespace: re-compute with `ignoreWhitespace: true`
5. Toggle raw view: show full contents side-by-side

## API Endpoints

### GET /api/test-roots
Returns all test roots with changes and global last-modified timestamp. Test roots are sorted lexicographically, approved files are sorted (shared first, then language-specific alphabetically), and languages within diff groups are sorted alphabetically.

**Response**:
```json
{
  "testRoots": [
    {
      "rootName": "delete-many/colls/all",
      "displayName": "Delete Many - Collections - All",
      "approvedFiles": [
        {
          "id": "shared.approved.txt@delete-many/colls/all",
          "type": "shared",
          "filePath": "/path/to/shared.approved.txt",
          "fileContent": "Hello, World!",
          "hasFile": true,
          "diffGroups": [
            {
              "id": "group-0",
              "languages": ["java", "python"],
              "receivedFiles": [
                "/path/to/shared.java.received.txt",
                "/path/to/shared.python.received.txt"
              ],
              "receivedContent": "Hello, Earth!"
            },
            {
              "id": "group-1",
              "languages": ["typescript"],
              "receivedFiles": ["/path/to/shared.typescript.received.txt"],
              "receivedContent": "Goodbye!"
            }
          ]
        },
        {
          "id": "java.approved.txt@delete-many/colls/all",
          "type": "language",
          "filePath": "/path/to/java.approved.txt",
          "fileContent": "Java specific",
          "hasFile": true,
          "language": "java",
          "diffGroups": [
            {
              "id": "group-0",
              "languages": ["java"],
              "receivedFiles": ["/path/to/java.received.txt"],
              "receivedContent": "Java specific updated"
            }
          ]
        }
      ]
    }
  ],
  "totalTestRoots": 12,
  "lastModified": "2026-02-02T05:54:06.684Z"
}
```

### POST /api/approve
Approves ALL diff groups for an approved file by updating the approved file and deleting all received files.

**Request**:
```json
{
  "approvedFileId": "shared.approved.txt@delete-many/colls/all",
  "lastModified": "2026-02-02T05:54:06.684Z"
}
```

**Success Response**:
```json
{
  "success": true,
  "message": "Approved shared.approved.txt affecting 3 languages across 2 diff groups",
  "lastModified": "2026-02-02T05:55:12.123Z"
}
```

**Stale Data Response** (409 Conflict):
```json
{
  "error": "STALE_DATA",
  "message": "Snapshots have been modified. Please refresh.",
  "currentLastModified": "2026-02-02T05:55:00.000Z"
}
```

### POST /api/reject
Rejects ALL diff groups for an approved file by deleting all received files.

**Request**:
```json
{
  "approvedFileId": "shared.approved.txt@delete-many/colls/all",
  "lastModified": "2026-02-02T05:54:06.684Z"
}
```

**Success Response**:
```json
{
  "success": true,
  "message": "Rejected shared.approved.txt affecting 3 languages across 2 diff groups",
  "lastModified": "2026-02-02T05:55:12.123Z"
}
```

**Stale Data Response** (409 Conflict):
```json
{
  "error": "STALE_DATA",
  "message": "Snapshots have been modified. Please refresh.",
  "currentLastModified": "2026-02-02T05:55:00.000Z"
}
```

## Approval/Rejection Behavior

### Shared Approved Files
- **Approve**: Updates `shared.approved.txt` with content from ANY diff group (they should all produce the same approved result), deletes ALL `shared.<language>.received.txt` files
- **Reject**: Deletes ALL `shared.<language>.received.txt` files
- **Cannot partially approve**: All diff groups are approved/rejected together

### Language-Specific Approved Files
- **Approve**: Updates `<language>.approved.txt`, deletes `<language>.received.txt`
- **Reject**: Deletes `<language>.received.txt`
- Same behavior as shared, but only affects single diff group

### Navigation After Action
- After approve/reject, automatically advance to next approved file in sequence
- If on last approved file, show completion message

### Stale Data Handling
- If `lastModified` mismatch detected, show modal and reload page
- User sees fresh data after reload
- No partial state corruption

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `←` (Left Arrow) | Previous approved file (sequential, crosses test root boundaries) |
| `→` (Right Arrow) | Next approved file (sequential, crosses test root boundaries) |
| `↑` (Up Arrow) | Previous diff group within current approved file |
| `↓` (Down Arrow) | Next diff group within current approved file |
| `0-9` | Select diff group by number in sidebar |
| `a` (hold 0.5s) | Approve current approved file (all its diff groups) and auto-advance - shows green progress spinner |
| `r` (hold 0.5s) | Reject current approved file (all its diff groups) and auto-advance - shows red progress spinner |
| `w` | Toggle whitespace diff |
| `v` | Toggle raw view |
| `F5` / `Ctrl+R` / `Cmd+R` | Refresh (reload page to get latest snapshots) |

**Hold-to-Confirm:** The `a` (approve) and `r` (reject) actions require holding the key for 0.5 seconds to prevent accidental actions. A circular donut-shaped progress spinner appears (green for approve, red for reject) that fills as you hold. Release the key before completion to cancel the action.

**Note:** Letter shortcuts (a, r, w, v) only trigger when pressed WITHOUT modifier keys (Ctrl, Cmd, Alt, Shift), preventing conflicts with native browser shortcuts like Cmd+R.

## State Management

```javascript
const state = {
  testRoots: [],                    // All test roots from API (sorted)
  flatApprovedFileList: [],         // Flattened list of all approved files with metadata
  currentIndex: 0,                  // Current position in flat list (0-based)
  selectedDiffGroupIndex: 0,        // Currently selected diff group (0-based)
  showRawView: false,
  ignoreWhitespace: false,
  lastModified: null                // Global timestamp from last-modified.txt
};

// Helper functions
function getCurrentItem() {
  return state.flatApprovedFileList[state.currentIndex];
}

function getCurrentApprovedFile() {
  return getCurrentItem().approvedFile;
}

function getCurrentTestRoot() {
  return getCurrentItem().testRoot;
}

function getCurrentDiffGroup() {
  const approvedFile = getCurrentApprovedFile();
  return approvedFile.diffGroups[state.selectedDiffGroupIndex];
}

function getDisplayInfo() {
  const item = getCurrentItem();
  return {
    testRootNumber: item.testRootIndex + 1,
    totalTestRoots: state.testRoots.length,
    approvedFileNumber: item.fileIndexInTestRoot + 1,
    totalApprovedFilesInTestRoot: item.totalFilesInTestRoot,
    testRootName: item.testRoot.rootName,
    approvedFileName: item.approvedFile.filePath.split('/').pop()
  };
}

async function approveCurrentFile() {
  const approvedFile = getCurrentApprovedFile();
  try {
    const response = await fetch('/api/approve', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        approvedFileId: approvedFile.id,
        lastModified: state.lastModified
      })
    });
    
    if (response.status === 409) {
      // Stale data - show modal and reload
      showStaleDataModal();
      return;
    }
    
    const result = await response.json();
    state.lastModified = result.lastModified;
    advanceToNextFile();
  } catch (error) {
    showError(error.message);
  }
}

function showStaleDataModal() {
  // Show modal: "Snapshots have changed. Refreshing..."
  // Then reload page
  setTimeout(() => window.location.reload(), 1500);
}
```

## Environment Configuration

### Required Environment Variables

### Keyboard Event Handling

**Important:** Letter shortcuts (a, r, w, v) must check for modifier keys to prevent conflicts with native browser shortcuts:

```javascript
document.addEventListener('keydown', (e) => {
  // Ignore if typing in input fields
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
    return;
  }
  
  // For letter keys, ignore if ANY modifier key is pressed
  // This prevents conflicts with browser shortcuts like Cmd+R, Ctrl+W, etc.
  const hasModifier = e.ctrlKey || e.metaKey || e.altKey || e.shiftKey;
  
  if (hasModifier && /^[a-z]$/i.test(e.key)) {
    return; // Let browser handle modified letter keys (Cmd+R, Ctrl+W, etc.)
  }
  
  // Now handle unmodified keys safely
  switch (e.key) {
    case 'a': approveCurrentFile(); break;
    case 'r': rejectCurrentFile(); break;
    // ... etc
  }
});
```

This ensures:
- `r` triggers reject action
- `Cmd+R` / `Ctrl+R` refreshes the page (native browser behavior)
- `w` toggles whitespace
- `Cmd+W` / `Ctrl+W` closes the tab (native browser behavior)



```bash
# Absolute path to snapshots directory
SNAPSHOTS_DIR=/Users/me/work/astra-client-docs-tests/snapshots

# Optional: Server port (default: 3000)
PORT=3000
```

### Startup Validation

Server must validate on startup:
1. `SNAPSHOTS_DIR` is set
2. `SNAPSHOTS_DIR` exists and is a directory
3. `${SNAPSHOTS_DIR}/last-modified.txt` exists
4. If validation fails, exit with clear error message

## Implementation Steps

### Phase 1: Backend Foundation
1. Initialize Node.js + TypeScript project with Express
2. Add environment variable validation for `SNAPSHOTS_DIR`
3. Implement `readLastModified()` to read global timestamp
4. Implement filesystem scanner to discover test roots
5. Implement stable ID generation: `createStableId(filename, rootName)`
6. Implement diff group creation for shared files (by content hash)
7. Implement diff group creation for language-specific files (single-item groups)
8. **Implement sorting functions**: `sortLanguages()`, `sortApprovedFiles()`, `sortTestRoots()`
9. Apply sorting to all data structures
10. Organize approved files by test root
11. Implement GET /api/test-roots endpoint with lastModified

### Phase 2: Action Handlers
12. Implement lastModified validation in approve/reject handlers
13. Implement POST /api/approve endpoint with staleness check
14. Implement POST /api/reject endpoint with staleness check
15. Return 409 Conflict on stale data with current timestamp
16. Add error handling and validation
17. Test with existing snapshot files

### Phase 3: Frontend UI
18. Create HTML structure with sidebar and main view
19. Style with CSS (clean, functional design)
20. Fetch test roots from API on load
21. Store lastModified in client state
22. Build flat approved file list with metadata (already sorted from API)
23. Implement arrow key navigation (left/right for files, up/down for groups)
24. Implement contextual display (test root + approved file position)
25. Implement sidebar rendering (diff groups for current approved file)
26. Implement main diff view rendering

### Phase 4: Client-Side Diffing
27. Include jsdiff library via CDN
28. Implement diff computation in JavaScript
29. Implement diff rendering with syntax highlighting
30. Add raw view toggle
31. Add whitespace toggle

### Phase 5: Interactions & Polish
32. Implement keyboard navigation (arrow keys + letter shortcuts)
33. Implement approve/reject actions with lastModified validation
34. Handle 409 Conflict responses (show modal, reload)
35. Add auto-advance after approve/reject
36. Add progress indicator with dual context
37. Add copy-to-clipboard for test commands
38. Add visual feedback for actions
39. Handle edge cases (no approved file, empty diffs)
40. Handle single diff group (up/down do nothing or wrap)

### Phase 6: Documentation & Testing
41. Write README with usage instructions
42. Document environment variables
43. Document last-modified.txt requirement
44. Test complete workflow
45. Test stale data scenarios
46. Test sorting (test roots, approved files, languages)
47. Document keyboard shortcuts in UI
48. Add startup script to package.json

## Test Command Generation

```typescript
function generateTestCommand(rootName: string, diffGroup: DiffGroup): string {
  const drivers = diffGroup.languages.join(','); // Already sorted alphabetically
  return `dh test ${drivers} -f "${rootName}"`;
}
```

## Edge Cases

1. **No approved file exists**: Show as "new snapshot" with all additions
2. **Empty diffs**: Skip or mark as "no changes"
3. **Large files**: Consider truncation in UI
4. **Invalid file paths**: Validate before operations
5. **Permission errors**: Handle gracefully
6. **Stale data during approve/reject**: Return 409, show modal, reload page
7. **Missing last-modified.txt**: Server fails to start with clear error
8. **Single approved file in test root**: Still show "Approved File 1 of 1"
9. **Single diff group**: Up/down arrows do nothing (or wrap to same item)
10. **Last approved file**: Show completion message instead of advancing
11. **SNAPSHOTS_DIR not set**: Server fails to start with clear error
12. **SNAPSHOTS_DIR doesn't exist**: Server fails to start with clear error

## Non-Goals

- Partial approval of shared changes for subset of languages
- Persisting review state outside filesystem
- Normalizing or modifying diff content
- Supporting non-text snapshot files
- General-purpose diff/review tool features
- Real-time file watching (use manual refresh instead)
- Separate test root skip navigation (just sequential left/right)
- Per-file modification time tracking (use global last-modified.txt)
- Custom sorting options (fixed sorting rules)

## Success Criteria

1. ✅ Scan and discover all test roots with changes
2. ✅ Group identical shared received files into diff groups
3. ✅ Treat language-specific files as single-item diff groups
4. ✅ Generate stable path-based IDs for approved files
5. ✅ Read global last-modified.txt timestamp
6. ✅ Validate lastModified on approve/reject operations
7. ✅ Return 409 Conflict on stale data
8. ✅ Client shows modal and reloads on stale data
9. ✅ **Sort test roots lexicographically by rootName**
10. ✅ **Sort approved files: shared first, then language-specific alphabetically**
11. ✅ **Sort languages alphabetically within diff groups**
12. ✅ Flat sequential navigation through all approved files
13. ✅ Arrow key navigation (left/right for files, up/down for groups)
14. ✅ Contextual display showing test root and approved file position
15. ✅ Show diff groups in sidebar for current approved file (1+ items)
16. ✅ Compute diffs client-side using jsdiff
17. ✅ Approve/reject affects ALL diff groups for an approved file
18. ✅ Auto-advance to next approved file after action
19. ✅ Test rerun commands can be copied
20. ✅ Raw view and whitespace toggles work instantly
21. ✅ Handle missing approved files
22. ✅ All file operations are atomic and safe
23. ✅ Consistent UI for both shared and language-specific files
24. ✅ Server validates SNAPSHOTS_DIR on startup
25. ✅ Server fails gracefully if last-modified.txt missing

## Next Steps

After plan approval, switch to `code` mode to begin implementation.