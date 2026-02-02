/**
 * Type definitions for the snapshot review dashboard
 */

/**
 * Base interface for all approved files
 */
export interface BaseApprovedFile {
  id: string;                    // Stable path-based ID: "filename@rootName"
  filePath: string;              // Path to *.approved.txt
  fileContent: string | null;    // Content (null if doesn't exist)
  hasFile: boolean;              // Whether approved file exists
  diffGroups: DiffGroup[];       // All approved files have diff groups (1+ items)
}

/**
 * Shared approved file (shared.approved.txt)
 */
export interface SharedApprovedFile extends BaseApprovedFile {
  type: 'shared';
}

/**
 * Language-specific approved file (<language>.approved.txt)
 */
export interface LanguageApprovedFile extends BaseApprovedFile {
  type: 'language';
  language: string;              // e.g., "java", "python", "typescript"
}

/**
 * Union type for all approved files
 */
export type ApprovedFile = SharedApprovedFile | LanguageApprovedFile;

/**
 * A group of received files with identical content
 */
export interface DiffGroup {
  id: string;                    // Unique identifier within approved file
  languages: string[];           // Languages with identical content (1+ items), sorted alphabetically
  receivedFiles: string[];       // Paths to received files (1+ items)
  receivedContent: string;       // Common content across all files in group
  approvedContent: string | null; // Content of approved file (null if doesn't exist)
}

/**
 * A test root directory containing approved files
 */
export interface TestRoot {
  rootName: string;                           // e.g., "delete-many/colls/all"
  displayName: string;                        // Human-readable name
  approvedFiles: ApprovedFile[];              // Sorted: shared first, then language-specific alphabetically
}

/**
 * Response from GET /api/test-roots
 */
export interface TestRootsResponse {
  testRoots: TestRoot[];
  totalTestRoots: number;
  lastModified: string;          // ISO 8601 timestamp from last-modified.txt
}

/**
 * Request body for POST /api/approve
 */
export interface ApproveRequest {
  approvedFileId: string;        // Stable ID of approved file
  lastModified: string;          // Client's last known timestamp
  expectedReceivedContent: string;  // Content client expects for received file
  expectedApprovedContent?: string; // Content client expects for approved file (optional)
}

/**
 * Request body for POST /api/reject
 */
export interface RejectRequest {
  approvedFileId: string;        // Stable ID of approved file
  lastModified: string;          // Client's last known timestamp
  expectedReceivedContent: string;  // Content client expects for received file
}

/**
 * Success response for approve/reject operations
 */
export interface ActionSuccessResponse {
  success: true;
  message: string;
  lastModified: string;          // New timestamp after operation
}

/**
 * Error response for stale data
 */
export interface StaleDataErrorResponse {
  error: 'STALE_DATA';
  message: string;
  currentLastModified: string;
}
