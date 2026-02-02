/**
 * Filesystem scanner for discovering test roots and organizing snapshot files
 */

import * as fs from 'fs/promises';
import * as path from 'path';
import * as crypto from 'crypto';
import {
  TestRoot,
  ApprovedFile,
  SharedApprovedFile,
  LanguageApprovedFile,
  DiffGroup,
  TestRootsResponse
} from './types';

/**
 * Read the global last-modified timestamp
 */
export async function readLastModified(snapshotsDir: string): Promise<string> {
  const lastModifiedPath = path.join(snapshotsDir, 'last-modified.txt');
  try {
    const content = await fs.readFile(lastModifiedPath, 'utf-8');
    return content.trim();
  } catch (error) {
    throw new Error(`Failed to read last-modified.txt: ${error}`);
  }
}

/**
 * Generate stable path-based ID for an approved file
 */
export function createStableId(filename: string, rootName: string): string {
  return `${filename}@${rootName}`;
}

/**
 * Calculate SHA-256 hash of content
 */
function hashContent(content: string): string {
  return crypto.createHash('sha256').update(content).digest('hex');
}

/**
 * Sort languages alphabetically
 */
export function sortLanguages(languages: string[]): string[] {
  return [...languages].sort();
}

/**
 * Sort approved files: shared first, then language-specific alphabetically
 */
export function sortApprovedFiles(files: ApprovedFile[]): ApprovedFile[] {
  return [...files].sort((a, b) => {
    // Shared files come first
    if (a.type === 'shared' && b.type !== 'shared') return -1;
    if (a.type !== 'shared' && b.type === 'shared') return 1;
    
    // Both are language-specific, sort by language name
    if (a.type === 'language' && b.type === 'language') {
      return a.language.localeCompare(b.language);
    }
    
    return 0;
  });
}

/**
 * Sort test roots lexicographically by rootName
 */
export function sortTestRoots(testRoots: TestRoot[]): TestRoot[] {
  return [...testRoots].sort((a, b) => a.rootName.localeCompare(b.rootName));
}

/**
 * Read file content, return null if file doesn't exist
 */
async function readFileContent(filePath: string): Promise<string | null> {
  try {
    return await fs.readFile(filePath, 'utf-8');
  } catch (error) {
    return null;
  }
}

/**
 * Group shared received files by content hash
 */
async function groupSharedReceivedFiles(
  receivedFiles: string[]
): Promise<Map<string, { languages: string[]; files: string[]; content: string }>> {
  const groups = new Map<string, { languages: string[]; files: string[]; content: string }>();
  
  for (const file of receivedFiles) {
    const content = await readFileContent(file);
    if (content === null) continue;
    
    const hash = hashContent(content);
    const language = path.basename(file).replace('shared.', '').replace('.received.txt', '');
    
    if (!groups.has(hash)) {
      groups.set(hash, { languages: [], files: [], content });
    }
    
    const group = groups.get(hash)!;
    group.languages.push(language);
    group.files.push(file);
  }
  
  return groups;
}

/**
 * Create diff groups for a shared approved file
 */
async function createSharedDiffGroups(
  dirPath: string,
  rootName: string
): Promise<DiffGroup[]> {
  const files = await fs.readdir(dirPath);
  const receivedFiles = files
    .filter(f => f.startsWith('shared.') && f.endsWith('.received.txt'))
    .map(f => path.join(dirPath, f));
  
  if (receivedFiles.length === 0) {
    return [];
  }
  
  const groups = await groupSharedReceivedFiles(receivedFiles);
  const diffGroups: DiffGroup[] = [];
  
  let groupIndex = 0;
  for (const [hash, group] of groups.entries()) {
    diffGroups.push({
      id: `group-${groupIndex}`,
      languages: sortLanguages(group.languages),
      receivedFiles: group.files,
      receivedContent: group.content
    });
    groupIndex++;
  }
  
  // Sort by number of languages (descending), then by first language name
  diffGroups.sort((a, b) => {
    if (a.languages.length !== b.languages.length) {
      return b.languages.length - a.languages.length;
    }
    return a.languages[0].localeCompare(b.languages[0]);
  });
  
  return diffGroups;
}

/**
 * Create single diff group for a language-specific approved file
 */
async function createLanguageDiffGroup(
  language: string,
  receivedFile: string
): Promise<DiffGroup | null> {
  const content = await readFileContent(receivedFile);
  if (content === null) {
    return null;
  }
  
  return {
    id: 'group-0',
    languages: [language],
    receivedFiles: [receivedFile],
    receivedContent: content
  };
}

/**
 * Create a shared approved file object
 */
async function createSharedApprovedFile(
  dirPath: string,
  rootName: string
): Promise<SharedApprovedFile | null> {
  const approvedPath = path.join(dirPath, 'shared.approved.txt');
  const fileContent = await readFileContent(approvedPath);
  const diffGroups = await createSharedDiffGroups(dirPath, rootName);
  
  if (diffGroups.length === 0) {
    return null; // No changes for this shared file
  }
  
  return {
    id: createStableId('shared.approved.txt', rootName),
    type: 'shared',
    filePath: approvedPath,
    fileContent,
    hasFile: fileContent !== null,
    diffGroups
  };
}

/**
 * Create a language-specific approved file object
 */
async function createLanguageApprovedFile(
  dirPath: string,
  rootName: string,
  language: string
): Promise<LanguageApprovedFile | null> {
  const approvedPath = path.join(dirPath, `${language}.approved.txt`);
  const receivedPath = path.join(dirPath, `${language}.received.txt`);
  
  const fileContent = await readFileContent(approvedPath);
  const diffGroup = await createLanguageDiffGroup(language, receivedPath);
  
  if (diffGroup === null) {
    return null; // No changes for this language
  }
  
  return {
    id: createStableId(`${language}.approved.txt`, rootName),
    type: 'language',
    filePath: approvedPath,
    fileContent,
    hasFile: fileContent !== null,
    language,
    diffGroups: [diffGroup]
  };
}

/**
 * Extract root name from directory path relative to snapshots directory
 */
function extractRootName(dirPath: string, snapshotsDir: string): string {
  return path.relative(snapshotsDir, dirPath);
}

/**
 * Generate display name from root name
 */
function generateDisplayName(rootName: string): string {
  return rootName
    .split('/')
    .map(part => part.split('-').map(word => 
      word.charAt(0).toUpperCase() + word.slice(1)
    ).join(' '))
    .join(' — ');
}

/**
 * Discover all languages with received files in a directory
 */
async function discoverLanguages(dirPath: string): Promise<string[]> {
  const files = await fs.readdir(dirPath);
  const languages = new Set<string>();
  
  for (const file of files) {
    if (file.endsWith('.received.txt') && !file.startsWith('shared.')) {
      const language = file.replace('.received.txt', '');
      languages.add(language);
    }
  }
  
  return Array.from(languages);
}

/**
 * Scan a directory for approved files with changes
 */
async function scanDirectory(
  dirPath: string,
  snapshotsDir: string
): Promise<TestRoot | null> {
  const files = await fs.readdir(dirPath);
  const hasReceivedFiles = files.some(f => f.endsWith('.received.txt'));
  
  if (!hasReceivedFiles) {
    return null;
  }
  
  const rootName = extractRootName(dirPath, snapshotsDir);
  const approvedFiles: ApprovedFile[] = [];
  
  // Check for shared approved file
  const sharedFile = await createSharedApprovedFile(dirPath, rootName);
  if (sharedFile) {
    approvedFiles.push(sharedFile);
  }
  
  // Check for language-specific approved files
  const languages = await discoverLanguages(dirPath);
  for (const language of languages) {
    const langFile = await createLanguageApprovedFile(dirPath, rootName, language);
    if (langFile) {
      approvedFiles.push(langFile);
    }
  }
  
  if (approvedFiles.length === 0) {
    return null;
  }
  
  return {
    rootName,
    displayName: generateDisplayName(rootName),
    approvedFiles: sortApprovedFiles(approvedFiles)
  };
}

/**
 * Recursively scan directory tree
 */
async function scanDirectoryTree(
  dirPath: string,
  snapshotsDir: string,
  testRoots: TestRoot[]
): Promise<void> {
  let entries;
  try {
    entries = await fs.readdir(dirPath, { withFileTypes: true });
  } catch (error) {
    return; // Skip inaccessible directories
  }
  
  // Check current directory
  const testRoot = await scanDirectory(dirPath, snapshotsDir);
  if (testRoot) {
    testRoots.push(testRoot);
  }
  
  // Recurse into subdirectories
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const subPath = path.join(dirPath, entry.name);
      await scanDirectoryTree(subPath, snapshotsDir, testRoots);
    }
  }
}

/**
 * Scan snapshots directory and return all test roots with changes
 */
export async function scanTestRoots(snapshotsDir: string): Promise<TestRootsResponse> {
  const lastModified = await readLastModified(snapshotsDir);
  const testRoots: TestRoot[] = [];
  
  await scanDirectoryTree(snapshotsDir, snapshotsDir, testRoots);
  
  const sortedTestRoots = sortTestRoots(testRoots);
  
  return {
    testRoots: sortedTestRoots,
    totalTestRoots: sortedTestRoots.length,
    lastModified
  };
}
