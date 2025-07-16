package com.simon.app.config

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream
import java.io.IOException

class ConfigManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockAssetManager: AssetManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
    }
    
    @Test
    fun `test getOpenAIApiKey returns key when present`() {
        val configContent = "openai.api.key=sk-test-key-12345"
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        
        `when`(mockAssetManager.open("config.properties")).thenReturn(inputStream)
        
        val configManager = ConfigManager(mockContext)
        val apiKey = configManager.getOpenAIApiKey()
        
        assertEquals("sk-test-key-12345", apiKey)
    }
    
    @Test(expected = IllegalStateException::class)
    fun `test getOpenAIApiKey throws when key not present`() {
        val configContent = "some.other.key=value"
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        
        `when`(mockAssetManager.open("config.properties")).thenReturn(inputStream)
        
        val configManager = ConfigManager(mockContext)
        configManager.getOpenAIApiKey()
    }
    
    @Test(expected = IllegalStateException::class)
    fun `test constructor throws when config file not found`() {
        `when`(mockAssetManager.open("config.properties")).thenThrow(IOException::class.java)
        
        ConfigManager(mockContext)
    }
    
    @Test
    fun `test getProperty returns value when present`() {
        val configContent = """
            openai.api.key=sk-test-key
            app.theme=dark
            app.volume=0.8
        """.trimIndent()
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        
        `when`(mockAssetManager.open("config.properties")).thenReturn(inputStream)
        
        val configManager = ConfigManager(mockContext)
        
        assertEquals("dark", configManager.getProperty("app.theme"))
        assertEquals("0.8", configManager.getProperty("app.volume"))
    }
    
    @Test
    fun `test getProperty returns default value when not present`() {
        val configContent = "openai.api.key=sk-test-key"
        val inputStream = ByteArrayInputStream(configContent.toByteArray())
        
        `when`(mockAssetManager.open("config.properties")).thenReturn(inputStream)
        
        val configManager = ConfigManager(mockContext)
        
        assertEquals("light", configManager.getProperty("app.theme", "light"))
        assertNull(configManager.getProperty("app.theme"))
    }
}