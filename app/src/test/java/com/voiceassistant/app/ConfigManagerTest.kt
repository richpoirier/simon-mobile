package com.voiceassistant.app

import android.content.Context
import android.content.res.AssetManager
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.util.Properties

class ConfigManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockAssetManager: AssetManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAssetManager = mockk(relaxed = true)
        
        every { mockContext.assets } returns mockAssetManager
        
        // Clear any existing ConfigManager state
        mockkObject(ConfigManager)
        clearStaticMockk(ConfigManager::class)
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    @Test
    fun `test successful API key loading from config`() {
        // Simulate config.properties content
        val configContent = """
            # OpenAI API Configuration
            openai.api.key=sk-test-key-12345
            # Other settings
            debug.mode=true
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        every { mockAssetManager.open("config.properties") } returns inputStream
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertEquals("sk-test-key-12345", apiKey)
    }
    
    @Test
    fun `test missing config file returns null`() {
        every { mockAssetManager.open("config.properties") } throws FileNotFoundException("config.properties not found")
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertNull("Should return null when config file is missing", apiKey)
    }
    
    @Test
    fun `test empty API key in config`() {
        val configContent = """
            openai.api.key=
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        every { mockAssetManager.open("config.properties") } returns inputStream
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertNull("Should return null for empty API key", apiKey)
    }
    
    @Test
    fun `test missing API key property in config`() {
        val configContent = """
            # Config without API key
            some.other.property=value
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        every { mockAssetManager.open("config.properties") } returns inputStream
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertNull("Should return null when API key property is missing", apiKey)
    }
    
    @Test
    fun `test config loading with special characters`() {
        val configContent = """
            openai.api.key=sk-test@#$%^&*()_+{}|:"<>?
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        every { mockAssetManager.open("config.properties") } returns inputStream
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertEquals("sk-test@#\$%^&*()_+{}|:\"<>?", apiKey)
    }
    
    @Test
    fun `test config caching prevents multiple reads`() {
        val configContent = "openai.api.key=cached-key"
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        every { mockAssetManager.open("config.properties") } returns inputStream
        
        // Initialize and get key multiple times
        ConfigManager.init(mockContext)
        val key1 = ConfigManager.getApiKey()
        val key2 = ConfigManager.getApiKey()
        val key3 = ConfigManager.getApiKey()
        
        // Verify asset was only opened once
        verify(exactly = 1) { mockAssetManager.open("config.properties") }
        
        // All keys should be the same
        assertEquals("cached-key", key1)
        assertEquals(key1, key2)
        assertEquals(key2, key3)
    }
    
    @Test
    fun `test error handling during config read`() {
        every { mockAssetManager.open("config.properties") } throws RuntimeException("Asset read error")
        
        ConfigManager.init(mockContext)
        val apiKey = ConfigManager.getApiKey()
        
        assertNull("Should handle errors gracefully", apiKey)
    }
}