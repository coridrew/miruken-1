package com.miruken.context

import com.miruken.assertAsync
import com.miruken.callback.Handler
import com.miruken.callback.Key
import com.miruken.callback.Provides
import com.miruken.callback.Proxy
import com.miruken.container.Managed
import com.miruken.container.TestContainer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.util.*
import kotlin.test.*

class PropertiesTest {
    private lateinit var _context: Context

    @Rule
    @JvmField val testName = TestName()

    class Foo
    interface Auction {
        fun buy(itemId: Long): UUID
    }

    @Before
    fun setup() {
        _context = Context()
    }

    @After
    fun cleanup() {
        _context.end()
    }

    @Test fun `Delegates property to context`() {
        val foo = Foo()
        _context.store(foo)
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context }
        assertSame(foo, instance.foo)
    }

    @Test fun `Delegates property to child context`() {
        val foo = Foo()
        _context.store(foo)
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context.createChild() }
        assertSame(foo, instance.foo)
    }

    @Test fun `Delegates optional property to context`() {
        val foo = Foo()
        _context.store(foo)
        val instance = object : ContextualHandler() {
            val foo by get<Foo?>()
        }.apply { context = _context }
        assertSame(foo, instance.foo)
    }

    @Test fun `Ignores missing optional property`() {
        val instance = object : ContextualHandler() {
            val foo by get<Foo?>()
        }.apply { context = _context }
        assertNull(instance.foo)
    }
    
    @Test fun `Delegates list property to context`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        })
        val instance = object : ContextualHandler() {
            val foos by getAll<Foo>()
        }.apply { context = _context }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Delegates array property to context`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo(), Foo())
        })
        val instance = object : ContextualHandler() {
            val foos by getArray<Foo>()
        }.apply { context = _context }
        assertEquals(3, instance.foos.size)
    }

    @Test fun `Delegates primitive property to context`() {
        _context.addHandlers(object : Handler() {
            @Provides
            val primes = listOf(2,3,5,7,11)

            @Provides
            @Key("help")
            val primaryHelp = "www.help.com"

            @Provides
            @Key("help")
            val secondaryHelp = "www.help2.com"

            @Provides
            @Key("help")
            val criticalHelp = "www.help3.com"
        })
        val instance = object : ContextualHandler() {
            val primes by get<IntArray>()
            val help by getArray<String>()
        }.apply { context = _context }
        assertTrue(instance.primes.contentEquals(arrayOf(2,3,5,7,11).toIntArray()))
        assertEquals(3, instance.help.size)
        assertTrue(instance.help.contains("www.help.com"))
        assertTrue(instance.help.contains("www.help2.com"))
        assertTrue(instance.help.contains("www.help3.com"))
    }

    @Test fun `Uses empty list property if missing`() {
        val instance = object : ContextualHandler() {
            val foos by getAll<Foo>()
        }.apply { context = _context }
        assertEquals(0, instance.foos.size)
    }

    @Test fun `Delegates promise property to context`() {
        val foo = Foo()
        _context.store(foo)
        val instance = object : ContextualHandler() {
            val foo by getAsync<Foo>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo then {
                assertSame(foo, it)
                done()
            }
        }
    }

    @Test fun `Delegates optional promise property to context`() {
        val instance = object : ContextualHandler() {
            val foo by getAsync<Foo?>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo then {
                assertNull(it)
                done()
            }
        }
    }

    @Test fun `Delegates promise list property to context`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo())
        })
        val instance = object : ContextualHandler() {
            val foo by getAllAsync<Foo>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo then {
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Delegates promise array property to context`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoos() = listOf(Foo(), Foo())
        })
        val instance = object : ContextualHandler() {
            val foo by getArrayAsync<Foo>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo then {
                assertEquals(2, it.size)
                done()
            }
        }
    }

    @Test fun `Delegates proxy property to context`() {
        _context.addHandlers(object : Handler(), Auction {
            override fun buy(itemId: Long): UUID = UUID.randomUUID()
        })
        val instance = object : ContextualHandler() {
            @Proxy val auction by get<Auction>()
        }.apply { context = _context }
        assertNotNull(instance.auction.buy(2))
    }

    @Test fun `Delegates container property to context`() {
        _context.addHandlers(TestContainer())
        val instance = object : ContextualHandler() {
            @Managed val foo by get<Foo>()
        }.apply { context = _context }
        assertNotNull(instance.foo)
    }

    @Test fun `Delegates promise container property to context`() {
        _context.addHandlers(TestContainer())
        val instance = object : ContextualHandler() {
            @Managed val foo by getAsync<Foo>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo then {
                assertNotNull(it)
                done()
            }
        }
    }

    @Test fun `Delegates property to context once`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoo() = Foo()
        })
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context }
        assertSame(instance.foo, instance.foo)
    }

    @Test fun `Delegates property to context always`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoo() = Foo()
        })
        val instance = object : ContextualHandler() {
            val foo by link<Foo>()
        }.apply { context = _context }
        assertNotSame(instance.foo, instance.foo)
    }

    @Test fun `Delegates property to context if changes`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoo() = Foo()
        })
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context }
        val foo = instance.foo
        assertSame(foo, instance.foo)

        instance.context = Context().apply {
            addHandlers(object : Handler() {
                @Provides
                fun provideFoo() = Foo()
            })
        }
        assertNotSame(foo, instance.foo)
    }

    @Test fun `Delegates property to context after ending`() {
        _context.addHandlers(object : Handler() {
            @Provides
            fun provideFoo() = Foo()
        })
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context }
        _context.end()
        assertNotNull(instance.foo)
    }

    @Test fun `Rejects property delegation if context unavailable`() {
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }
        assertFailsWith(IllegalStateException::class) {
            instance.foo
        }
    }

    @Test fun `Ignores missing optional property if context unavailable`() {
        val instance = object : ContextualHandler() {
            val foo by get<Foo?>()
        }
        assertNull(instance.foo)
    }

    @Test fun `Rejects property delegation if missing`() {
        val instance = object : ContextualHandler() {
            val foo by get<Foo>()
        }.apply { context = _context }
        assertFailsWith(IllegalStateException::class) {
            instance.foo
        }
    }

    @Test fun `Rejects promise property delegation if missing`() {
        val instance = object : ContextualHandler() {
            val foo by getAsync<Foo>()
        }.apply { context = _context }
        assertAsync(testName) { done ->
            instance.foo catch {
                assertTrue(it is IllegalStateException)
                done()
            }
        }
    }
}