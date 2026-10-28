package com.collegeai.backend.entity;

public enum DocumentIndexVersionStatus {

    // New version is being prepared.
    STAGING,

    // This version is currently used by chatbot retrieval.
    ACTIVE,

    // A newer version replaced this version.
    RETIRED,

    // Indexing this version failed.
    FAILED
}