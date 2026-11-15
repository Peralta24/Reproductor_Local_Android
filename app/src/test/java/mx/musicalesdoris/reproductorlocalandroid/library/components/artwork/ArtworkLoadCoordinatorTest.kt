package mx.musicalesdoris.reproductorlocalandroid.library.components.artwork

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import mx.musicalesdoris.reproductorlocalandroid.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArtworkLoadCoordinatorTest {
    @Test
    fun concurrentRequestsForSameKeyShareOneLoad() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val gate = CompletableDeferred<String?>()
            var loadCount = 0
            val cache = mutableMapOf<String, String>()
            val coordinator = ArtworkLoadCoordinator(
                scope, cache::get, { key, value -> cache[key] = value },
                { loadCount++; gate.await() }
            )

            val first = async(start = CoroutineStart.UNDISPATCHED) { coordinator.load("cover") }
            val second = async(start = CoroutineStart.UNDISPATCHED) { coordinator.load("cover") }
            assertEquals(1, loadCount)

            gate.complete("decoded")
            assertEquals("decoded", first.await())
            assertEquals("decoded", second.await())
            assertEquals(1, loadCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun successfulLoadIsReusedFromTheExistingCache() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var loadCount = 0
            val cache = mutableMapOf<String, String>()
            val coordinator = ArtworkLoadCoordinator(
                scope, cache::get, { key, value -> cache[key] = value },
                { loadCount++; "decoded" }
            )

            assertEquals("decoded", coordinator.load("cover"))
            assertEquals("decoded", coordinator.load("cover"))
            assertEquals(1, loadCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedLoadIsNotCachedAndCanBeRetried() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var loadCount = 0
            val cache = mutableMapOf<String, String>()
            val coordinator = ArtworkLoadCoordinator(
                scope, cache::get, { key, value -> cache[key] = value },
                { loadCount++; if (loadCount == 1) null else "decoded" }
            )

            assertEquals(null, coordinator.load("cover"))
            assertEquals("decoded", coordinator.load("cover"))
            assertEquals(2, loadCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun cancelingOneConsumerDoesNotCancelAnotherConsumersSharedLoad() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val gate = CompletableDeferred<String?>()
            var loadCount = 0
            val coordinator = ArtworkLoadCoordinator<String, String>(
                scope, { null }, { _, _ -> }, { loadCount++; gate.await() }
            )

            val canceledConsumer = async(start = CoroutineStart.UNDISPATCHED) { coordinator.load("cover") }
            val remainingConsumer = async(start = CoroutineStart.UNDISPATCHED) { coordinator.load("cover") }
            canceledConsumer.cancelAndJoin()
            gate.complete("decoded")

            assertEquals("decoded", remainingConsumer.await())
            assertEquals(1, loadCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun changedFileMetadataCreatesANewCacheKey() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var loadCount = 0
            val cache = mutableMapOf<ArtworkCacheKey, String>()
            val coordinator = ArtworkLoadCoordinator(
                scope, cache::get, { key, value -> cache[key] = value },
                { loadCount++; "decoded-$loadCount" }
            )
            val original = ArtworkCacheKey("cover.jpg", 100L, 200L, 126)
            val changed = original.copy(lastModifiedMs = 101L)

            assertEquals("decoded-1", coordinator.load(original))
            assertEquals("decoded-2", coordinator.load(changed))
            assertEquals(2, loadCount)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun catalogRevisionUsesIdentityAndChangesWhenCatalogIsReplaced() {
        val firstCatalog = listOf(Song(id = "1", fileName = "1.mp3", title = "Track"))
        val sameReference = ArtworkRevisionKey(firstCatalog)
        val anotherSameContentList = firstCatalog + emptyList()
        val updatedCatalog = listOf(firstCatalog.single().copy(title = "Updated"))

        assertEquals(sameReference, ArtworkRevisionKey(firstCatalog))
        assertNotEquals(sameReference, ArtworkRevisionKey(anotherSameContentList))
        assertNotEquals(sameReference, ArtworkRevisionKey(updatedCatalog))
    }
}
