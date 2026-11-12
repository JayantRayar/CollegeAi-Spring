package com.collegeai.backend.exception;

/**
 * Thrown when the ChromaDB vector store is unavailable
 * or returns an unexpected error during document retrieval.
 *
 * This exception allows callers to distinguish between
 * "no relevant documents found" and "retrieval failed".
 */
public class VectorStoreException extends RuntimeException {

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
