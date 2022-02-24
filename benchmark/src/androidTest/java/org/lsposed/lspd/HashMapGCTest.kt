package org.lsposed.lspd

import android.content.ContentResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@RunWith(AndroidJUnit4::class)
@SmallTest
class HashMapGCTest {
    private val normMap = ConcurrentHashMap<KeyCase, String>()
    private val weakMap = ConcurrentHashMap<WeakKeyCase, String>()

    private data class KeyCase(
        val clazz: Class<*>?,
        val str: String?
    )

    private class WeakKeyCase(
        val clazz: WeakReference<Class<*>>,
        val str: WeakReference<String>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WeakKeyCase

            if (clazz.get() != other.clazz.get()) return false
            if (str.get() != other.str.get()) return false

            return true
        }

        override fun hashCode(): Int {
            var result = clazz.get()?.hashCode() ?: 0
            result = 31 * result + (str.get()?.hashCode() ?: 0)
            return result
        }
    }


    @Test fun hashcodeForWeakReferences() {
        WeakReference(0).hashCode() shouldNotBe WeakReference(0).hashCode()
    }

    @Before fun fillingMembers() {
        ContentResolver::class.java.declaredFields.forEach {
            normMap[KeyCase(it.type, it.name)] = it.name
            weakMap[WeakKeyCase(WeakReference(it.type), WeakReference(it.name))] = it.name
        }
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    @Test fun gc() {
        normMap.shouldNotBeEmpty()
        weakMap.shouldNotBeEmpty()

        val firstNormalKey = normMap.keys.firstOrNull()
        val firstWeakKey = weakMap.keys.firstOrNull()

        firstNormalKey.shouldNotBeNull()
        firstWeakKey.shouldNotBeNull()

        normMap.clear()
        weakMap.clear()

        System.gc()
        Thread.sleep(5000)

        firstNormalKey?.str.shouldNotBeNull()
        firstNormalKey?.clazz.shouldNotBeNull()

        firstWeakKey?.str.shouldNotBeNull()
        firstWeakKey?.str.get().shouldBeNull()
        firstWeakKey?.clazz.shouldNotBeNull()
        firstWeakKey?.clazz.get().shouldBeNull()
    }
}