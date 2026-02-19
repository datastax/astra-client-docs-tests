/**
 * POST /api/reject - Reject all diff groups for an approved file
 */

import { Router, Request, Response } from 'express';
import * as fs from 'fs/promises';
import * as path from 'path';
import { RejectRequest, ActionSuccessResponse, StaleDataErrorResponse } from '../types';
import { readLastModified } from '../scanner';

const router = Router();

/**
 * Validate that client's lastModified matches current timestamp
 */
async function validateLastModified(
  snapshotsDir: string,
  clientLastModified: string
): Promise<{ valid: boolean; currentLastModified: string }> {
  const currentLastModified = await readLastModified(snapshotsDir);
  return {
    valid: clientLastModified === currentLastModified,
    currentLastModified
  };
}

/**
 * Verify received file content matches expected value
 */
async function verifyReceivedContent(
  receivedFiles: string[],
  expectedContent: string
): Promise<{ valid: boolean; reason?: string }> {
  // For shared files with multiple diff groups, we need to check if ANY received file matches
  // the expected content (since different diff groups have different content)
  let foundMatch = false;
  for (const receivedFile of receivedFiles) {
    const actualContent = await fs.readFile(receivedFile, 'utf-8');
    if (actualContent === expectedContent) {
      foundMatch = true;
      break;
    }
  }
  
  if (!foundMatch) {
    return {
      valid: false,
      reason: `None of the received files match the expected content. Please refresh.`
    };
  }
  
  return { valid: true };
}

/**
 * Parse approved file ID to extract filename and root name
 */
function parseApprovedFileId(id: string): { filename: string; rootName: string } {
  const atIndex = id.lastIndexOf('@');
  if (atIndex === -1) {
    throw new Error(`Invalid approved file ID format: ${id}`);
  }
  return {
    filename: id.substring(0, atIndex),
    rootName: id.substring(atIndex + 1)
  };
}

/**
 * Find all received files for an approved file
 */
async function findReceivedFiles(
  examplesDir: string,
  rootName: string,
  filename: string
): Promise<string[]> {
  const dirPath = path.join(examplesDir, rootName, '_snapshots');
  const files = await fs.readdir(dirPath);
  
  if (filename === 'shared.approved.txt') {
    // Find all shared.<language>.received.txt files
    return files
      .filter(f => f.startsWith('shared.') && f.endsWith('.received.txt'))
      .map(f => path.join(dirPath, f));
  } else {
    // Find <language>.received.txt file
    const language = filename.replace('.approved.txt', '');
    const receivedFile = path.join(dirPath, `${language}.received.txt`);
    try {
      await fs.access(receivedFile);
      return [receivedFile];
    } catch {
      return [];
    }
  }
}

/**
 * Reject handler
 */
router.post('/', async (req: Request, res: Response) => {
  try {
    const { 
      approvedFileId, 
      lastModified: clientLastModified,
      expectedReceivedContent
    } = req.body as RejectRequest;
    const examplesDir = process.env.EXAMPLES_DIR!;
    
    if (!approvedFileId || !clientLastModified || !expectedReceivedContent) {
      return res.status(400).json({
        error: 'INVALID_REQUEST',
        message: 'Missing required fields: approvedFileId, lastModified, expectedReceivedContent'
      });
    }
    
    // Validate lastModified
    const validation = await validateLastModified(examplesDir, clientLastModified);
    if (!validation.valid) {
      const response: StaleDataErrorResponse = {
        error: 'STALE_DATA',
        message: 'Snapshots have been modified. Please refresh.',
        currentLastModified: validation.currentLastModified
      };
      return res.status(409).json(response);
    }
    
    // Parse approved file ID
    const { filename, rootName } = parseApprovedFileId(approvedFileId);
    
    // Find all received files
    const receivedFiles = await findReceivedFiles(examplesDir, rootName, filename);
    
    if (receivedFiles.length === 0) {
      return res.status(404).json({
        error: 'NOT_FOUND',
        message: `No received files found for ${approvedFileId}`
      });
    }
    
    // Verify received file content matches expected value
    const contentVerification = await verifyReceivedContent(
      receivedFiles,
      expectedReceivedContent
    );
    
    if (!contentVerification.valid) {
      const response: StaleDataErrorResponse = {
        error: 'STALE_DATA',
        message: contentVerification.reason!,
        currentLastModified: validation.currentLastModified
      };
      return res.status(409).json(response);
    }
    
    // Delete all received files
    await Promise.all(receivedFiles.map(f => fs.unlink(f)));
    
    // Count languages
    const languages = new Set<string>();
    for (const file of receivedFiles) {
      const basename = path.basename(file);
      if (basename.startsWith('shared.')) {
        const lang = basename.replace('shared.', '').replace('.received.txt', '');
        languages.add(lang);
      } else {
        const lang = basename.replace('.received.txt', '');
        languages.add(lang);
      }
    }
    
    const newLastModified = await readLastModified(examplesDir);
    
    const response: ActionSuccessResponse = {
      success: true,
      message: `Rejected ${filename} affecting ${languages.size} language${languages.size !== 1 ? 's' : ''}`,
      lastModified: newLastModified
    };
    
    res.json(response);
  } catch (error) {
    console.error('Error in reject handler:', error);
    res.status(500).json({
      error: 'INTERNAL_ERROR',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;
