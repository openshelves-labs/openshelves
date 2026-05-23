package com.openshelves.model.enums;

/**
 * Represents the various roles a contributor can have in relation to a book.
 *
 * <p>These roles are used to classify the involvement of an author, editor,
 * or other personnel in the creation and production of a work.</p>
 */
public enum AuthorRole {
    /** Primary writer of the work. */
    AUTHOR,

    /** Joint primary writer of the work. */
    CO_AUTHOR,

    /** Responsible for editing and curating the content. */
    EDITOR,

    /** Translated the work into another language. */
    TRANSLATOR,

    /** Created illustrations or artwork for the work. */
    ILLUSTRATOR,

    /** Provided photographs for the work. */
    PHOTOGRAPHER,

    /** Wrote the foreword. */
    FOREWORD,

    /** Wrote the introduction. */
    INTRODUCTION,

    /** Wrote the preface. */
    PREFACE,

    /** Wrote the afterword. */
    AFTERWORD,

    /** Made a general contribution to the work. */
    CONTRIBUTOR,

    /** Compiled or assembled the work. */
    COMPILER,

    /** Narrated the audiobook version of the work. */
    NARRATOR
}
