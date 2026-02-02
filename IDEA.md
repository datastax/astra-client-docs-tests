### Context

This project uses snapshot testing to verify the correctness of client documentation example snippets, and to
identify regressions between client or document versions.

The way it works is that the manually approved snapshots are stored in a `*.approved.txt` file, with two variants:
- `shared.approved.txt`
    - This is where multiple different clients output the same snapshots for a given example, to avoid duplication and help enforce consistency.
- `<client>.approved.txt` (e.g. `python.approved.txt`)
    - This is where certain clients can have their own specific snapshots for a given example, if needed.

When the tests are run, client documentation snippets are executed and their output is compared against the approved snapshots.

If the output matches the approved snapshot, the test passes. If there are differences, the test fails,
and the new output is stored in a `*.received.txt` file for review. The developer can then compare the `*.received.txt`
file with the corresponding `*.approved.txt` file to see what has changed, also with two variants:
- `shared.<client>.received.txt`
    - For outputs that are common across multiple clients for a given example. These all correspond to the single canonical `shared.approved.txt` file for that example.
- `<client>.received.txt`
    - For outputs that are specific to a particular client and example.

### Issue

How do we deal with multiple `shared.<client>.received.txt` files that correspond to a single
`shared.approved.txt` file for a given example?

It's easy to diff a single `shared.<client>.received.txt` file against its corresponding
`shared.approved.txt`, but when there are multiple `shared.<client>.received.txt` files for the same example,
it becomes cumbersome to review each one individually.

The core problem is that these multiple `shared.<client>.received.txt` files usually represent a single logical
change set for a given example, but the current tooling forces the developer to review them as separate diffs.

### Proposed Solution

The idea would be a dashboard to replace @./scripts/review_snapshots.sh that would use
https://github.com/kpdecker/jsdiff to show a unified view of all the diffs for a given example’s
`shared.approved.txt` file against all its corresponding `shared.<client>.received.txt` files.

This dashboard would allow the developer to quickly see:
- which clients have changes
- which clients share the same changes
- what those changes are
and decide whether to approve the changes (by updating the example’s `shared.approved.txt` file) or reject them
(by discarding the corresponding `shared.<client>.received.txt` files).

The layout could be centered around a main diff view, with a sidebar listing all distinct changes detected
when comparing an example’s `shared.approved.txt` against the various `shared.<client>.received.txt` files.

If multiple `shared.<client>.received.txt` files have the _exact same file content_ (down to the byte),
the dashboard could treat them as a single change group, indicating that the same change
applies to multiple clients and only needs to be reviewed once.

See @./snapshots/example for an example of how multiple clients can share the same approved snapshot file.

Each change group in the sidebar would indicate which clients are affected.
Clicking on a change group would update the main diff view to show the differences for that group.

Keyboard shortcuts could be implemented to quickly navigate between change groups and take action:
- e.g.:
    - pressing "n" to go to the next change group
    - "0-9" to pick a specific change group quickly, with groups numbered in the sidebar and ordered by number of affected clients
    - "a" to approve the current change group (updating the example’s `shared.approved.txt`)
    - "r" to reject the current change group (discarding the corresponding received files)
    - "c" to continue to the next failed example without taking action

A locally running API server could serve the dashboard, reading the `*.approved.txt` and `*.received.txt` files
from the filesystem, and providing endpoints to approve or reject change groups.
The server would be intentionally simple and stateless, with all changes being persisted directly to the filesystem
and reviewed via normal git workflows.

The API can use TS; the frontend can use HTML. No need for fancy frameworks or build steps.

### Reviewer Workflow (Example)

1. Run the snapshot tests locally.
2. Start the snapshot review dashboard.
3. The dashboard loads the example’s `shared.approved.txt` and all corresponding `shared.<client>.received.txt` files.
    - If `shared.approved.txt` does not exist yet, the dashboard treats the received files as candidate initial approved snapshots.
4. The sidebar shows one or more change groups for that example, each potentially affecting multiple clients.
5. For each change group:
    - review the unified diff in the main view
    - optionally inspect per-client received output if needed
    - approve the change (updating the example’s `shared.approved.txt`), or
    - reject the change (discarding the corresponding received files), or
    - skip it for now and move on
6. Once all change groups for all examples have been reviewed, commit the resulting filesystem changes using git.

#### Additional workflow ideas

- Show a per-example progress indicator (e.g., “Example 3 of 12”) to help reviewers keep track.
- Allow temporarily viewing the full raw output of `approved` and `received` files for confidence.
- Display a small note or tooltip explaining why each change group exists (e.g., “Grouped because these clients produced identical diffs”).
- Clearly separate examples visually so that approvals/rejections for one example cannot accidentally affect another.
- Add a codeblock which has a copy button which allows you to copy the command to rerun that specific example's test (e.g. `dh test <client(s)> -f <example>`).

### Dashboard Features

- Sidebar listing all change groups for the current example.
- Main diff view for the currently selected change group.
- Keyboard shortcuts for quick navigation and actions (`n`, `0-9`, `a`, `r`, `c`).
- Optional toggle for whitespace-only differences.
- Optional raw view of received vs approved files.
- Visual hint showing which clients belong to each change group.
- Clear handling of examples without an existing `shared.approved.txt`.

In the end though, the goal is to streamline the review process and have a FUNCTIONAL dashboard; not necessarily a
BEAUTIFUL one. Function over form.

### Non-Goals

- Supporting partial approval of a shared change for only a subset of clients.
    - Approving a change updates `shared.approved.txt`, which is shared and cannot differ per client.
- Persisting review state outside of the filesystem or git.
- Replacing snapshot testing itself; this tool only improves the review experience.
- Building a general-purpose diff or code review tool.
- Normalizing the diff
  - Nothing in the diffs or between `shared.approved.txt` and `shared.<client>.received.txt` files should be altered or normalized.
  - Not even whitespace.

This is not a general snapshot approval tool. This is heavily specific to the use case of multiple clients
sharing a single approved snapshot file, and the need to review multiple received files against it.
