package com.collegeai.backend.dto;

/**
 * Represents whether retrieved college documents
 * contain enough information to answer a question.
 */
public enum AnswerabilityStatus {

    ANSWERABLE,

    PARTIALLY_ANSWERABLE,

    NOT_ANSWERABLE
}