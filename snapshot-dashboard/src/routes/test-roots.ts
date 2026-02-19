/**
 * GET /api/test-roots - Get all test roots with changes
 */

import { Router, Request, Response } from 'express';
import { scanTestRoots } from '../scanner';

const router = Router();

/**
 * Get all test roots handler
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const examplesDir = process.env.EXAMPLES_DIR!;
    const result = await scanTestRoots(examplesDir);
    res.json(result);
  } catch (error) {
    console.error('Error scanning test roots:', error);
    res.status(500).json({
      error: 'INTERNAL_ERROR',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;
