package com.andrerinas.openheadunit.main

import com.andrerinas.openheadunit.App
import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

/**
 * The real App, with the one thing Robolectric leaves null filled in before onCreate reads it.
 * Used by the tests that need the app component (settings) to exist.
 */
class TestApp : App(), TestLifecycleApplication {

    override fun beforeTest(method: Method?) {
        applicationInfo.nativeLibraryDir = "/system/lib64"
    }

    override fun afterTest(method: Method?) {}

    override fun prepareTest(application: Any?) {}
}
