/**
 * Snapshot Review Dashboard - Frontend Application
 */

// Global state
const state = {
  testRoots: [],
  flatApprovedFileList: [],
  currentIndex: 0,
  selectedDiffGroupIndex: 0,
  showRawView: false,
  ignoreWhitespace: false,
  lastModified: null
};

// DOM elements
const elements = {
  loading: document.getElementById('loading'),
  emptyState: document.getElementById('empty-state'),
  mainContent: document.getElementById('main-content'),
  completionState: document.getElementById('completion-state'),
  progressText: document.getElementById('progress-text'),
  testRootName: document.getElementById('test-root-name'),
  approvedFileName: document.getElementById('approved-file-name'),
  diffGroupInfo: document.getElementById('diff-group-info'),
  diffGroupsList: document.getElementById('diff-groups-list'),
  diffView: document.getElementById('diff-view'),
  approveBtn: document.getElementById('approve-btn'),
  rejectBtn: document.getElementById('reject-btn'),
  toggleRawBtn: document.getElementById('toggle-raw'),
  toggleWhitespaceBtn: document.getElementById('toggle-whitespace'),
  refreshBtn: document.getElementById('refresh-btn'),
  staleModal: document.getElementById('stale-modal'),
  errorModal: document.getElementById('error-modal'),
  errorMessage: document.getElementById('error-message'),
  errorCloseBtn: document.getElementById('error-close-btn')
};

/**
 * Initialize the application
 */
async function init() {
  try {
    showLoading();
    await loadTestRoots();
    
    if (state.flatApprovedFileList.length === 0) {
      showEmptyState();
    } else {
      showMainContent();
      render();
      setupEventListeners();
    }
  } catch (error) {
    console.error('Initialization error:', error);
    showError('Failed to load snapshots: ' + error.message);
  }
}

/**
 * Load test roots from API
 */
async function loadTestRoots() {
  const response = await fetch('/api/test-roots');
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  
  const data = await response.json();
  state.testRoots = data.testRoots;
  state.lastModified = data.lastModified;
  
  // Build flat list
  buildFlatList();
}

/**
 * Build flat list of all approved files
 */
function buildFlatList() {
  state.flatApprovedFileList = [];
  
  state.testRoots.forEach((testRoot, testRootIndex) => {
    testRoot.approvedFiles.forEach((approvedFile, fileIndex) => {
      state.flatApprovedFileList.push({
        approvedFile,
        testRoot,
        testRootIndex,
        fileIndexInTestRoot: fileIndex,
        totalFilesInTestRoot: testRoot.approvedFiles.length
      });
    });
  });
}

/**
 * Get current item from flat list
 */
function getCurrentItem() {
  return state.flatApprovedFileList[state.currentIndex];
}

/**
 * Get current approved file
 */
function getCurrentApprovedFile() {
  return getCurrentItem().approvedFile;
}

/**
 * Get current test root
 */
function getCurrentTestRoot() {
  return getCurrentItem().testRoot;
}

/**
 * Get current diff group
 */
function getCurrentDiffGroup() {
  const approvedFile = getCurrentApprovedFile();
  return approvedFile.diffGroups[state.selectedDiffGroupIndex];
}

/**
 * Get display info for current position
 */
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

/**
 * Generate test command for diff group
 */
function generateTestCommand(rootName, diffGroup) {
  const drivers = diffGroup.languages.join(',');
  return `dh test ${drivers} -f "${rootName}"`;
}

/**
 * Render the current state
 */
function render() {
  renderProgress();
  renderTestRootInfo();
  renderDiffGroupsList();
  renderDiffGroupInfo();
  renderDiff();
}

/**
 * Render progress indicator
 */
function renderProgress() {
  const info = getDisplayInfo();
  elements.progressText.textContent = 
    `Test Root ${info.testRootNumber} of ${info.totalTestRoots} ` +
    `(Approved File ${info.approvedFileNumber} of ${info.totalApprovedFilesInTestRoot})`;
}

/**
 * Render test root info
 */
function renderTestRootInfo() {
  const info = getDisplayInfo();
  elements.testRootName.textContent = info.testRootName;
  elements.approvedFileName.textContent = info.approvedFileName;
}

/**
 * Render diff groups list in sidebar
 */
function renderDiffGroupsList() {
  const approvedFile = getCurrentApprovedFile();
  const html = approvedFile.diffGroups.map((group, index) => {
    const isActive = index === state.selectedDiffGroupIndex;
    const languagesText = group.languages.join(', ');
    const countText = group.languages.length === 1 
      ? '1 lang' 
      : `${group.languages.length} langs`;
    
    return `
      <div class="diff-group-item ${isActive ? 'active' : ''}" data-index="${index}">
        <span class="diff-group-number">${index}</span>
        <div class="diff-group-languages">${languagesText}</div>
        <div class="diff-group-count">${countText}</div>
      </div>
    `;
  }).join('');
  
  elements.diffGroupsList.innerHTML = html;
  
  // Add click handlers
  elements.diffGroupsList.querySelectorAll('.diff-group-item').forEach(item => {
    item.addEventListener('click', () => {
      const index = parseInt(item.dataset.index);
      selectDiffGroup(index);
    });
  });
}

/**
 * Render diff group info
 */
function renderDiffGroupInfo() {
  const diffGroup = getCurrentDiffGroup();
  const testRoot = getCurrentTestRoot();
  const command = generateTestCommand(testRoot.rootName, diffGroup);
  
  const languagesBadges = diffGroup.languages
    .map(lang => `<span class="language-badge">${lang}</span>`)
    .join('');
  
  const html = `
    <div class="diff-group-title">Diff Group ${state.selectedDiffGroupIndex}</div>
    <div class="diff-group-languages-list">${languagesBadges}</div>
    <div class="test-command">
      <code>${command}</code>
      <button class="copy-btn" onclick="copyToClipboard('${command.replace(/'/g, "\\'")}')">Copy</button>
    </div>
  `;
  
  elements.diffGroupInfo.innerHTML = html;
}

/**
 * Render diff view
 */
function renderDiff() {
  const approvedFile = getCurrentApprovedFile();
  const diffGroup = getCurrentDiffGroup();
  
  if (state.showRawView) {
    renderRawView(approvedFile, diffGroup);
  } else {
    renderUnifiedDiff(approvedFile, diffGroup);
  }
}

/**
 * Render unified diff view
 */
function renderUnifiedDiff(approvedFile, diffGroup) {
  const approved = approvedFile.fileContent || '';
  const received = diffGroup.receivedContent;
  
  // Use jsdiff library
  const diff = Diff.diffLines(approved, received, { 
    ignoreWhitespace: state.ignoreWhitespace 
  });
  
  let lineNumber = 1;
  const html = diff.map(part => {
    const lines = part.value.split('\n');
    // Remove last empty line if exists
    if (lines[lines.length - 1] === '') {
      lines.pop();
    }
    
    return lines.map(line => {
      let className = 'context';
      let prefix = ' ';
      
      if (part.added) {
        className = 'addition';
        prefix = '+';
      } else if (part.removed) {
        className = 'deletion';
        prefix = '-';
      }
      
      const displayLine = lineNumber;
      if (!part.removed) {
        lineNumber++;
      }
      
      return `
        <div class="diff-line ${className}">
          <span class="diff-line-number">${part.removed ? '' : displayLine}</span>
          <span class="diff-line-content">${prefix} ${escapeHtml(line)}</span>
        </div>
      `;
    }).join('');
  }).join('');
  
  elements.diffView.innerHTML = html || '<p style="color: var(--color-text-secondary);">No differences</p>';
}

/**
 * Render raw view (side-by-side)
 */
function renderRawView(approvedFile, diffGroup) {
  const approved = approvedFile.fileContent || '(no approved file)';
  const received = diffGroup.receivedContent;
  
  const html = `
    <div class="raw-view">
      <div class="raw-view-column">
        <div class="raw-view-title">Approved</div>
        <div class="raw-view-content">${escapeHtml(approved)}</div>
      </div>
      <div class="raw-view-column">
        <div class="raw-view-title">Received</div>
        <div class="raw-view-content">${escapeHtml(received)}</div>
      </div>
    </div>
  `;
  
  elements.diffView.innerHTML = html;
}

/**
 * Escape HTML special characters
 */
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

/**
 * Copy text to clipboard
 */
window.copyToClipboard = function(text) {
  navigator.clipboard.writeText(text).then(() => {
    // Visual feedback
    const btn = event.target;
    const originalText = btn.textContent;
    btn.textContent = 'Copied!';
    setTimeout(() => {
      btn.textContent = originalText;
    }, 1500);
  }).catch(err => {
    console.error('Failed to copy:', err);
  });
};

/**
 * Navigate to previous approved file
 */
function navigatePrevFile() {
  if (state.currentIndex > 0) {
    state.currentIndex--;
    state.selectedDiffGroupIndex = 0;
    render();
  }
}

/**
 * Navigate to next approved file
 */
function navigateNextFile() {
  if (state.currentIndex < state.flatApprovedFileList.length - 1) {
    state.currentIndex++;
    state.selectedDiffGroupIndex = 0;
    render();
  }
}

/**
 * Navigate to previous diff group
 */
function navigatePrevGroup() {
  const approvedFile = getCurrentApprovedFile();
  if (state.selectedDiffGroupIndex > 0) {
    state.selectedDiffGroupIndex--;
    render();
  }
}

/**
 * Navigate to next diff group
 */
function navigateNextGroup() {
  const approvedFile = getCurrentApprovedFile();
  if (state.selectedDiffGroupIndex < approvedFile.diffGroups.length - 1) {
    state.selectedDiffGroupIndex++;
    render();
  }
}

/**
 * Select diff group by index
 */
function selectDiffGroup(index) {
  const approvedFile = getCurrentApprovedFile();
  if (index >= 0 && index < approvedFile.diffGroups.length) {
    state.selectedDiffGroupIndex = index;
    render();
  }
}

/**
 * Toggle raw view
 */
function toggleRawView() {
  state.showRawView = !state.showRawView;
  renderDiff();
}

/**
 * Toggle whitespace in diff
 */
function toggleWhitespace() {
  state.ignoreWhitespace = !state.ignoreWhitespace;
  renderDiff();
}

/**
 * Approve current approved file
 */
async function approveCurrentFile() {
  const approvedFile = getCurrentApprovedFile();
  
  // Disable buttons during operation
  elements.approveBtn.disabled = true;
  elements.rejectBtn.disabled = true;
  
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
      // Stale data
      showStaleDataModal();
      return;
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Approval failed');
    }
    
    const result = await response.json();
    state.lastModified = result.lastModified;
    
    // Advance to next file
    advanceToNextFile();
  } catch (error) {
    console.error('Approve error:', error);
    showError('Failed to approve: ' + error.message);
  } finally {
    elements.approveBtn.disabled = false;
    elements.rejectBtn.disabled = false;
  }
}

/**
 * Reject current approved file
 */
async function rejectCurrentFile() {
  const approvedFile = getCurrentApprovedFile();
  
  // Disable buttons during operation
  elements.approveBtn.disabled = true;
  elements.rejectBtn.disabled = true;
  
  try {
    const response = await fetch('/api/reject', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        approvedFileId: approvedFile.id,
        lastModified: state.lastModified
      })
    });
    
    if (response.status === 409) {
      // Stale data
      showStaleDataModal();
      return;
    }
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Rejection failed');
    }
    
    const result = await response.json();
    state.lastModified = result.lastModified;
    
    // Advance to next file
    advanceToNextFile();
  } catch (error) {
    console.error('Reject error:', error);
    showError('Failed to reject: ' + error.message);
  } finally {
    elements.approveBtn.disabled = false;
    elements.rejectBtn.disabled = false;
  }
}

/**
 * Advance to next approved file after action
 */
function advanceToNextFile() {
  // Remove current item from flat list
  state.flatApprovedFileList.splice(state.currentIndex, 1);
  
  // Check if we're done
  if (state.flatApprovedFileList.length === 0) {
    showCompletionState();
    return;
  }
  
  // Adjust index if needed
  if (state.currentIndex >= state.flatApprovedFileList.length) {
    state.currentIndex = state.flatApprovedFileList.length - 1;
  }
  
  // Reset diff group selection
  state.selectedDiffGroupIndex = 0;
  
  // Re-render
  render();
}

/**
 * Setup event listeners
 */
function setupEventListeners() {
  // Keyboard navigation
  document.addEventListener('keydown', (e) => {
    // Ignore if typing in input
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
      return;
    }
    
    // For letter keys, ignore if ANY modifier key is pressed
    // This prevents conflicts with browser shortcuts like Cmd+R, Ctrl+W, etc.
    const hasModifier = e.ctrlKey || e.metaKey || e.altKey || e.shiftKey;
    
    if (hasModifier && /^[a-z]$/i.test(e.key)) {
      return; // Let browser handle modified letter keys (Cmd+R, Ctrl+W, etc.)
    }
    
    switch (e.key) {
      case 'ArrowLeft':
        e.preventDefault();
        navigatePrevFile();
        break;
      case 'ArrowRight':
        e.preventDefault();
        navigateNextFile();
        break;
      case 'ArrowUp':
        e.preventDefault();
        navigatePrevGroup();
        break;
      case 'ArrowDown':
        e.preventDefault();
        navigateNextGroup();
        break;
      case 'a':
        e.preventDefault();
        approveCurrentFile();
        break;
      case 'r':
        e.preventDefault();
        rejectCurrentFile();
        break;
      case 'v':
        e.preventDefault();
        toggleRawView();
        break;
      case 'w':
        e.preventDefault();
        toggleWhitespace();
        break;
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        e.preventDefault();
        selectDiffGroup(parseInt(e.key));
        break;
    }
  });
  
  // Button clicks
  elements.approveBtn.addEventListener('click', approveCurrentFile);
  elements.rejectBtn.addEventListener('click', rejectCurrentFile);
  elements.toggleRawBtn.addEventListener('click', toggleRawView);
  elements.toggleWhitespaceBtn.addEventListener('click', toggleWhitespace);
  elements.refreshBtn.addEventListener('click', () => window.location.reload());
  elements.errorCloseBtn.addEventListener('click', hideError);
}

/**
 * Show loading state
 */
function showLoading() {
  elements.loading.style.display = 'flex';
  elements.emptyState.style.display = 'none';
  elements.mainContent.style.display = 'none';
  elements.completionState.style.display = 'none';
}

/**
 * Show empty state
 */
function showEmptyState() {
  elements.loading.style.display = 'none';
  elements.emptyState.style.display = 'flex';
  elements.mainContent.style.display = 'none';
  elements.completionState.style.display = 'none';
}

/**
 * Show main content
 */
function showMainContent() {
  elements.loading.style.display = 'none';
  elements.emptyState.style.display = 'none';
  elements.mainContent.style.display = 'flex';
  elements.completionState.style.display = 'none';
}

/**
 * Show completion state
 */
function showCompletionState() {
  elements.loading.style.display = 'none';
  elements.emptyState.style.display = 'none';
  elements.mainContent.style.display = 'none';
  elements.completionState.style.display = 'flex';
}

/**
 * Show stale data modal and reload
 */
function showStaleDataModal() {
  elements.staleModal.style.display = 'flex';
  setTimeout(() => {
    window.location.reload();
  }, 2000);
}

/**
 * Show error modal
 */
function showError(message) {
  elements.errorMessage.textContent = message;
  elements.errorModal.style.display = 'flex';
}

/**
 * Hide error modal
 */
function hideError() {
  elements.errorModal.style.display = 'none';
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', init);
