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
  examplesDir: string,
  clientLastModified: string
): Promise<{ valid: boolean; currentLastModified: string }> {
  const currentLastModified = await readLastModified(examplesDir);
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
  // For shared files with multiple diff groups, we need to check if ANY received file matches
  // the expected content (since different diff groups have different content)
  let foundMatch = false;
  for (const receivedFile of receivedFiles) {
    const actualReceivedContent = await fs.readFile(receivedFile, 'utf-8');
    if (actualReceivedContent === expectedReceivedContent) {
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
    const approvedPath = path.join(examplesDir, rootName, '_snapshots', filename);
    
    // Find all received files
    const receivedFiles = await findReceivedFiles(examplesDir, rootName, filename);
    
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
    
    // Find the received file that matches the expected content
    // This is critical when there are multiple diff groups with different content
    let matchingFile: string | null = null;
    for (const receivedFile of receivedFiles) {
      const content = await fs.readFile(receivedFile, 'utf-8');
      if (content === expectedReceivedContent) {
        matchingFile = receivedFile;
        break;
      }
    }
    
    if (!matchingFile) {
      // This should never happen since verifyFileContents already checked
      return res.status(500).json({
        error: 'INTERNAL_ERROR',
        message: 'Could not find matching received file'
      });
    }
    
    // Read content from the matching received file
    const newContent = await fs.readFile(matchingFile, 'utf-8');
    
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
    
    const newLastModified = await readLastModified(examplesDir);
    
    const response: ActionSuccessResponse = {
      success: true,
      message: `Approved ${filename} affecting ${languages.size} language${languages.size === 1 ? '' : 's'}`,
      lastModified: newLastModified
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
