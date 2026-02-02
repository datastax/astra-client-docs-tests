/**
 * POST /api/approve - Approve all diff groups for an approved file
 */

import { Router, Request, Response } from 'express';
import * as fs from 'fs/promises';
import * as path from 'path';
import { ApproveRequest, ActionSuccessResponse, StaleDataErrorResponse } from '../types';
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
 * Verify file contents match expected values
 */
async function verifyFileContents(
  receivedFiles: string[],
  approvedPath: string,
  expectedReceivedContent: string,
  expectedApprovedContent?: string
): Promise<{ valid: boolean; reason?: string }> {
  // Read and compare received file content
  const actualReceivedContent = await fs.readFile(receivedFiles[0], 'utf-8');
  
  if (actualReceivedContent !== expectedReceivedContent) {
    return {
      valid: false,
      reason: `Received file has been modified externally (${actualReceivedContent.length} bytes vs expected ${expectedReceivedContent.length} bytes). Please refresh.`
    };
  }
  
  // If approved file content was provided, verify it too
  if (expectedApprovedContent !== undefined) {
    try {
      const actualApprovedContent = await fs.readFile(approvedPath, 'utf-8');
      
      if (actualApprovedContent !== expectedApprovedContent) {
        return {
          valid: false,
          reason: `Approved file has been modified externally (${actualApprovedContent.length} bytes vs expected ${expectedApprovedContent.length} bytes). Please refresh.`
        };
      }
    } catch (error) {
      // Approved file doesn't exist - this is OK for new files
      if (expectedApprovedContent !== null) {
        return {
          valid: false,
          reason: 'Approved file was deleted. Please refresh.'
        };
      }
    }
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
  snapshotsDir: string,
  rootName: string,
  filename: string
): Promise<string[]> {
  const dirPath = path.join(snapshotsDir, rootName);
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
 * Approve handler
 */
router.post('/', async (req: Request, res: Response) => {
  try {
    const { 
      approvedFileId, 
      lastModified: clientLastModified,
      expectedReceivedContent,
      expectedApprovedContent
    } = req.body as ApproveRequest;
    const snapshotsDir = process.env.SNAPSHOTS_DIR!;
    
    if (!approvedFileId || !clientLastModified || !expectedReceivedContent) {
      return res.status(400).json({
        error: 'INVALID_REQUEST',
        message: 'Missing required fields: approvedFileId, lastModified, expectedReceivedContent'
      });
    }
    
    // Validate lastModified
    const validation = await validateLastModified(snapshotsDir, clientLastModified);
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
    const approvedPath = path.join(snapshotsDir, rootName, filename);
    
    // Find all received files
    const receivedFiles = await findReceivedFiles(snapshotsDir, rootName, filename);
    
    if (receivedFiles.length === 0) {
      return res.status(404).json({
        error: 'NOT_FOUND',
        message: `No received files found for ${approvedFileId}`
      });
    }
    
    // Verify file contents match expected values
    const contentVerification = await verifyFileContents(
      receivedFiles,
      approvedPath,
      expectedReceivedContent,
      expectedApprovedContent
    );
    
    if (!contentVerification.valid) {
      const response: StaleDataErrorResponse = {
        error: 'STALE_DATA',
        message: contentVerification.reason!,
        currentLastModified: validation.currentLastModified
      };
      return res.status(409).json(response);
    }
    
    // Read content from first received file (they should all be identical for shared files)
    const newContent = await fs.readFile(receivedFiles[0], 'utf-8');
    
    // Update approved file
    await fs.writeFile(approvedPath, newContent, 'utf-8');
    
    // Delete all received files
    await Promise.all(receivedFiles.map(f => fs.unlink(f)));
    
    // Count languages and diff groups
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
    
    const response: ActionSuccessResponse = {
      success: true,
      message: `Approved ${filename} affecting ${languages.size} language${languages.size === 1 ? '' : 's'}`,
      lastModified: new Date().toISOString()
    };
    
    res.json(response);
  } catch (error) {
    console.error('Error in approve handler:', error);
    res.status(500).json({
      error: 'INTERNAL_ERROR',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;
