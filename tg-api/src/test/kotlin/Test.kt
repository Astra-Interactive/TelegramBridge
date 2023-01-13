import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Assertions.assertTrue;
import ru.astrainteractive.telegramapi.BotApi
import ru.astrainteractive.telegramapi.HttpClient
import ru.astrainteractive.telegramapi.TelegramBot
import kotlin.test.assertNotNull

class Test {

    @Test
    fun testGetRequest(){
        val client = HttpClient()
        val response = client.get("").getOrNull()
        assertNotNull(response)
    }
    @Test
    fun testPostRequest(){
        val client = HttpClient()
        val response = client.post("").onFailure {
            it.printStackTrace()
        }.onSuccess {
            println(it)
        }.getOrNull()
        assertNotNull(response)
    }

    @Test
    fun testBot(){
        val botApi = BotApi("",HttpClient())
        runBlocking {
            botApi.getMe()
            botApi.getUpdate().onSuccess {
                println(it)
            }.onFailure {
                it.printStackTrace()
            }
        }

    }
}