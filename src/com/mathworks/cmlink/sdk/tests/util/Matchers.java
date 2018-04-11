/* Copyright 2016 The MathWorks, Inc. */

/* Copyright (c) Microsoft Corporation */
package com.mathworks.cmlink.sdk.tests.util;

import com.mathworks.cmlink.api.LocalStatus;
import com.mathworks.cmlink.api.Revision;
import com.mathworks.cmlink.api.version.r16b.FileState;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;

public class Matchers {
    private Matchers() {
    }

    /**
     * Used to ensure that the each Map entry has a value which is satisfied by the specified matcher.
     */
    public static <T> Matcher<? super Map<File, T>> allValues(final Matcher<T> matcher) {
        final Map<File, T> fViolatingEntries = new HashMap<>();
        return new TypeSafeMatcher<Map<File, T>>() {
            @Override
            protected boolean matchesSafely(Map<File, T> fileMap) {
                for (Map.Entry<File, T> entry : fileMap.entrySet()) {
                    if (!matcher.matches(entry.getValue())) {
                        fViolatingEntries.put(entry.getKey(), entry.getValue());
                    }
                }
                return fViolatingEntries.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Map has values which all satisfy the following condition:\n  ");
                matcher.describeTo(description);
                description.appendText("\n  Non conforming map entries:\n");
                for (Map.Entry<File, T> entry : fViolatingEntries.entrySet()) {
                    description.appendText("    " + entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
        };
    }

    /**
     * Pass through matcher for is, used to make hamcrest statements read better.
     */
    public static <T> Matcher<T> are(T value) {
        return is(value);
    }

    public static <T> Matcher<Iterable<T>> hasItems(Collection<T> values) {
        //noinspection unchecked
        return CoreMatchers.hasItems((T[]) values.toArray());
    }

    /**
     * A matcher used to assert that a FileState instance returns the specified LocalStatus.
     */
    public static Matcher<FileState> haveStatus(final LocalStatus status) {
        return new TypeSafeMatcher<FileState>() {
            @Override
            protected boolean matchesSafely(FileState fileState) {
                return fileState != null && fileState.getLocalStatus() == status;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("FileState has the LocalStatus property " + status);
            }
        };
    }

    /**
     * A matcher used to assert that a FileState instance returns the specified LocalStatus.
     */
    public static Matcher<FileState> areLocked() {
        return new TypeSafeMatcher<FileState>() {
            @Override
            protected boolean matchesSafely(FileState fileState) {
                return fileState != null && fileState.hasLock();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("FileState has the property hasLock: true");
            }
        };
    }

    /**
     * Use to match a String as being empty.
     */
    public static Matcher<String> emptyString() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String s) {
                return s.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String is empty");
            }
        };
    }

    /**
     * Used to match a a revision as being more recent that a specified revision.
     */
    public static Matcher<? super Revision> moreRecentThan(final Revision benchmarkRevision) {
        return new TypeSafeMatcher<Revision>() {
            @Override
            protected boolean matchesSafely(Revision revision) {
                return revision.compareTo(benchmarkRevision) > 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Revision should have been greater than " + benchmarkRevision);
            }
        };
    }

}
