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
export async function readLastModified(examplesDir: string): Promise<string> {
  const lastModifiedPath = path.join(examplesDir, 'last-modified.txt');

  if (!await fs.stat(lastModifiedPath).catch(() => false)) {
    return new Date(0).toISOString(); // Return epoch if file doesn't exist
  }

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
  } catch (_) {
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
): Promise<DiffGroup[]> {
  const files = await fs.readdir(dirPath);
  const receivedFiles = files
    .filter(f => f.startsWith('shared.') && f.endsWith('.received.txt'))
    .map(f => path.join(dirPath, f));
  
  if (receivedFiles.length === 0) {
    return [];
  }
  
  const approvedPath = path.join(dirPath, 'shared.approved.txt');
  const approvedContent = await readFileContent(approvedPath);
  
  const groups = await groupSharedReceivedFiles(receivedFiles);
  const diffGroups: DiffGroup[] = [];
  
  let groupIndex = 0;
  for (const group of groups.values()) {
    diffGroups.push({
      id: `group-${groupIndex}`,
      languages: sortLanguages(group.languages),
      receivedFiles: group.files,
      receivedContent: group.content,
      approvedContent
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
  receivedFile: string,
  approvedFile: string
): Promise<DiffGroup | null> {
  const content = await readFileContent(receivedFile);
  if (content === null) {
    return null;
  }
  
  const approvedContent = await readFileContent(approvedFile);
  
  return {
    id: 'group-0',
    languages: [language],
    receivedFiles: [receivedFile],
    receivedContent: content,
    approvedContent
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
  const diffGroups = await createSharedDiffGroups(dirPath);
  
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
  const diffGroup = await createLanguageDiffGroup(language, receivedPath, approvedPath);
  
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
function extractRootName(dirPath: string, exampleDir: string): string {
  return path.relative(exampleDir, dirPath);
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
  examplesDir: string
): Promise<TestRoot | null> {
  const snapshotsDir = path.join(dirPath, '_snapshots');

  // Check if _snapshots directory exists
  try {
    await fs.access(snapshotsDir);
  } catch {
    return null; // No _snapshots directory, skip this directory
  }

  const files = await fs.readdir(snapshotsDir);
  const hasReceivedFiles = files.some(f => f.endsWith('.received.txt'));
  
  if (!hasReceivedFiles) {
    return null;
  }
  
  const rootName = extractRootName(dirPath, examplesDir);
  const approvedFiles: ApprovedFile[] = [];
  
  // Check for shared approved file
  const sharedFile = await createSharedApprovedFile(snapshotsDir, rootName);
  if (sharedFile) {
    approvedFiles.push(sharedFile);
  }
  
  // Check for language-specific approved files
  const languages = await discoverLanguages(snapshotsDir);
  for (const language of languages) {
    const langFile = await createLanguageApprovedFile(snapshotsDir, rootName, language);
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
  examplesDir: string,
  testRoots: TestRoot[]
): Promise<void> {
  let entries;
  try {
    entries = await fs.readdir(dirPath, { withFileTypes: true });
  } catch (error) {
    return; // Skip inaccessible directories
  }
  
  // Check current directory
  const testRoot = await scanDirectory(dirPath, examplesDir);
  if (testRoot) {
    testRoots.push(testRoot);
  }
  
  // Recurse into subdirectories
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const subPath = path.join(dirPath, entry.name);
      await scanDirectoryTree(subPath, examplesDir, testRoots);
    }
  }
}

/**
 * Scan snapshots directory and return all test roots with changes
 */
export async function scanTestRoots(examplesDir: string): Promise<TestRootsResponse> {
  const lastModified = await readLastModified(examplesDir);
  const testRoots: TestRoot[] = [];
  
  await scanDirectoryTree(examplesDir, examplesDir, testRoots);
  
  const sortedTestRoots = sortTestRoots(testRoots);
  
  return {
    testRoots: sortedTestRoots,
    totalTestRoots: sortedTestRoots.length,
    lastModified
  };
}
