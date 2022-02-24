package org.lsposed.lspd

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.lsposed.lspd.cases.NewXposedHelpers
import org.lsposed.lspd.cases.OldXposedHelpers
import org.lsposed.lspd.cases.StringBuilderXposedHelpers
import java.util.concurrent.ConcurrentHashMap

/**
 * Benchmarks cache keys on ART.
 */
@RunWith(AndroidJUnit4::class)
class ReflectionCacheBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun findFieldOld() = benchmarkRule.measureRepeated {
        requireNotNull(OldXposedHelpers.findField(ContentResolver::class.java, "mRandom"))
    }


    @Test
    fun findFieldStringBuilder() = benchmarkRule.measureRepeated {
        requireNotNull(
            StringBuilderXposedHelpers.findField(ContentResolver::class.java, "mRandom")
        )
    }

    @Test
    fun findFieldNew() = benchmarkRule.measureRepeated {
        requireNotNull(NewXposedHelpers.findField(ContentResolver::class.java, "mRandom"))
    }

    @Test
    fun findConstructorExactOld() = benchmarkRule.measureRepeated {
        requireNotNull(
            OldXposedHelpers.findConstructorExact(
                ContentResolver::class.java,
                Context::class.java
            )
        )
    }

    @Test
    fun findConstructorExactStringBuilder() = benchmarkRule.measureRepeated {
        requireNotNull(
            StringBuilderXposedHelpers.findConstructorExact(
                ContentResolver::class.java,
                Context::class.java
            )
        )
    }

    @Test
    fun findConstructorExactNew() = benchmarkRule.measureRepeated {
        requireNotNull(
            NewXposedHelpers.findConstructorExact(
                ContentResolver::class.java,
                Context::class.java
            )
        )
    }

    @Test
    fun findConstructorBestMatchOld() = benchmarkRule.measureRepeated {
        requireNotNull(
            OldXposedHelpers.findConstructorBestMatch(
                ContentResolver::class.java,
                null
            )
        )
    }

    @Test
    fun findConstructorBestMatchStringBuilder() = benchmarkRule.measureRepeated {
        requireNotNull(
            StringBuilderXposedHelpers.findConstructorBestMatch(
                ContentResolver::class.java,
                null
            )
        )
    }

    @Test
    fun findConstructorBestMatchNew() = benchmarkRule.measureRepeated {
        requireNotNull(
            NewXposedHelpers.findConstructorBestMatch(
                ContentResolver::class.java,
                null
            )
        )
    }

    @Test
    fun findMethodExactOld() = benchmarkRule.measureRepeated {
        requireNotNull(
            OldXposedHelpers.findMethodExact(
                ContentResolver::class.java,
                "acquireExistingProvider",
                Context::class.java,
                String::class.java
            )
        )
    }

    @Test
    fun findMethodExactStringBuilder() = benchmarkRule.measureRepeated {
        requireNotNull(
            StringBuilderXposedHelpers.findMethodExact(
                ContentResolver::class.java,
                "acquireExistingProvider",
                Context::class.java,
                String::class.java
            )
        )
    }

    @Test
    fun findMethodExactNew() = benchmarkRule.measureRepeated {
        requireNotNull(
            NewXposedHelpers.findMethodExact(
                ContentResolver::class.java,
                "acquireExistingProvider",
                Context::class.java,
                String::class.java
            )
        )
    }

    @Test
    fun findMethodBestMatchOld() = benchmarkRule.measureRepeated {
        requireNotNull(
            OldXposedHelpers.findMethodBestMatch(
                ContentResolver::class.java,
                "canonicalize",
                null,
            )
        )
    }

    @Test
    fun findMethodBestMatchStringBuilder() = benchmarkRule.measureRepeated {
        requireNotNull(
            StringBuilderXposedHelpers.findMethodBestMatch(
                ContentResolver::class.java,
                "canonicalize",
                null,
            )
        )
    }

    @Test
    fun findMethodBestMatchNew() = benchmarkRule.measureRepeated {
        requireNotNull(
            NewXposedHelpers.findMethodBestMatch(
                ContentResolver::class.java,
                "canonicalize",
                null,
            )
        )
    }
}